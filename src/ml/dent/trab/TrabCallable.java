package ml.dent.trab;

import java.util.List;

public interface TrabCallable {
    int arity();

    Object call(Interpreter interpreter, Token callee, List<Object> arguments);
}
