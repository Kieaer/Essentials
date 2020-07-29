package essentials.external

import java.net.InetAddress
import java.net.UnknownHostException
import kotlin.experimental.and

/*
 * Copyright 2002-2019 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */ /**
 * Matches a request based on IP Address or subnet mask matching against the remote
 * address.
 *
 *
 * Both IPv6 and IPv4 addresses are supported, but a matcher which is configured with an
 * IPv4 address will never match a request which returns an IPv6 address, and vice-versa.
 *
 * @author Luke Taylor
 * @since 3.0.2
 *
 *
 * Slightly modified by omidzk to have zero dependency to any frameworks other than the JDK.
 */
class IpAddressMatcher(ipAddress: String) {
    private var nMaskBits = 0
    private val requiredAddress: InetAddress
    fun matches(address: String): Boolean {
        val remoteAddress = parseAddress(address)
        if (requiredAddress.javaClass != remoteAddress.javaClass) {
            return false
        }
        if (nMaskBits < 0) {
            return remoteAddress == requiredAddress
        }
        val remAddr = remoteAddress.address
        val reqAddr = requiredAddress.address
        val nMaskFullBytes = nMaskBits / 8
        val finalByte = (0xFF00 shr (nMaskBits and 0x07)).toByte()
        for (i in 0 until nMaskFullBytes) {
            if (remAddr[i] != reqAddr[i]) {
                return false
            }
        }
        return if (finalByte.toInt() != 0) {
            remAddr[nMaskFullBytes] and finalByte == reqAddr[nMaskFullBytes] and finalByte
        } else true
    }

    private fun parseAddress(address: String): InetAddress {
        return try {
            InetAddress.getByName(address)
        } catch (e: UnknownHostException) {
            throw IllegalArgumentException("Failed to parse address$address", e)
        }
    }

    /**
     * Takes a specific IP address or a range specified using the IP/Netmask (e.g.
     * 192.168.1.0/24 or 202.24.0.0/14).
     *
     * @param ipAddress the address or range of addresses from which the request must
     * come.
     */
    init {
        var ipAddress = ipAddress
        if (ipAddress.indexOf('/') > 0) {
            val addressAndMask = ipAddress.split("/").toTypedArray()
            ipAddress = addressAndMask[0]
            nMaskBits = addressAndMask[1].toInt()
        } else {
            nMaskBits = -1
        }
        requiredAddress = parseAddress(ipAddress)
        assert(requiredAddress.address.size * 8 >= nMaskBits) {
            String.format("IP address %s is too short for bitmask of length %d",
                    ipAddress, nMaskBits)
        }
    }
}