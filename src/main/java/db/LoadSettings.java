package db;

import utils.Utils;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class LoadSettings {

    public ArrayList<Method> methodList = new ArrayList<>(); // Все найденные процедури и функции
    public ArrayList<Called> counting = new ArrayList<>(); // Для подсчета вызова каждого метода
    public Map<String, List<Method>> moduleMethods = new HashMap<>(); // локальные методы каждого модуля
    public List<String> bslFiles;
    public String base;
    public String basePath;
    public String jsonPath;
    public String path;

    public LoadSettings(String base, Map<String, Object> settings) throws Exception {

        File root = new File(settings.get("path").toString().concat(File.separator).concat(base));

        this.bslFiles = Utils.getFilePathModules(root); // Полный список модулей
        this.base = root.toString().split(File.separator.concat(File.separator))[2];
        this.basePath = settings.get("path").toString().concat(File.separator).concat(base);
        this.jsonPath = settings.get("json").toString();
        this.path = settings.get("path").toString();
    }
}
