package ml.dent.trab;

public class TrabClass implements TrabClassable {
    private Stmt.Class stmt;
    Environment closure;

    public TrabClass(Stmt.Class stmt, Environment closure) {
        this.stmt = stmt;
        this.closure = closure;
        bindFunctions();
    }

    private void bindFunctions() {
        for (Stmt.Function fun : stmt.methods) {
            closure.define(fun.name.lexeme, new TrabFunction(fun, new Environment(closure)));
        }
    }

    @Override
    public TrabFunction getFunction(String s) {
        return (TrabFunction) closure.get(s);
    }

    @Override
    public String toString() {
        return "<class " + stmt.name.lexeme + ">";
    }
}
