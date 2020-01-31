package ml.dent.trab;

import java.util.List;

public class ShuntedFunction implements TrabCallable {
    private TrabCallable func;
    private Object shunt;

    ShuntedFunction(TrabCallable func, Object shunt) {
        this.func = func;
        this.shunt = shunt;
    }

    @Override
    public int arity() {
        return func.arity() - 1;
    }

    @Override
    public Object call(Interpreter interpreter, Token callee, List<Object> arguments) {
        arguments.add(0, shunt);
        return func.call(interpreter, callee, arguments);
    }
}
