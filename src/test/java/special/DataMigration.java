package special;

public class DataMigration {
    /*File root = Paths.get("datatest/").toFile();

    public DataMigration() throws Exception {
        System.out.print("\r" + dbundle("data-migration"));
        String condata = readString(new FileInputStream(new File("test/config.yml")), "UTF-8");
        condata = condata.replace("colornick update interval", "cupdatei").replace("null", "none");
        root.child("config.hjson").writeString("{\n" + condata + "\n}");
        root.child("config.yml").delete();

        if (root.child("BlockReqExp.yml").exists()) move("BlockReqExp");
        if (root.child("Exp.yml").exists()) move("Exp");
        if (root.child("permission.yml").exists()) {
            root.child("permission.yml").delete();
            config.validfile();
        }
        if (root.child("data/data.json").exists()) {
            String data = root.child("data/data.json").readString();
            JsonObject value = JsonValue.readJSON(data).asObject();
            if (value.get("banned") != null) {
                JsonObject arrays = value.get("banned").asObject();
                saveall();
                for (int a = 0; a < arrays.size(); a++) {
                    LocalDateTime date = LocalDateTime.parse(arrays.get("date").asString());
                    String name = arrays.get("name").asString();
                    String uuid = arrays.get("uuid").asString();
                    PluginData.banned.add(new PluginData.banned(date, name, uuid));
                }
                // 서버간 이동 데이터들은 변환하지 않음
                root.child("data/data.json").delete();
            }
        }
        System.out.print("\r" + dbundle("data-migration") + " " + dbundle("success") + "\n");
    }

    public void move(String path){
        String data = root.child(path+".yml").readString();
        root.child(path+".hjson").writeString("{\n"+data+"\n}");
        root.child(path+".yml").delete();
    }

    public String readString(InputStream input, String encoding){
        StringBuilder output = new StringBuilder();
        try {
            InputStreamReader reader = new InputStreamReader(input, encoding);
            char[] buffer = new char[256];

            while (true) {
                int length = reader.read(buffer);
                if (length == -1) {
                    return output.toString();
                }

                output.append(buffer, 0, length);
            }
        }catch(Exception e){
            e.printStackTrace();
        }
        return output.toString();
    }*/
}
