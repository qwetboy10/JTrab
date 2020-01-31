package ml.dent.trab;

import java.util.List;

abstract class Expr {
  interface Visitor<R> {
    R visitLiteralExpr(Literal expr);
    R visitTrabStringExpr(TrabString expr);
    R visitVariableExpr(Variable expr);
    R visitTrabListExpr(TrabList expr);
    R visitGroupingExpr(Grouping expr);
    R visitCallExpr(Call expr);
    R visitGetExpr(Get expr);
    R visitIndexExpr(Index expr);
    R visitUnaryExpr(Unary expr);
    R visitPostExpr(Post expr);
    R visitBinaryExpr(Binary expr);
    R visitLogicalExpr(Logical expr);
    R visitLambdaExpr(Lambda expr);
    R visitAssignExpr(Assign expr);
  }
  static class Literal extends Expr {
    Literal(Object value) {
      this.value = value;
    }

    <R> R accept(Visitor<R> visitor) {
      return visitor.visitLiteralExpr(this);
    }

    final Object value;
  }
  static class TrabString extends Expr {
    TrabString(List<Expr> values) {
      this.values = values;
    }

    <R> R accept(Visitor<R> visitor) {
      return visitor.visitTrabStringExpr(this);
    }

    final List<Expr> values;
  }
  static class Variable extends Expr {
    Variable(Token name) {
      this.name = name;
    }

    <R> R accept(Visitor<R> visitor) {
      return visitor.visitVariableExpr(this);
    }

    final Token name;
  }
  static class TrabList extends Expr {
    TrabList(List<Expr> values, Token start) {
      this.values = values;
      this.start = start;
    }

    <R> R accept(Visitor<R> visitor) {
      return visitor.visitTrabListExpr(this);
    }

    final List<Expr> values;
    final Token start;
  }
  static class Grouping extends Expr {
    Grouping(Expr expression) {
      this.expression = expression;
    }

    <R> R accept(Visitor<R> visitor) {
      return visitor.visitGroupingExpr(this);
    }

    final Expr expression;
  }
  static class Call extends Expr {
    Call(Token operator, Expr left, List<Expr> arguments) {
      this.operator = operator;
      this.left = left;
      this.arguments = arguments;
    }

    <R> R accept(Visitor<R> visitor) {
      return visitor.visitCallExpr(this);
    }

    final Token operator;
    final Expr left;
    final List<Expr> arguments;
  }
  static class Get extends Expr {
    Get(Expr left, Token name) {
      this.left = left;
      this.name = name;
    }

    <R> R accept(Visitor<R> visitor) {
      return visitor.visitGetExpr(this);
    }

    final Expr left;
    final Token name;
  }
  static class Index extends Expr {
    Index(Expr operand, Expr start, Expr end, Expr step, Token bracket) {
      this.operand = operand;
      this.start = start;
      this.end = end;
      this.step = step;
      this.bracket = bracket;
    }

    <R> R accept(Visitor<R> visitor) {
      return visitor.visitIndexExpr(this);
    }

    final Expr operand;
    final Expr start;
    final Expr end;
    final Expr step;
    final Token bracket;
  }
  static class Unary extends Expr {
    Unary(Token operator, Expr right) {
      this.operator = operator;
      this.right = right;
    }

    <R> R accept(Visitor<R> visitor) {
      return visitor.visitUnaryExpr(this);
    }

    final Token operator;
    final Expr right;
  }
  static class Post extends Expr {
    Post(Token operator, Expr left) {
      this.operator = operator;
      this.left = left;
    }

    <R> R accept(Visitor<R> visitor) {
      return visitor.visitPostExpr(this);
    }

    final Token operator;
    final Expr left;
  }
  static class Binary extends Expr {
    Binary(Expr left, Token operator, Expr right) {
      this.left = left;
      this.operator = operator;
      this.right = right;
    }

    <R> R accept(Visitor<R> visitor) {
      return visitor.visitBinaryExpr(this);
    }

    final Expr left;
    final Token operator;
    final Expr right;
  }
  static class Logical extends Expr {
    Logical(Expr left, Token operator, Expr right) {
      this.left = left;
      this.operator = operator;
      this.right = right;
    }

    <R> R accept(Visitor<R> visitor) {
      return visitor.visitLogicalExpr(this);
    }

    final Expr left;
    final Token operator;
    final Expr right;
  }
  static class Lambda extends Expr {
    Lambda(List<Token> arguments, Stmt right) {
      this.arguments = arguments;
      this.right = right;
    }

    <R> R accept(Visitor<R> visitor) {
      return visitor.visitLambdaExpr(this);
    }

    final List<Token> arguments;
    final Stmt right;
  }
  static class Assign extends Expr {
    Assign(Token name, Token operator, Expr value) {
      this.name = name;
      this.operator = operator;
      this.value = value;
    }

    <R> R accept(Visitor<R> visitor) {
      return visitor.visitAssignExpr(this);
    }

    final Token name;
    final Token operator;
    final Expr value;
  }

  abstract <R> R accept(Visitor<R> visitor);
}
