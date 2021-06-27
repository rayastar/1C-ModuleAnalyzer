package utils;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import db.Called;
import db.Method;
import db.Module;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class Utils {

    public static List<String> getFilePathModules(File root) throws Exception {
        List<String> bslFiles = new ArrayList<>();
        for (File file : root.listFiles())
            checkFile(file, bslFiles);
        return bslFiles;
    }

    public static List<List<Method>> getParts(ArrayList<Method> methodList) {

        int cores = Runtime.getRuntime().availableProcessors() / 2;
        int countPart = (int) Math.floor(methodList.size() / cores);

        List<List<Method>> parts = chopped(methodList, countPart);

        if (parts.size() != cores) {
            parts.get(parts.size() - 2).addAll(parts.get(parts.size() - 1));
            parts.remove(parts.size() - 1);
        }

        return parts;
    }

    public static List<String> getCallsMethods(String str) {

        List<String> variants = new ArrayList<>();

        if (!str.contains("("))
            return variants;

        String res = containsMethodCall(str);
        if (!res.isEmpty()) {
            variants.add(res);
            return variants;
        }

        String arr[] = str.split("\\.");

        ///
        if (arr.length == 1 && str.contains("(") && str.contains(")")) {
            String[] ar = str.substring(0, str.indexOf("(")).split(" ");
            variants.add(ar[ar.length - 1]);
            return variants;
        }

        int index = 0;
        while (true) {

            if (index == arr.length - 1)
                break;

            String arLeft[] = arr[index].split(" ");
            if (arLeft.length == 0) {
                index++;
                continue;
            }

            String firstWord = arLeft[arLeft.length - 1];

            String parts[] = firstWord.split("\\(");
            if (parts.length == 2 && !parts[0].isEmpty()) {
                variants.add(parts[0]);
                break;
            }

            String arRight[] = arr[index + 1].split(" ");


            if (arRight.length == 0 || !arRight[0].contains("(")) {
                index++;
                continue;
            }

            String secondWord = arRight[0].split("\\(")[0];

            String result = firstWord.concat(".").concat(secondWord);
            result = result.replaceAll("[\\\"\"(]", "");
            variants.add(result);
            index++;

            if (index == arr.length - 1)
                break;
        }

        return variants;
    }

    public static List<String> getStringLines(String path) {
        List<String> lines = null;
        try {
            lines = Files.readAllLines(Paths.get(path));
        } catch (IOException e) {
            e.printStackTrace();
        }

        return  lines;
    }

    public static Map<String, String> getProperty(String line, int index, List<String> fileLines) {


        Map<String, String> map = new HashMap<>();

        String[] arr = line.split("\\(");
        String[] typeAndName = arr[0].split(" ");
        boolean isExport = line.endsWith("Экспорт") || line.endsWith("Export");

        String directive = "";
        if (index != 0 && fileLines.get(index - 1).startsWith("&"))
            directive = fileLines.get(index - 1).replace("&", "");

        String parameters = getParameters(line, index, fileLines);

        map.put("parameters", parameters.replace("  ", " "));
        map.put("type", typeAndName[0]);
        map.put("name", typeAndName[1]);
        map.put("isExport", String.valueOf(isExport));
        map.put("directive", directive.trim());

        return map;

    }

    public static Map<String, String> getModuleInfo(String path) {

        String slash = File.separator.concat(File.separator);
        String[] arr = path.split(slash);
        String moduleType = arr[arr.length - 1].replace(".bsl", "");

        String moduleName = "";
        if (arr[3].equals("CommonModules")) {
            moduleName = arr[4];
            moduleType = "CommonModules";
        }

        Map<String, String> moduleInfo = new HashMap<>();
        moduleInfo.put("className", arr[3]);
        moduleInfo.put("objectName", arr[4]);
        moduleInfo.put("moduleName", moduleName);
        moduleInfo.put("moduleType", moduleType);

        return moduleInfo;

    }

    public static boolean startMethod(String line) {
        return line.startsWith("Процедура ") || line.startsWith("Функция ");
    }

    public static boolean endMethod(String line) {
        return line.startsWith("КонецПроцедуры") || line.startsWith("КонецФункции");
    }

    public static boolean skipLine(String lineIWant) {
        return lineIWant.length() == 0 || lineIWant.startsWith("//") || lineIWant.startsWith("|");
    }

    public static String MD5(String md5) {
        try {
            java.security.MessageDigest md = java.security.MessageDigest.getInstance("MD5");
            byte[] array = md.digest(md5.getBytes());
            StringBuffer sb = new StringBuffer();
            for (int i = 0; i < array.length; ++i) {
                sb.append(Integer.toHexString((array[i] & 0xFF) | 0x100).substring(1, 3));
            }
            return sb.toString();
        } catch (java.security.NoSuchAlgorithmException ignored) {
        }
        return null;
    }

    public static String formatStrLine(String strLine) {

        strLine = strLine.replace("\t", " ");
        strLine = strLine.replace("  ", " ");
        strLine = strLine.replace("\uFEFF", "");

        return strLine.trim();
    }

    public static String getArea(String strLine, String area) {

        String value = "";
        if (strLine.startsWith("#Область")) {
            String[] arr = strLine.split(" ");
            value = arr[1];
        } else if (strLine.startsWith("#КонецОбласти")) {
            value = "";
        } else {
            value = area;
        }

        return value;
    }

    public static void saveListToJSON(List<?> list, String fileName){
        Gson gson = new GsonBuilder().setPrettyPrinting().create();
        String json = gson.toJson(list);
        writeUnicodeClassic(fileName, json);
    }

    public static void fillModuleProperties(String path, String base, List<Module> modules) {

        File dir = new File(path);
        File[] files = dir.listFiles((d, name) -> name.endsWith(".xml"));

        for (File file : files) {

            Module module = new Module();
            module.name = file.getName().replace(".xml", "");
            module.base = base;

            DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
            DocumentBuilder db = null;
            try {
                db = dbf.newDocumentBuilder();
            } catch (ParserConfigurationException e) {
                e.printStackTrace();
            }
            Document doc = null;
            try {
                doc = db.parse(file);
            } catch (SAXException e) {
                e.printStackTrace();
            } catch (IOException e) {
                e.printStackTrace();
            }
            doc.getDocumentElement().normalize();
            NodeList nodeList = doc.getElementsByTagName("Properties");

            NodeList list = nodeList.item(0).getChildNodes();
            for (int itr = 0; itr < list.getLength(); itr++) {
                Node node = list.item(itr);

                switch (node.getNodeName()) {
                    case "Global":
                        module.global = Boolean.valueOf(node.getTextContent());
                        break;
                    case "ClientManagedApplication":
                        module.client = Boolean.valueOf(node.getTextContent());
                        break;
                    case "Server":
                        module.server = Boolean.valueOf(node.getTextContent());
                        break;
                    case "ExternalConnection":
                        module.externalConnection = Boolean.valueOf(node.getTextContent());
                        break;
                    case "ServerCall":
                        module.serverCall = Boolean.valueOf(node.getTextContent());
                        break;
                    case "Privileged":
                        module.privileged = Boolean.valueOf(node.getTextContent());
                        break;
                    case "ReturnValuesReuse":
                        module.returnValuesReuse = node.getTextContent();
                    default:
                }
            }
            modules.add(module);
        }

    }

    public static NodeList getNodeList(File file, String tag) {

        DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
        DocumentBuilder db = null;
        Document doc = null;
        try {
            db = dbf.newDocumentBuilder();
            doc = db.parse(file);
        } catch (ParserConfigurationException | SAXException | IOException e) {
            throw new RuntimeException(e);
        }

        doc.getDocumentElement().normalize();
        NodeList nodeListRoot = doc.getElementsByTagName(tag);

        return nodeListRoot.item(0).getChildNodes();
    }


    private static <T> List<List<T>> chopped(List<T> list, final int L) {
        List<List<T>> parts = new ArrayList<List<T>>();
        final int N = list.size();
        for (int i = 0; i < N; i += L) {
            parts.add(new ArrayList<T>(
                    list.subList(i, Math.min(N, i + L)))
            );
        }
        return parts;
    }

    private static boolean isModule(String name) {
        return name.endsWith("bsl");
    }

    private static String getParameters(String line, int index, List<String> fileLines) {

        int start = line.indexOf("(");
        int end = line.indexOf(")");
        String parameters = "";
        if (line.contains("()")) {

        } else if (!line.contains("(") && !line.contains(")")) {
            line = fileLines.get(index + 1).trim();
            start = line.indexOf("(");
            end = line.indexOf(")");
            parameters = line.substring(start + 1, end);
            ;
        } else if (line.contains(")")) {
            parameters = line.substring(start + 1, end);
        } else {

            parameters = line.substring(start, line.length());

            int ind = index + 1;
            String next = "";
            while (!next.contains(")")) {
                next = fileLines.get(ind).trim();
                parameters = parameters.concat(" ").concat(next);
                ind++;
            }
            parameters = parameters.trim();
            start = parameters.indexOf("(");
            end = parameters.indexOf(")");
            parameters = parameters.substring(start + 1, end);
            parameters = parameters.trim();

        }

        return parameters;
    }

    private static String containsMethodCall(final String s) {
        String regex = "([\\\\n\\r]*[а-яА-Я09_]+\\.[\\s\\n\\r]*[а-яА-Я09_]+)[\\s\\n\\r]*(?=\\(.*)";
        Pattern funcPattern = Pattern.compile(regex);
        Matcher m = funcPattern.matcher(s); //Matcher is used to match pattern with string
        String result = "";
        if (m.find()) {
            result = m.group(0);
        }

        return result;

    }

    private static void writeUnicodeClassic(String fileName, String text) {

        File file = new File(fileName);

        try (FileOutputStream fos = new FileOutputStream(file);
             OutputStreamWriter osw = new OutputStreamWriter(fos, StandardCharsets.UTF_8);
             BufferedWriter writer = new BufferedWriter(osw)
        ) {

            writer.append(text);

        } catch (IOException e) {
            e.printStackTrace();
        }

    }

    private static void checkFile(File file, List<String> bslFiles) throws Exception {
        if (file.isDirectory()) {
            getFilesRecursive(file, bslFiles);
        } else {
            if (isModule(file.toString())) {
                bslFiles.add(file.toString());
            }
        }
    }

    private static void getFilesRecursive(File pFile, List<String> bslFiles) throws Exception {
        for (File files : pFile.listFiles()) {
            checkFile(files, bslFiles);
        }
    }

    public static List<String> getContent(NodeList nodeList) {
        List<String> content = new ArrayList<>();
        for (int itr = 0; itr < nodeList.getLength(); itr++) {
            Node node = nodeList.item(itr);

            if (node.getNodeType() == Node.ELEMENT_NODE) {
                Element eElement = (Element) node;
                String value = eElement.getFirstChild().getTextContent();
                content.add(value);
            }
        }

        return content;
    }
}
