package ml.dent.trab;

import ml.dent.trab.Expr.*;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import static ml.dent.trab.TokenType.*;

public class Interpreter implements Expr.Visitor<Object>, Stmt.Visitor<Void> {
    private final Environment globals = new Environment();
    Environment environment = globals;
    private boolean isRepl;
    private boolean print;

    public Interpreter() {
        globals.define("print", new TrabCallable() {

            @Override
            public int arity() {
                return 1;
            }

            @Override
            public Object call(Interpreter interpreter, Token callee, List<Object> arguments) {
                System.out.println(interpreter.stringify(arguments.get(0)));
                return new TrabNull();
            }

            @Override
            public String toString() {
                return "<native fn>";
            }
        });
        globals.define("clock", new TrabCallable() {

            @Override
            public int arity() {
                return 0;
            }

            @Override
            public Object call(Interpreter interpreter, Token callee, List<Object> arguments) {
                return System.currentTimeMillis();
            }

            @Override
            public String toString() {
                return "<native fn>";
            }
        });
        globals.define("InternalList", new TrabClassable() {
            @Override
            public TrabCallable getFunction(String s) {
                switch (s) {
                    case "add":
                        return new TrabCallable() {

                            @Override
                            public int arity() {
                                return 2;
                            }

                            @Override
                            public Object call(Interpreter interpreter, Token callee, List<Object> arguments) {
                                interpreter.checkListOperand(callee, arguments.get(0));
                                List list = (List) arguments.get(0);
                                Object add = arguments.get(1);
                                list.add(add);
                                return new TrabNull();
                            }

                            @Override
                            public String toString() {
                                return "<native fn>";
                            }
                        };
                    case "set":
                        return new TrabCallable() {

                            @Override
                            public int arity() {
                                return 3;
                            }

                            @Override
                            public Object call(Interpreter interpreter, Token callee, List<Object> arguments) {
                                interpreter.checkListOperand(callee, arguments.get(0));
                                interpreter.checkNumberOperand(callee, arguments.get(1));
                                List list = (List) arguments.get(0);
                                int index = (int) (double) arguments.get(1);
                                Object element = arguments.get(2);
                                list.set(index, element);
                                return new TrabNull();
                            }

                            @Override
                            public String toString() {
                                return "<native fn>";
                            }
                        };
                    case "length":
                        return new TrabCallable() {

                            @Override
                            public int arity() {
                                return 1;
                            }

                            @Override
                            public Object call(Interpreter interpreter, Token callee, List<Object> arguments) {
                                interpreter.checkListOperand(callee, arguments.get(0));
                                List list = (List) arguments.get(0);
                                return (Double) (double) list.size();
                            }

                            @Override
                            public String toString() {
                                return "<native fn>";
                            }
                        };
                }
                return null;
            }

            @Override
            public String toString() {
                return "<native class>";
            }
        });
    }

    public void run(List<Stmt> statements, boolean isRepl, String stin) {
        this.isRepl = isRepl;
        globals.define("stin", stin);
        try {
            for (Stmt stmt : statements) {
                print = true;
                execute(stmt);
            }
        } catch (RuntimeError error) {
            Trab.runtimeError(error);
        }
    }

    public Object evaluate(Expr expr) {
        if (expr == null) return null;
        return expr.accept(this);
    }

    public void execute(Stmt stmt) {
        stmt.accept(this);
    }

    public void executeBlock(Stmt.Block block, Environment newEnv) {
        this.environment = newEnv;
        for (Stmt stmt : block.statements) execute(stmt);
        this.environment = this.environment.parent;
    }

    @Override
    public Void visitBlockStmt(Stmt.Block stmt) {
        executeBlock(stmt, new Environment(environment));
        return null;
    }

    @Override
    public Void visitClassStmt(Stmt.Class stmt) {
        environment.define(stmt.name.lexeme, new TrabClass(stmt, new Environment(environment)));
        return null;
    }

    @Override
    public Void visitExpressionStmt(Stmt.Expression stmt) {
        evaluate(stmt.expression);
        return null;
    }

    @Override
    public Void visitPrintableExpressionStmt(Stmt.PrintableExpression stmt) {
        if (print) {
            if (stmt.expression instanceof Call && ((Call) stmt.expression).left instanceof Variable) {
                if (((Variable) ((Call) stmt.expression).left).name.lexeme.equals("print")) {
                    print = false;
                    evaluate(stmt.expression);
                    return null;
                }
            }
            print = false;
            Object o = evaluate(stmt.expression);
            if (o != null && isRepl) System.out.println(stringify(o));
        } else evaluate(stmt.expression);
        return null;
    }


