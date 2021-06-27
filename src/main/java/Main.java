import db.*;
import me.tongfei.progressbar.ProgressBar;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.yaml.snakeyaml.Yaml;
import utils.Utils;

import java.io.*;
import java.net.URISyntaxException;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.security.CodeSource;
import java.util.*;
import java.util.stream.Collectors;

public class Main {

    public static void main(String[] args) throws Exception {

        Map<String, Object> settings = getSettings();
        ArrayList<String> bases = (ArrayList<String>) settings.get("bases");

        for (String base : bases) {

            LoadSettings data = new LoadSettings(base, settings);

            fillModuleMethods(data);
            fillModuleCount(data);
            fillInnerMethods(data);
            fillCount(data);
            fillMetadata(data);
            fillSubsystem(data);

        }
    }

    private static void fillSubsystem(LoadSettings data) {

        File root = new File(data.basePath.concat(File.separator).concat("Subsystems"));

        List<Subsystem> list = new ArrayList<>();

        for (File file : root.listFiles()) {

            if (file.isDirectory())
                continue;

            NodeList nodeList = Utils.getNodeList(file, "Content");

            Subsystem ss = new Subsystem();
            ss.content = Utils.getContent(nodeList);
            ss.name = file.getName().replace(".xml", "");
            ss.path = file.getAbsolutePath().replace(file.getName(), "");
            ss.base = data.base;
            list.add(ss);

            String p = ss.path.concat(ss.name).concat(File.separator).concat("Subsystems");
            if (!new File(p).exists())
                continue;
            getSubsystemRecursive(ss, new File(p), data.base);

        }

        String filePath = data.jsonPath.concat(File.separator).concat(data.base).concat(" ").concat("subsystem.json");
        Utils.saveListToJSON(list, filePath);
    }

    private static void getSubsystemRecursive(Subsystem ss, File innerRoot, String base) {

        for (File file : innerRoot.listFiles()) {

            if (file.isDirectory() || file.getName().equals("CommandInterface"))
                continue;

            NodeList nodeList = Utils.getNodeList(file, "Content");
            Subsystem child = new Subsystem();
            child.base = base;
            child.content = Utils.getContent(nodeList);
            child.name = file.getName().replace(".xml", "");
            child.path = file.getAbsolutePath().replace(file.getName(), "").concat(child.name);
            ss.child.add(child);

            String p = child.path.concat(File.separator).concat("Subsystems");
            if (!new File(p).exists())
                continue;
            getSubsystemRecursive(child, new File(p), base);

        }
    }

    private static Map<String, Object> getSettings() throws URISyntaxException {

        CodeSource codeSource = Main.class.getProtectionDomain().getCodeSource();
        File jarFile = new File(codeSource.getLocation().toURI().getPath());
        String currentPath = jarFile.getParentFile().getPath();

        String settingsPath = currentPath.concat(File.separator).concat("settings.yml");
        InputStream inputStream = null;
        try {
            inputStream = new FileInputStream(new File(settingsPath));
        } catch (FileNotFoundException e) {
            throw new RuntimeException(e);
        }

        return new Yaml().load(inputStream);
    }

    private static void fillModuleMethods(LoadSettings data) {

        String title = data.base + " - " + "Searching all methods ";
        int count = data.bslFiles.size();

        ProgressBar pb = new ProgressBar(title, count);
        pb.start();
        for (String bsl : data.bslFiles) {
            File file = new File(bsl);
            List<Method> moduleMethods = new ArrayList<>();
            analyzeModule(file.toString(), data.methodList, moduleMethods);
            data.moduleMethods.put(bsl, moduleMethods);
            pb.step();
        }
        pb.stop();

    }

    private static void fillMetadata(LoadSettings data) {
        File root = new File(data.path);

        List<Metadata1C> list = new ArrayList<>();

        for (File file : root.listFiles()) {

            String base = file.toString().split(File.separator.concat(File.separator))[2];
            File confFile = new File(file.toString() + File.separator + "Configuration.xml");

            NodeList nodeList = Utils.getNodeList(confFile, "ChildObjects");
            for (int itr = 0; itr < nodeList.getLength(); itr++) {
                Node node = nodeList.item(itr);

                if (node.getNodeType() == Node.ELEMENT_NODE) {
                    Element eElement = (Element) node;
                    String name = eElement.getTagName();
                    String value = eElement.getFirstChild().getTextContent();
                    list.add(new Metadata1C(base, name, value));
                }
            }
        }
    }

    private static void fillModuleCount(LoadSettings data) {
        File root = new File(data.basePath);
        List<Module> modules = new ArrayList<>();
        String commonModulePath = data.basePath.concat(File.separator).concat("CommonModules");
        Utils.fillModuleProperties(commonModulePath, data.base, modules);
        String modulePath = data.jsonPath.concat(File.separator).concat(data.base).concat(" ").concat("module.json");

        for (Module m : modules)
            m.count = (int) data.methodList.stream().
                    filter(x -> x.base.equals(m.base) && x.moduleName.equals(m.name))
                    .count();

        Utils.saveListToJSON(modules, modulePath);
    }

