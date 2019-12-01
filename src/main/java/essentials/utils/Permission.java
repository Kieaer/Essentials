package essentials.utils;

import io.anuke.arc.Core;
import io.anuke.arc.collection.Array;
import org.yaml.snakeyaml.Yaml;

import java.util.Map;

public class Permission {
    private Map<String, Array<String>> obj;
    Yaml yaml = new Yaml();

    Permission(){
        if(Core.settings.getDataDirectory().child("mods/Essentials/permission.yml").exists()) {
            obj = yaml.load(String.valueOf(Core.settings.getDataDirectory().child("mods/Essentials/permission.yml").readString()));
        }
    }

    public void main(){
        yaml.
    }
}