    @Override
    public Void visitIfStmt(Stmt.If stmt) {
        if (isTruthy(stmt.ifToken, evaluate(stmt.condition))) {
            execute(stmt.thenBranch);
        } else execute(stmt.elseBranch);
        return null;
    }

    @Override
    public Void visitReturnStmt(Stmt.Return stmt) {
        throw new Return(evaluate(stmt.value));
    }

    @Override
    public Void visitVarStmt(Stmt.Var stmt) {
        if (environment.isDefinedInClosure(stmt.name.lexeme))
            throw new RuntimeError(stmt.name, "Variable cannot be redefined in same context");
        environment.define(stmt.name.lexeme, evaluate(stmt.initializer));
        return null;
    }

    @Override
    public Void visitWhileStmt(Stmt.While stmt) {
        while (isTruthy(stmt.whileToken, evaluate(stmt.condition))) {
            execute(stmt.body);
        }
        return null;
    }

    @Override
    public Object visitLiteralExpr(Literal expr) {

        return expr.value;
    }

    @Override
    public Object visitTrabStringExpr(TrabString expr) {

        StringBuffer buf = new StringBuffer();
        for (Expr e : expr.values) {
            buf.append(stringify(evaluate(e)));
        }
        return buf.toString();
    }

    @Override
    public Object visitVariableExpr(Variable expr) {
        return getVar(expr.name);
    }

    @Override
    public Object visitTrabListExpr(TrabList expr) {

        List<Object> list = new ArrayList<>();
        for (Expr e : expr.values)
            list.add(evaluate(e));
        return list;
    }

    @Override
    public Object visitGroupingExpr(Grouping expr) {

        return evaluate(expr.expression);
    }

    @Override
    public Object visitCallExpr(Call expr) {
        Object o = evaluate(expr.left);
        if (o instanceof TrabCallable) {
            TrabCallable call = (TrabCallable) o;
            if (expr.arguments.size() != call.arity())
                throw new RuntimeError(expr.operator, "Expected " + call.arity() + " arguments but got " + expr.arguments.size());
            List args = new ArrayList();
            args.addAll(expr.arguments.stream().map(this::evaluate).collect(Collectors.toList()));
            return ((TrabCallable) o).call(this, expr.operator, args);
        } else throw new RuntimeError(expr.operator, "Only Lambdas, Functions, and Methods can be called");
    }

    @Override
    public Object visitGetExpr(Get expr) {
        Object o = evaluate(expr.left);
        TrabClassable trabClass = null;
        if (o instanceof List) {
            trabClass = (TrabClassable) globals.get("InternalList");
            TrabCallable temp = trabClass.getFunction(expr.name.lexeme);
            if (temp == null) throw new RuntimeError(expr.name, "Method " + expr.name.lexeme + " does not exist");
            return new ShuntedFunction(temp, o);
        } else if (o instanceof TrabClassable)
            trabClass = (TrabClassable) o;
        else throw new RuntimeError(expr.name, "Can only call '.' on class");
        Object temp = trabClass.getFunction(expr.name.lexeme);
        if (temp == null) {
            System.out.println(expr.toString());
            throw new RuntimeError(expr.name, "Method " + expr.name.lexeme + " does not exist");
        }
        return temp;
    }

