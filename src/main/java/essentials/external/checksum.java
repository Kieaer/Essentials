package essentials.external;

import arc.Core;

import java.io.FileInputStream;
import java.io.IOException;
import java.security.DigestInputStream;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

import static essentials.Main.root;

public class checksum {
    private final String sum;

    public checksum() {
        StringBuilder result = new StringBuilder();

        try {
            MessageDigest md = MessageDigest.getInstance("SHA-512");
            try (DigestInputStream dis = new DigestInputStream(new FileInputStream(Core.settings.getDataDirectory().child("mods/Essentials.jar").absolutePath()), md)) {
                while (dis.read() != -1) ;
                md = dis.getMessageDigest();
            }
            for (byte b : md.digest()) result.append(String.format("%02x", b));
        } catch (NoSuchAlgorithmException | IOException e) {
            sum = "null";
            return;
        }

        sum = result.toString();
    }

    public boolean check(String hash) {
        return hash.equals(sum);
    }

    public void extract() {
        root.child("Essentials.sha512sum").writeString(sum);
    }
}
