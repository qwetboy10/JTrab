package ml.dent.trab;

import java.util.HashMap;

public class Environment {
    final HashMap<String, Object> variables;
    final Environment parent;

    public Environment() {
        variables = new HashMap<>();
        parent = null;
    }

    public Environment(Environment parent) {
        variables = new HashMap<>();
        this.parent = parent;
    }

    public Object get(String s) {
        Object o = variables.get(s);
        if (o == null && parent != null) return parent.get(s);
        return o;
    }

    public Object define(String s, Object o) {
        return variables.put(s, o);
    }

    public Object update(String s, Object o) {
        if (variables.containsKey(s)) {
            variables.put(s, o);
            return o;
        } else if (parent != null) return parent.update(s, o);
        else return null;
    }

    public boolean isDefined(String s) {
        return variables.containsKey(s) || (parent != null && parent.isDefined(s));
    }

    public boolean isDefinedInClosure(String s) {
        return variables.containsKey(s);
    }
    public String toString()
    {
        return variables.toString();
    }

}
