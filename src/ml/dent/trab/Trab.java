package ml.dent.trab;

import java.io.*;
import java.util.List;

public class Trab {
    private enum Mode {
        SCAN, PARSE, RESOLVE, RUN
    }

    private static boolean hadError = false;
    private static boolean hadRuntimeError = false;
    private static boolean isRepl = false;
    private static boolean verbose = false;
    private static Interpreter interpreter;
    private static Mode mode;
    private static String[] stdLib = {"Math.trab", "List.trab"};

    public static void main(String[] args) throws IOException {
        interpreter = new Interpreter();
        for (String s : stdLib) loadFile(s);
        if (hadError || hadRuntimeError) {
            System.out.println("Standard Library Error");
            System.exit(255);
        }
        if (args.length > 0) runFile(args[0]);
        else runRepl();
    }

    private static void runRepl() throws IOException {
        InputStreamReader input = new InputStreamReader(System.in);
        BufferedReader reader = new BufferedReader(input);
        isRepl = true;
        for (; ; ) {
            System.out.print("> ");
            run(reader.readLine(), isRepl, "");
            hadError = false;
            hadRuntimeError = false;
        }
    }

    private static void loadFile(String fileName) throws IOException {

        BufferedReader reader = new BufferedReader(new FileReader(new File(fileName)));
        StringBuffer file = new StringBuffer();
        while (reader.ready()) file.append(reader.readLine() + "\n");
        run(file.toString(), isRepl, "");
    }


    private static void runFile(String fileName) throws IOException {
        BufferedReader reader = new BufferedReader(new FileReader(new File(fileName)));
        StringBuffer file = new StringBuffer();
        while (reader.ready()) file.append(reader.readLine() + "\n");
        BufferedReader standardIn = new BufferedReader(new InputStreamReader(System.in));
        StringBuffer stin = new StringBuffer();
        while (standardIn.ready()) stin.append(standardIn.readLine() + "\n");
        run(file.toString(), isRepl, stin.toString());
    }

    private static void run(String program, boolean isRepl, String stin) {
        List<Token> tokens = Scanner.scan(program);
        if (hadError) {
            System.out.println("Scanner Error");
            return;
        }

        List<Stmt> prog = new Parser(tokens).parse();
        if (hadError) {
            System.out.println("Parse Error");
            return;
        }
        interpreter.run(prog, isRepl, stin);
    }

    public static void error(int line) {
        System.out.println("Error at line " + line);
        hadError = true;
    }

    public static void error(Token token) {
        error("Error at token " + token.lexeme, token.line);
    }

    public static void error(Token token, String message) {
        error(message, token.line);
    }

    public static void error(String message, int line) {
        if (isRepl) System.out.println(message);
        else System.out.println(message + " at line " + line);
        hadError = true;
    }

    public static void runtimeError(RuntimeError error) {
        hadRuntimeError = true;
        error(error.token, error.getMessage());
    }

}
