package db;

import java.util.ArrayList;
import java.util.List;

public class Subsystem {

    public String name;
    public String base;
    public String path;
    public List<String> content;
    public List<Subsystem> child = new ArrayList<>();

    public Subsystem(String base, String name, String value) {
        this.base = base;
        this.name = name;
    }

    public Subsystem() {

    }
}
