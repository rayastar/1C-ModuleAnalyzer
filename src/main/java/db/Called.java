package db;


public class Called {

    private String parent;
    public String child;
    private String base;
    private String className;
    private String objectName;
    private String moduleType;
    private String fileName;
    boolean isFunction;
    private int countMethods;
    private String param;

    public Called(Method method, String child, String base, String filePath) {
        this.parent = method.fullName;
        this.child = child + "#" + filePath;
        this.base = base;
        this.className = method.className;
        this.objectName = method.objectName;
        this.moduleType = method.moduleType;
        this.isFunction = method.isFunction;
        this.countMethods = method.countMethods;
        this.param = method.parameters;
        this.fileName = method.filePath;
    }

}
