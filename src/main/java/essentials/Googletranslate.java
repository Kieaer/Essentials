package essentials;

import com.alibaba.fastjson.JSONArray;

import javax.script.Invocable;
import javax.script.ScriptEngine;
import javax.script.ScriptEngineManager;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.net.URLEncoder;
import java.net.Proxy;
import java.net.URI;
import java.util.Map;
import java.net.InetSocketAddress;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.http.HttpHost;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.utils.URIBuilder;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.util.EntityUtils;


public class Googletranslate {

    private static final String PATH = "/GA-resources/gettk.js";
    public String url;
    public void setUrl(String url) {
        this.url = url;
    }
    public static boolean isBlank(String string) {
        if (string == null || "".equals(string.trim())) {
            return true;
        }
        return false;
    }

    public static boolean isNotBlank(String string) {
        return !isBlank(string);
    }

    public static String extractByStartAndEnd(String str, String startStr, String endStr) {
        String regEx = startStr + ".*?"+endStr;
        String group = findMatchString(str, regEx);
        String trim = group.replace(startStr, "").replace(endStr, "").trim();
        return trim(trim);
    }

    public static String findMatchString(String str, String regEx) {
        try {
            Pattern pattern = Pattern.compile(regEx);
            Matcher matcher = pattern.matcher(str);
            return findFristGroup(matcher);
        } catch (Exception e) {
            e.printStackTrace();
        return null;
        }
    }

    private static String findFristGroup(Matcher matcher) {
        matcher.find();
        return matcher.group(0);
    }

    public static String removeAllBlank(String s){
        String result = "";
        if(null!=s && !"".equals(s)){
            result = s.replaceAll("[　*| *| *|//s*]*", "");
        }
        return result;
    }

    public static String trim(String s){
        String result = "";
        if(null!=s && !"".equals(s)){
            result = s.replaceAll("^[　*| *| *|//s*]*", "").replaceAll("[　*| *| *|//s*]*$", "");
        }
        return result;
    }

    public static final int SO_TIMEOUT_MS = 8000;
    public static final int CONNECTION_TIMEOUT_MS = 1000;

