import essentials.core.PlayerDB;
import org.junit.Test;
import org.mindrot.jbcrypt.BCrypt;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Random;

public class testPlayerDB {
    public static ArrayList<PlayerDB.PlayerData> data = new ArrayList<>();
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
    public void testPlayerDBWrite() {
        createData(1);
        assert data.size() != 0;

        PlayerDB.PlayerData d = data.get(0);
        d.name = "Random name";
    }

    @Test
    public void testPlayerDBRead(){
        assert data.size() != 0;
        PlayerDB.PlayerData d = data.get(0);
        System.out.println(d.name);
    }
}