    @Override
    public Object visitIndexExpr(Index expr) {

        if (expr.start != null && expr.end == null && expr.step == null) {
            Object left = evaluate(expr.operand);
            Object index = evaluate(expr.start);
            if (left instanceof List) {
                if (index instanceof Double) {
                    int ind = (int) (double) index;
                    if (ind < 0) ind += ((List) left).size();
                    if (ind < 0 || ind >= ((List) left).size())
                        throw new RuntimeError(expr.bracket, "List index out of bounds");
                    return ((List) left).get(ind);
                } else
                    throw new RuntimeError(expr.bracket, "Index must be number");
            } else if (left instanceof String) {
                int ind = (int) (double) index;
                if (ind < 0 || ind >= ((String) left).length())
                    throw new RuntimeError(expr.bracket, "String index out of bounds");
                return ((String) left).substring(ind, ind + 1);
            } else
                throw new RuntimeError(expr.bracket, "Index can only be called on list");
        } else {
            Object left = evaluate(expr.operand);
            Object start = evaluate(expr.start);
            Object end = evaluate(expr.end);
            Object step = evaluate(expr.step);
            if (left instanceof List) {
                if (start != null)
                    checkNumberOperand(expr.bracket, start);
                if (end != null)
                    checkNumberOperand(expr.bracket, end);
                if (step != null)
                    checkNumberOperand(expr.bracket, step);
                int s = start == null ? 0 : (int) (double) start;
                int e = end == null ? ((List) left).size() : (int) (double) end;
                int st = step == null ? 1 : (int) (double) (step);
                List ll = ((List) left);
                List<Object> n = new ArrayList<>();
                if (s < 0) s += ll.size();
                if (e < 0) e += ll.size();
                if (st < 0) {
                    for (int i = e - 1; i >= s; i += st)
                        n.add(ll.get(i));
                } else {
                    for (int i = s; i < e; i += st) {
                        n.add(ll.get(i));
                    }
                }
                return n;
            } else if (left instanceof String) {
                if (start != null)
                    checkNumberOperand(expr.bracket, start);
                if (end != null)
                    checkNumberOperand(expr.bracket, end);
                if (step != null)
                    checkNumberOperand(expr.bracket, step);
                int s = start == null ? 0 : (int) (double) start;
                int e = end == null ? ((String) left).length() : (int) (double) end;
                int st = step == null ? 1 : (int) (double) (step);
                String string = ((String) left);
                StringBuffer n = new StringBuffer();
                if (s < 0) s += string.length();
                if (e < 0) e += string.length();
                if (st < 0) {
                    for (int i = e - 1; i >= s; i += st)
                        n.append(string, i, i + 1);
                } else {
                    for (int i = s; i < e; i += st) {
                        n.append(string, i, i + 1);
                    }
                }
                return n.toString();
            } else
                throw new RuntimeError(expr.bracket, "Index can only be called on list");
        }
    }

    @Override
    public Object visitUnaryExpr(Unary expr) {

        switch (expr.operator.type) {
            case MINUS:
                Object o = evaluate(expr.right);
                checkNumberOperand(expr.operator, o);
                return -1.0 * (Double) o;
            case BANG:
                return !isTruthy(expr.operator, evaluate(expr.right));
            case PLUS_PLUS:
                if (!(expr.right instanceof Expr.Variable))
                    throw new RuntimeError(expr.operator, "Operand must be variable");
                Expr.Variable variable = (Expr.Variable) expr.right;
                Object o2 = evaluate(expr.right);
                checkNumberOperand(expr.operator, o2);
                environment.update(variable.name.lexeme, (Double) o2 + 1);
                return getVar(variable.name);
            case MINUS_MINUS:
                if (!(expr.right instanceof Expr.Variable))
                    throw new RuntimeError(expr.operator, "Operand must be variable");
                Expr.Variable variable2 = (Expr.Variable) expr.right;
                Object o3 = evaluate(expr.right);
                checkNumberOperand(expr.operator, o3);
                environment.update(variable2.name.lexeme, (Double) o3 - 1);
                return getVar(variable2.name);
            case QUESTION:
                Object command = evaluate(expr.right);
                checkStringOperand(expr.operator, command);
                String s = (String) command;
                try {
                    Process p = Runtime.getRuntime().exec(s);
                    BufferedReader in = new BufferedReader(new InputStreamReader(p.getInputStream()));
                    String output = "";
                    while (in.ready()) output += in.readLine() + "\n";
                    return output.trim();
                } catch (Exception e) {

                    return new TrabNull();
                }
        }
        return null;
    }

    @Override
    public Object visitPostExpr(Post expr) {
        if (!(expr.left instanceof Expr.Variable)) throw new RuntimeError(expr.operator, "Operand must be variable");
        Expr.Variable variable = (Expr.Variable) expr.left;
        checkNumberOperand(expr.operator, getVar(variable.name));
        Object o = evaluate(variable);
        if (expr.operator.type == PLUS_PLUS)
            environment.update(variable.name.lexeme, (Double) o + 1.0);
        else if (expr.operator.type == MINUS_MINUS)
            environment.update(variable.name.lexeme, (Double) o - 1.0);
        return o;
    }