    public static String doGet(String url, Map<String, String> param) {
        CloseableHttpClient httpclient = HttpClients.createDefault();
        String resultString = "";
        CloseableHttpResponse response = null;
        try {
            URIBuilder builder = new URIBuilder(url);
            if (param != null) {
                for (String key : param.keySet()) {
                    builder.addParameter(key, (String) param.get(key));
                }
            }
            URI uri = builder.build();

            HttpGet httpGet = new HttpGet(uri);

            response = httpclient.execute(httpGet);

            if (response.getStatusLine().getStatusCode() == 200)
                resultString = EntityUtils.toString(response.getEntity(), "UTF-8");
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            try {
                if (response != null) {
                    response.close();
                }
                httpclient.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        return resultString;
    }

    public static String doGet(String url, Map<String, String> param, Proxy proxy) throws Exception {
        CloseableHttpClient httpclient = HttpClients.createDefault();
        String resultString = "";
        CloseableHttpResponse response = null;
        try {
            URIBuilder builder = new URIBuilder(url);
            if (param != null) {
                for (String key : param.keySet()) {
                    builder.addParameter(key, (String) param.get(key));
                }
            }
            URI uri = builder.build();

            HttpGet httpGet = new HttpGet(uri);
            httpGet.setConfig(buildRequestConfig(proxy));

            response = httpclient.execute(httpGet);

            if (response.getStatusLine().getStatusCode() == 200)
                resultString = EntityUtils.toString(response.getEntity(), "UTF-8");
        } catch (Exception e) {
            throw e;
        } finally {
            try {
                if (response != null) {
                    response.close();
                }
                httpclient.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        return resultString;
    }

    public static String doGetWithProxy(String url, Proxy proxy) throws Exception {
        return doGet(url, null, proxy);
    }

    public static String doGet(String url) {
        return doGet(url, null);
    }

    public static RequestConfig buildRequestConfig(Proxy proxy) {
        String address = proxy.address().toString();
        String[] addressArr = address.replace("/", "").split(":");
        String ip = addressArr[0].trim();
        String host = addressArr[1].trim();

        if ((proxy != null) && (!(isBlank(ip))) && (!(isBlank(host)))) {
            HttpHost httpHost = new HttpHost(ip, Integer.parseInt(host));
            RequestConfig requestConfig = RequestConfig.custom().setProxy(httpHost)
                    .setSocketTimeout(SO_TIMEOUT_MS)
                    .setConnectTimeout(CONNECTION_TIMEOUT_MS).build();
            return requestConfig;
        }

        RequestConfig requestConfig = RequestConfig.custom()
                .setSocketTimeout(SO_TIMEOUT_MS)
                .setConnectTimeout(CONNECTION_TIMEOUT_MS).build();
        return requestConfig;
    }

    static ScriptEngine engine = null;

    static {
        ScriptEngineManager maneger = new ScriptEngineManager();
        engine = maneger.getEngineByName("javascript");
        FileInputStream fileInputStream = null;
        Reader scriptReader = null;

        try {
            scriptReader = new InputStreamReader(Googletranslate.class.getResourceAsStream(PATH), "utf-8");
            engine.eval(scriptReader);
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            if (fileInputStream != null) {
                try {
                    fileInputStream.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
            if (scriptReader != null) {
                try {
                    scriptReader.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
    }


    public String getTKK() throws Exception {
        setUrl("https://translate.google.cn/");

        try {
            String result = doGet(this.url);
            if (isNotBlank(result)) {
                if (result.indexOf("tkk") > -1) {
                    String matchString = findMatchString(result, "tkk:.*?',");
                    String tkk = matchString.substring(5, matchString.length() - 2);
                    return tkk;
                }
            }
        } catch (Exception e) {
        }
        return null;
    }

    public static String getTK(String word, String tkk) {
        String result = null;

        try {
            if (engine instanceof Invocable) {
                Invocable invocable = (Invocable) engine;
                result = (String) invocable.invokeFunction("tk", new Object[]{word, tkk});
            }
        } catch (Exception e) {
        }

        return result;
    }

    public String translate(String word, String from, String to) throws Exception {
        if (isBlank(word)) {
            return null;
        }

        String tkk = getTKK();

        if (isBlank(tkk)) {
        }

        String tk = getTK(word, tkk);

        try {
            word = URLEncoder.encode(word, "UTF-8");
        } catch (Exception e) {
            e.printStackTrace();
        }

        StringBuffer buffer = new StringBuffer("https://translate.google.cn/translate_a/single?client=t");

        if (isBlank(from)) {
            from = "auto";
        }

        buffer.append("&sl=" + from);
        buffer.append("&tl=" + to);
        buffer.append("&hl=zh-CN&dt=at&dt=bd&dt=ex&dt=ld&dt=md&dt=qca&dt=rw&dt=rm&dt=ss&dt=t&ie=UTF-8&oe=UTF-8&source=btn&kc=0");
        buffer.append("&tk=" + tk);
        buffer.append("&q=" + word);
        setUrl(buffer.toString());

        try {
            String result = doGet(this.url);
            JSONArray array = (JSONArray) JSONArray.parse(result);
            JSONArray rArray = array.getJSONArray(0);
            StringBuffer rBuffer = new StringBuffer();
            for (int i = 0; i < rArray.size(); i++) {
                String r = rArray.getJSONArray(i).getString(0);
                if (isNotBlank(r)) {
                    rBuffer.append(r);
                }
            }
            return rBuffer.toString();
        } catch (Exception e) {
            return null;
        }
    }

    public String translate(String word, String to) throws Exception {
        return translate(word, null, to);
    }
}
