package ml.dent.trab;

import java.util.List;
import java.util.stream.Collectors;

public class TrabFunction implements TrabCallable {
    Stmt.Function fun;
    Environment closure;

    TrabFunction(Stmt.Function fun, Environment closure) {
        this.fun = fun;
        this.closure = closure;
    }


    @Override
    public int arity() {
        return fun.arguments.size();
    }

    @Override
    public Object call(Interpreter interpreter, Token callee, List<Object> arguments) {
        for (int i = 0; i < arguments.size(); i++) {
            closure.define(fun.arguments.get(i).lexeme, arguments.get(i));
        }
        Stmt.Block block = null;
        if (fun.body instanceof Stmt.Block) block = (Stmt.Block) fun.body;
        else if (fun.body instanceof Stmt.Expression) block = new Stmt.Block(List.of((Stmt.Expression) fun.body));
        try {
            interpreter.executeBlock(block, closure);
        } catch (Return ret) {
            return ret.value;
        }
        return new TrabNull();
    }

    @Override
    public String toString() {
        return "<function " + fun.arguments.stream().map(x -> x.lexeme).collect(Collectors.joining(", ")) + ">";
    }
}
