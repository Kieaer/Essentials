import essentials.core.PlayerDB;
import org.junit.Test;
import org.mindrot.jbcrypt.BCrypt;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Random;
import java.util.concurrent.ThreadLocalRandom;

public class testPlayerDB {
    ArrayList<PlayerDB.PlayerData> data = new ArrayList<>();
    Random rd = new Random();

    void createData(int a){
        String date = DateTimeFormatter.ofPattern("yy-MM-dd HH:mm.ss").format(LocalDateTime.now());
        StringBuilder name = new StringBuilder();
        StringBuilder accountid = new StringBuilder();
        StringBuilder accountpw = new StringBuilder();
        for(int b=0;b<20;b++) name.append((char) (rd.nextInt(26) + 97));
        for(int b=0;b<20;b++) accountid.append((char) (rd.nextInt(26) + 97));
        for(int b=0;b<20;b++) accountpw.append((char) (rd.nextInt(26) + 97));

        data.add(new PlayerDB.PlayerData(
                a,
                name.toString(), // name
                "testuuid",
                "testcountry",
                "testcountry_code",
                "language",
                rd.nextBoolean(), // isAdmin
                0,
                0,
                0,
                0,
                0,
                0,
                0,
                0,
                500,
                "0/500",
                date, // firstdate
                date, // lastdate
                "none",
                "none",
                "none",
                "00:00.00",
                0,
                0,
                0,
                0,
                0,
                0,
                "none",
                rd.nextBoolean(), // banned
                rd.nextBoolean(), // translate
                rd.nextBoolean(), // crosschat
                rd.nextBoolean(), // colornick
                false, // connected
                "none",
                "default",
                false, // mute
                0L,
                accountid.toString(), // accountid
                BCrypt.hashpw(accountpw.toString(), BCrypt.gensalt(11)) // accountpw
        ));
    }

    @Test
    public void testPlayerDBWrite() throws InterruptedException {
        Thread first = new Thread(() -> {
            for (int a = 0; a < 100; a++) {
                System.out.print("\r" + a + "...");
                createData(a);
                System.out.flush();
            }
        });
        first.start();
        first.join();
    }

    @Test
    public void testPlayerDBRead(){
        for (int a=0;a<20;a++){
            PlayerDB.PlayerData result = data.get(ThreadLocalRandom.current().nextInt(0, 100));
            assert result.udid == 0L;
        }
    }

    @Test
    public void testPlayerDBWriteRemove() throws InterruptedException {
        data = new ArrayList<>();
        Thread write = new Thread(() -> {
            for (int a = 0; a < 100; a++) {
                System.out.print("\r" + a + "...");
                createData(a);
                System.out.flush();
            }
        });
        write.start();
        write.join();
        Thread remove = new Thread(() -> {
            data.remove(1);
            data.remove(5);
            data.remove(8);
            data.remove(19);
            data.remove(36);
        });
        remove.start();
        remove.join();
    }
}