    @Override
    public Object visitBinaryExpr(Binary expr) {

        Object left = evaluate(expr.left);
        Object right = evaluate(expr.right);
        switch (expr.operator.type) {
            case PLUS:
                if (left instanceof Double && right instanceof Double)
                    return (Double) left + (Double) right;
                if (left instanceof String || right instanceof String)
                    return stringify(left) + stringify(right);
                else
                    throw new RuntimeError(expr.operator, "Invalid types");
            case MINUS:
                checkNumberOperands(expr.operator, left, right);
                return (Double) left - (Double) right;
            case STAR:
                checkNumberOperands(expr.operator, left, right);
                return (Double) left * (Double) right;
            case SLASH:
                checkNumberOperands(expr.operator, left, right);
                return (Double) left / (Double) right;
            case PERCENT:
                checkNumberOperands(expr.operator, left, right);
                return (Double) left % (Double) right;
            case STAR_STAR:
                checkNumberOperands(expr.operator, left, right);
                return Math.pow((Double) left, (Double) right);
            case LESS_EQUAL:
                checkNumberOperands(expr.operator, left, right);
                return (Double) left <= (Double) right;
            case LESS:
                checkNumberOperands(expr.operator, left, right);
                return (Double) left < (Double) right;
            case GREATER_EQUAL:
                checkNumberOperands(expr.operator, left, right);
                return (Double) left >= (Double) right;
            case GREATER:
                checkNumberOperands(expr.operator, left, right);
                return (Double) left > (Double) right;
            case EQUAL_EQUAL:
                return isEqual(left, right);

        }
        return null;
    }

    @Override
    public Object visitLogicalExpr(Logical expr) {

        switch (expr.operator.type) {
            case AND:
                if (isTruthy(expr.operator, evaluate(expr.left)))
                    return isTruthy(expr.operator, evaluate(expr.right));
                else
                    return false;
            case OR:
                if (isTruthy(expr.operator, evaluate(expr.left)))
                    return true;
                else
                    return isTruthy(expr.operator, evaluate(expr.right));
        }
        return null;
    }

    @Override
    public Object visitLambdaExpr(Lambda expr) {
        return new TrabLambda(expr, new Environment(environment));
    }

    @Override
    public Void visitFunctionStmt(Stmt.Function stmt) {
        environment.define(stmt.name.lexeme, new TrabFunction(stmt, new Environment(environment)));
        return null;
    }

    @Override
    public Object visitAssignExpr(Assign expr) {
        if (!environment.isDefined(expr.name.lexeme))
            throw new RuntimeError(expr.name, "Cannot assign undefined variable");
        Object o = evaluate(expr.value);
        if (expr.operator.type == EQUAL) {
            return environment.update(expr.name.lexeme, o);
        }
        checkNumberOperand(expr.operator, o);
        checkNumberOperand(expr.operator, getVar(expr.name));
        Double value = (Double) o;
        Double original = (Double) getVar(expr.name);
        switch (expr.operator.type) {
            case PLUS_EQUAL:
                return environment.update(expr.name.lexeme, value + original);
            case MINUS_EQUAL:
                return environment.update(expr.name.lexeme, original - value);
            case STAR_EQUAL:
                return environment.update(expr.name.lexeme, original * value);
            case SLASH_EQUAL:
                return environment.update(expr.name.lexeme, original / value);
            case PERCENT_EQUAL:
                return environment.update(expr.name.lexeme, original % value);
            case STAR_STAR_EQUAL:
                return environment.update(expr.name.lexeme, Math.pow(original, value));
        }
        return null;
    }

    private Object getVar(Token token) {
        if (!environment.isDefined(token.lexeme))
            throw new RuntimeError(token, "Cannot access unassigned variable");
        return environment.get(token.lexeme);
    }

    private String stringify(Object o) {

        if (o instanceof Double) {
            String s = o.toString();
            if (s.endsWith(".0"))
                return s.substring(0, s.length() - 2);
            else
                return s;
        }
        if (o instanceof List) {
            return stringifyList((List) o);
        }
        return o.toString();
    }

    private String stringifyList(List list) {
        StringBuffer buf = new StringBuffer();
        buf.append("[");
        for (Object o : list) {
            buf.append(stringify(o));
            buf.append(", ");
        }
        buf.setLength(buf.length() - 2);
        buf.append("]");
        return buf.toString();
    }

    private void checkNumberOperand(Token operator, Object operand) {

        if (operand instanceof Double)
            return;
        throw new RuntimeError(operator, "Operand must be a number");
    }

    private void checkListOperand(Token operator, Object operand) {

        if (operand instanceof List)
            return;
        throw new RuntimeError(operator, "Operand must be a list");
    }

    private void checkStringOperand(Token operator, Object operand) {

        if (operand instanceof String)
            return;
        throw new RuntimeError(operator, "Operand must be a string");
    }

    private void checkNumberOperands(Token operator, Object left, Object right) {

        if (left instanceof Double && right instanceof Double)
            return;
        throw new RuntimeError(operator, "Operands must be numbers");
    }

    private boolean isEqual(Object a, Object b) {

        return a.equals(b);
    }

    private boolean isTruthy(Token operator, Object object) {

        if (object instanceof Boolean)
            return (boolean) object;
        throw new RuntimeError(operator, "Operand must be boolean");
    }
}
