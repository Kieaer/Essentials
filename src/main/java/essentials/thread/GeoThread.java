package essentials.thread;

import org.json.JSONObject;
import org.json.JSONTokener;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class GeoThread implements Runnable{
    private String ip;
    private boolean isLocal;
    private static String geo;
    private static String geocode;
    private static String lang;

    public GeoThread(String ip, boolean isLocal) {
        this.ip = ip;
        this.isLocal = isLocal;
    }

    public static String getGeo() {
        return geo;
    }

    public static String getGeocode() {
        return geocode;
    }

    public static String getLang() {
        return lang;
    }

    @Override
    public void run() {
        Thread.currentThread().setName("Get geolocation thread");
        Pattern p = Pattern.compile(
                "(^127\\.)|\n" +
                "(^10\\.)|\n" +
                "(^172\\.1[6-9]\\.)|(^172\\.2[0-9]\\.)|(^172\\.3[0-1]\\.)|\n" +
                "(^192\\.168\\.)");
        Matcher m = p.matcher(ip);

        if(m.find()){
            isLocal = true;
        }
        if(isLocal) {
            geo = "Local IP";
            geocode = "LC";
            lang = "en";
        } else {
            try {
                String apiURL = "http://ipapi.co/" + ip + "/json";
                URL url = new URL(apiURL);
                HttpURLConnection con = (HttpURLConnection) url.openConnection();
                con.setReadTimeout(2000);
                con.setRequestMethod("POST");

                boolean redirect = false;

                int status = con.getResponseCode();
                if (status != HttpURLConnection.HTTP_OK) {
                    if (status == HttpURLConnection.HTTP_MOVED_TEMP || status == HttpURLConnection.HTTP_MOVED_PERM || status == HttpURLConnection.HTTP_SEE_OTHER) redirect = true;
                }

                if (redirect) {
                    String newUrl = con.getHeaderField("Location");
                    String cookies = con.getHeaderField("Set-Cookie");

                    con = (HttpURLConnection) new URL(newUrl).openConnection();
                    con.setRequestProperty("Cookie", cookies);
                }

                BufferedReader br = new BufferedReader(new InputStreamReader(con.getInputStream()));
                String inputLine;
                StringBuffer response = new StringBuffer();
                while ((inputLine = br.readLine()) != null) {
                    response.append(inputLine);
                }
                br.close();
                JSONTokener parser = new JSONTokener(response.toString());
                JSONObject result = new JSONObject(parser);

                if(result.has("reserved")){
                    geo = "Local IP";
                    geocode = "LC";
                    lang = "en";
                } else {
                    geo = result.getString("country_name");
                    geocode = result.getString("country");
                    lang = result.getString("languages").substring(0, 1);
                }
            } catch (IOException e) {
                geo = "invalid";
                geocode = "invalid";
                lang = "en";
            }
        }
    }
}