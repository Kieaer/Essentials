package essentials.thread;

import essentials.Global;
import org.apache.maven.artifact.versioning.DefaultArtifactVersion;
import org.json.JSONObject;
import org.json.JSONTokener;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;

public class Update {
    public static void main(){
        HttpURLConnection con = null;
        try {
            String apiURL = "https://api.github.com/repos/kieaer/Essentials/releases/latest";
            URL url = new URL(apiURL);
            con = (HttpURLConnection) url.openConnection();
            con.setRequestMethod("GET");
            con.setRequestProperty("Content-length", "0");
            con.setUseCaches(false);
            con.setAllowUserInteraction(false);
            con.setConnectTimeout(3000);
            con.setReadTimeout(3000);
            con.connect();
            int status = con.getResponseCode();
            StringBuilder response = new StringBuilder();

            switch (status) {
                case 200:
                case 201:
                    BufferedReader br = new BufferedReader(new InputStreamReader(con.getInputStream()));
                    String line;
                    while ((line = br.readLine()) != null) {
                        response.append(line).append("\n");
                    }
                    br.close();
                    con.disconnect();
            }

            JSONTokener parser = new JSONTokener(response.toString());
            JSONObject object = new JSONObject(parser);

            DefaultArtifactVersion latest = new DefaultArtifactVersion((String) object.get("tag_name"));
            DefaultArtifactVersion current = new DefaultArtifactVersion("4.0");

            if(latest.compareTo(current) > 0){
                Global.log("New version found!");
            } else if(latest.compareTo(current) == 0){
                Global.log("Current version is up to date.");
            } else if(latest.compareTo(current) < 0){
                Global.log("You're using development version!");
            }

        } catch (Exception e){
            e.printStackTrace();
        }
    }
}
