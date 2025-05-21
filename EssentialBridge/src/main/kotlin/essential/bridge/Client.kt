package essential.bridge

import arc.util.Log
import mindustry.gen.Call

class Client : Runnable {
    // todo ban 공유 서버 ip
    var socket: java.net.Socket? = java.net.Socket()
    var lastReceivedMessage: String? = ""
    var writer: java.io.BufferedWriter? = null
    var reader: java.io.BufferedReader? = null

    override fun run() {
        try {
            Main.Companion.conf.port?.let {
                socket!!.connect(
                    java.net.InetSocketAddress(
                        Main.Companion.conf.address,
                        it
                    ), 5000
                )
            }

            reader = java.io.BufferedReader(java.io.InputStreamReader(socket!!.getInputStream()))
            writer = java.io.BufferedWriter(java.io.OutputStreamWriter(socket!!.getOutputStream()))

            while (!Thread.currentThread().isInterrupted()) {
                try {
                    val d = reader!!.readLine()
                    if (d != null) {
                        when (d) {
                            "message" -> {
                                val msg = reader!!.readLine()
                                lastReceivedMessage = msg
                                Call.sendMessage(msg)
                            }

                            "exit" -> {
                                socket!!.close()
                                java.lang.Thread.currentThread().interrupt()
                            }

                            else -> {}
                        }
                    }
                } catch (e: java.lang.Exception) {
                    java.lang.Thread.currentThread().interrupt()
                }
            }
        } catch (e: java.net.SocketTimeoutException) {
            Log.info(essential.bridge.Main.Companion.bundle.get("network.client.timeout"))
        } catch (e: java.io.InterruptedIOException) {
            try {
                socket!!.close()
            } catch (ex: java.io.IOException) {
                throw java.lang.RuntimeException(ex)
            }
        } catch (e: java.lang.Exception) {
            e.printStackTrace()
        }
    }

    @kotlin.Throws(java.io.IOException::class)
    private fun write(msg: kotlin.String) {
        writer!!.write(msg)
        writer!!.newLine()
        writer!!.flush()
    }

    fun message(message: kotlin.String) {
        try {
            write("message")
            write(message)
        } catch (e: java.io.IOException) {
            e.printStackTrace()
        }
    }

    fun send(command: kotlin.String, vararg parameter: kotlin.String?) {
        when (command) {
            "crash" -> try {
                java.net.Socket("mindustry.kr", 6000).use { socket ->
                    socket.setSoTimeout(5000)
                    java.io.BufferedWriter(java.io.OutputStreamWriter(socket.getOutputStream())).use { out ->
                        out.write("crash\n")
                        java.util.Scanner(socket.getInputStream()).use { sc ->
                            sc.nextLine()
                            out.write(parameter[0] + "\n")
                            out.write("null")
                            sc.nextLine()
                            kotlin.io.println("Crash log reported!")
                        }
                    }
                }
            } catch (e: java.net.SocketTimeoutException) {
                kotlin.io.println("Connection timed out. crash report server may be closed.")
            } catch (e: java.io.IOException) {
                e.printStackTrace()
            }

            "exit" -> if (socket != null) {
                try {
                    write("exit")
                    socket!!.close()
                } catch (e: java.io.IOException) {
                    e.printStackTrace()
                }
            }

            else -> {}
        }
    }
}