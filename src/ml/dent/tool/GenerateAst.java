package ml.dent.tool;

import java.io.IOException;
import java.io.PrintWriter;
import java.util.Arrays;
import java.util.List;

public class GenerateAst {
	public static void main(String[] args) throws IOException {

		String outputDir = "./src/ml/dent/trab";
		defineAst(outputDir, "Expr",
				Arrays.asList(
						"Literal    : Object value",
						"TrabString : List<Expr> values",
						"Variable   : Token name",
						"TrabList   : List<Expr> values, Token start",
						"Grouping   : Expr expression",
						"Call       : Token operator, Expr left, List<Expr> arguments",
						"Get        : Expr left, Token name",
						"Index      : Expr operand, Expr start, Expr end, Expr step, Token bracket",
						"Unary      : Token operator, Expr right",
						"Post       : Token operator, Expr left",
						"Binary     : Expr left, Token operator, Expr right",
						"Logical    : Expr left, Token operator, Expr right",
						"Lambda     : List<Token> arguments, Stmt right",
						"Assign     : Token name, Token operator, Expr value"
						));
		defineAst(outputDir, "Stmt",
				Arrays.asList(
						 "Block      : List<Stmt> statements",
						 "Class      : Token name, List<Stmt.Function> methods",
						 "Expression : Expr expression",
						 "PrintableExpression : Expr expression",
						 "If         : Token ifToken, Expr condition, Stmt thenBranch, Stmt elseBranch",
				      	 "Return     : Token keyword, Expr value",
						 "Var        : Token name, Expr initializer",
						 "While      : Token whileToken, Expr condition, Stmt body",
						 "Function   : Token name, List<Token> arguments, Stmt body"
						));
	}

	private static void defineAst(String outputDir, String baseName, List<String> types) throws IOException {
		String path = outputDir + "/" + baseName + ".java";
		PrintWriter writer = new PrintWriter(path, "UTF-8");

		writer.println("package ml.dent.trab;");
		writer.println();
		writer.println("import java.util.List;");
		writer.println();
		writer.println("abstract class " + baseName + " {");
		defineVisitor(writer, baseName, types);
		for (String type : types) {
			String className = type.split(":")[0].trim();
			String fields = type.split(":")[1].trim();
			defineType(writer, baseName, className, fields);
		}
		writer.println();
		writer.println("  abstract <R> R accept(Visitor<R> visitor);");
		writer.println("}");
		writer.close();
	}

	private static void defineVisitor(PrintWriter writer, String baseName, List<String> types) {
		writer.println("  interface Visitor<R> {");

		for (String type : types) {
			String typeName = type.split(":")[0].trim();
			writer.println("    R visit" + typeName + baseName + "(" + typeName + " " + baseName.toLowerCase() + ");");
		}

		writer.println("  }");
	}

	private static void defineType(PrintWriter writer, String baseName, String className, String fieldList) {
		writer.println("  static class " + className + " extends " + baseName + " {");

		// Constructor.
		writer.println("    " + className + "(" + fieldList + ") {");

		// Store parameters in fields.
		String[] fields = fieldList.split(", ");
		for (String field : fields) {
			String name = field.split(" ")[1];
			writer.println("      this." + name + " = " + name + ";");
		}

		writer.println("    }");
		writer.println();
		writer.println("    <R> R accept(Visitor<R> visitor) {");
		writer.println("      return visitor.visit" + className + baseName + "(this);");
		writer.println("    }");
		// Fields.
		writer.println();
		for (String field : fields) {
			writer.println("    final " + field + ";");
		}

		writer.println("  }");
	}
}