    private static void analyzeModule(String filePath, ArrayList<Method> methodList, List<Method> moduleMethods) {

        List<String> allLines = null;
        try {
            allLines = Files.readAllLines(Paths.get(filePath), Charset.forName("UTF-8"));
        } catch (IOException e) {
            e.printStackTrace();
        }
        Map<String, String> moduleInfo = Utils.getModuleInfo(filePath);
        String area = "";
        for (int index = 0; index < allLines.size(); index++) {
            String strLine = allLines.get(index);
            strLine = Utils.formatStrLine(strLine);
            area = Utils.getArea(strLine, area);
            createMethod(index, strLine, filePath, moduleInfo, allLines, methodList, area, moduleMethods);
        }

    }

    private static void createMethod(int index, String line, String
            filePath, Map<String, String> moduleInfo, List<String> fileLines, ArrayList<Method> list, String
                                             area, List<Method> moduleMethods) {

        if (Utils.startMethod(line)) {
            Map<String, String> info = Utils.getProperty(line, index, fileLines);
            Method method = new Method(info, moduleInfo, index, filePath, line, area);
            list.add(method);
        }

        if (Utils.endMethod(line)) {
            Method method = list.get(list.size() - 1);
            method.ending = index;
            method.lineCount = method.ending - method.begin + 1;

            moduleMethods.add(method);

        }
    }

    private static void fillInnerMethods(LoadSettings data) {

        List<Method> commonMethods = data.methodList.stream()
                .filter(x -> x.isExport
                        && x.moduleType.equals("CommonModules"))
                .collect(Collectors.toList());

        List<String> namesCommonMethods = commonMethods.stream()
                .map(x -> x.fullName)
                .collect(Collectors.toList());

        List<String> filesPath = data.methodList.stream()
                .map(x -> x.filePath)
                .collect(Collectors.toList());

        Set<String> set = new HashSet<>(filesPath);
        filesPath.clear();
        filesPath.addAll(set);

        ProgressBar pb = new ProgressBar(data.base + " - " + "Filling inner methods", filesPath.size());
        pb.start();

        for (String path : filesPath) {

            pb.step();

            List<Method> moduleMethods = data.moduleMethods.get(path);

            List<String> localMethodsName = moduleMethods.stream()
                    .filter(x -> x.filePath.equals(path))
                    .map(x -> x.name)
                    .collect(Collectors.toList());

            List<String> lines = Utils.getStringLines(path);

            for (Method localModule : moduleMethods) {
                ArrayList<String> methods = new ArrayList<>();

                String body = "";
                for (int i = localModule.begin + 1; i < localModule.ending; ++i) {

                    String lineIWant = Utils.formatStrLine(lines.get(i));
                    body = body.concat(lineIWant);

                    if (Utils.skipLine(lineIWant))
                        continue;

                    List<String> foundMethods = Utils.getCallsMethods(lineIWant);

                    for (String method : foundMethods) {
                        if (localMethodsName.contains(method)) {

                            Method pp = moduleMethods.stream()
                                    .filter(x -> x.name.equals(method)
                                            && x.filePath.equals(path))
                                    .findAny().orElse(null);

                            methods.add(method);
                            data.counting.add(new Called(localModule, method, data.base, pp.filePath));

                        } else {
                            if (namesCommonMethods.contains(method)) {

                                Method pp = commonMethods.stream()
                                        .filter(x -> x.fullName.equals(method))
                                        .findAny().orElse(null);

                                String[] arr = method.split("\\.");
                                methods.add(method);
                                data.counting.add(new Called(localModule, arr[1], data.base, pp.filePath));
                            }
                        }

                    }
                }

                localModule.calledMethods = methods.stream().collect(Collectors.joining(","));
                localModule.countMethods = methods.size();
                localModule.bodyHash = Utils.MD5(body);

                Set<String> uniqueMethods = new HashSet<>(methods);
                methods.clear();
                methods.addAll(uniqueMethods);

                localModule.uniqueCountMethods = uniqueMethods.size();
                localModule.uniqueCalledMethods = methods.stream().collect(Collectors.joining(","));
            }
        }

        pb.stop();

    }

    private static void fillCount(LoadSettings data) {

        List<List<Method>> parts = Utils.getParts(data.methodList);

        Map<String, Long> counted = data.counting.stream()
                .collect(Collectors.groupingBy(x -> x.child, Collectors.counting()));

//        if (methodList.size() > 350000) {
//            for (int i = 0; i < parts.size(); i++) {
//                fillCount(parts.get(i), counted, i);
//                String path = "D:\\json\\" + base + " - " + i + ".json";
//                utils.Utils.saveListToJSON(parts.get(i), path);
//            }
//        } else {
        for (int i = 0; i < parts.size(); i++)
            fillCountBackground(parts.get(i), counted, i, data);
//        }

    }

    private static void fillCountBackground(List<Method> list, Map<String, Long> counted, int order, LoadSettings data) {

        Thread t1 = new Thread(new Runnable() {
            @Override
            public void run() {
                fillCount(list, counted, order);
                String path = data.jsonPath + File.separator + data.base + " - " + order + ".json";
                Utils.saveListToJSON(list, path);
            }
        });
        t1.start();

    }

    private static void fillCount(List<Method> list, Map<String, Long> counted, int order) {

        ProgressBar pb = new ProgressBar(order + " - " + "fill the number of calls", list.size());
        pb.start();

        for (Method element : list) {

            String name = element.name + "#" + element.filePath;
            long count = counted.entrySet().stream()
                    .filter(e -> e.getKey().equals(name))
                    .map(Map.Entry::getValue)
                    .findAny()
                    .orElse((long) 0);

            element.count = (int) count;
            pb.step();
        }

        pb.stop();

    }

}

