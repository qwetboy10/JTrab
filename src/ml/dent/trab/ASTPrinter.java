package ml.dent.trab;

import ml.dent.trab.Expr.Assign;
import ml.dent.trab.Expr.Binary;
import ml.dent.trab.Expr.Call;
import ml.dent.trab.Expr.Get;
import ml.dent.trab.Expr.Grouping;
import ml.dent.trab.Expr.Index;
import ml.dent.trab.Expr.Lambda;
import ml.dent.trab.Expr.Literal;
import ml.dent.trab.Expr.Logical;
import ml.dent.trab.Expr.Post;
import ml.dent.trab.Expr.TrabList;
import ml.dent.trab.Expr.TrabString;
import ml.dent.trab.Expr.Unary;
import ml.dent.trab.Expr.Variable;

public class ASTPrinter implements Expr.Visitor<String> {
	public String print(Expr expr) {
		return expr.accept(this);
	}

	private String paren(Expr... exprs) {
		StringBuffer buf = new StringBuffer();
		buf.append("(");
		for (Expr e : exprs)
			buf.append(print(e) + " ");
		buf.setLength(buf.length() - 1);
		buf.append(")");
		return buf.toString();
	}

	@Override
	public String visitLiteralExpr(Literal expr) {
		return expr.value.toString();
	}

	@Override
	public String visitTrabListExpr(TrabList expr) {
		StringBuffer buf = new StringBuffer();
		buf.append("[");
		for (Expr e : expr.values)
			buf.append(print(e) + ", ");
		buf.setLength(buf.length() - 2);
		buf.append("]");
		return buf.toString();
	}

	@Override
	public String visitGroupingExpr(Grouping expr) {
		return "(" + print(expr.expression) + ")";
	}

	@Override
	public String visitUnaryExpr(Unary expr) {
		return "(" + expr.operator.lexeme + " " + print(expr.right) + ")";
	}

	@Override
	public String visitPostExpr(Post expr) {
		return "(" + expr.operator.lexeme + "(post) " + print(expr.left) + ")";
	}

	@Override
	public String visitCallExpr(Call expr) {
		StringBuffer buf = new StringBuffer("(call ");
		buf.append(print(expr.left));
		for (Expr e : expr.arguments)
			buf.append(" " + print(e));
		buf.append(")");
		return buf.toString();
	}

	@Override
	public String visitLambdaExpr(Lambda expr) {
		StringBuffer buf = new StringBuffer("(lambda ");
		for (Token e : expr.arguments)
			buf.append(e.lexeme + " ");
		//buf.append(print(expr.right));
		buf.append(")");
		return buf.toString();
	}

	@Override
	public String visitBinaryExpr(Binary expr) {
		return "(" + expr.operator.lexeme + " " + print(expr.left) + " " + print(expr.right) + ")";
	}

	@Override
	public String visitLogicalExpr(Logical expr) {
		return "(" + expr.operator.lexeme + " " + print(expr.left) + " " + print(expr.right) + ")";
	}

	@Override
	public String visitAssignExpr(Assign expr) {
		return "(" + expr.operator.lexeme + " " + expr.name.lexeme + " " + print(expr.value) + ")";
	}

	@Override
	public String visitGetExpr(Get expr) {
		return "(get " + print(expr.left) + " " + expr.name.lexeme + ")";
	}

	@Override
	public String visitVariableExpr(Variable expr) {
		return expr.name.lexeme;
	}

	@Override
	public String visitIndexExpr(Index expr) {
		return "([] " + print(expr.operand) + " " + print(expr.start) + ")";
	}

	@Override
	public String visitTrabStringExpr(TrabString expr) {
		StringBuffer buf = new StringBuffer("\"");
		for(Expr e : expr.values)
		{
			buf.append(print(e));
		}
		buf.append("\"");
		return buf.toString();
	}

}
