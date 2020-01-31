package ml.dent.trab;

import java.util.List;
import java.util.stream.Collectors;

public class TrabLambda implements TrabCallable {
    Expr.Lambda lambda;
    Environment closure;

    TrabLambda(Expr.Lambda lambda, Environment closure) {
        this.lambda = lambda;
        this.closure = closure;
    }


    @Override
    public int arity() {
        return lambda.arguments.size();
    }

    @Override
    public Object call(Interpreter interpreter, Token callee, List<Object> arguments) {
        for (int i = 0; i < arguments.size(); i++) {
            closure.define(lambda.arguments.get(i).lexeme, arguments.get(i));
        }
        Stmt.Block block = null;
        if (lambda.right instanceof Stmt.Block) block = (Stmt.Block) lambda.right;
        else if (lambda.right instanceof Stmt.Return)
            block = new Stmt.Block(List.of((Stmt.Return) lambda.right));
        try {
            interpreter.executeBlock(block, closure);
        } catch (Return ret) {
            return ret.value;
        }
        return new TrabNull();
    }


    @Override
    public String toString() {
        return "<lambda " + lambda.arguments.stream().map(x -> x.lexeme).collect(Collectors.joining(", ")) + ">";
    }
}
