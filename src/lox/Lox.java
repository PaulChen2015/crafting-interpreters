package lox;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.List;
import java.util.Set;
import static lox.TokenType.*;


/**
 * @author chenpeng
 * @since 2018-12-27 13:33
 */
public class Lox {

    private static final Interpreter interpreter = new Interpreter();
    private static boolean hadError = false;
    private static boolean hadRuntimeError = false;

    private static final Set<TokenType> types = Set.of(VAR, IF, WHILE, LEFT_BRACE,
            PRINT, FUN, FOR, CLASS, RETURN, THIS);

    public static void main(String[] args) throws IOException {
        if (args.length > 1) {
            System.out.println("Usage: jlox [script]");
            System.exit(64);
        } else if (args.length == 1) {
            runFile(args[0]);
        } else {
            runPrompt();
        }
    }

    private static void runPrompt() {
        InputStreamReader input = new InputStreamReader(System.in);
        BufferedReader reader = new BufferedReader(input);

        for (;;) {
            System.out.print("> ");
            try {
                runPrompt(reader.readLine());
                hadError = false;
            } catch (Exception e) {
                hadError = false;
                e.printStackTrace();
            }
        }
    }

    private static void runPrompt(String source) {
        Scanner scanner = new Scanner(source);
        List<Token> tokens = scanner.scanTokens();
        if (treatAsStmt(tokens)) {
            run(tokens);
        } else {
            runExpression(tokens);
        }

    }

    private static boolean treatAsStmt(List<Token> tokens) {
        TokenType first = tokens.get(0).type;
        return ((IDENTIFIER == first && tokens.get(1).type == EQUAL)
                || types.contains(first));
    }

    private static void runExpression(List<Token> tokens) {
        Expr expression = new Parser(tokens).parseExpr();
        if (hadError) return;

        interpreter.interpreterExpr(expression);
    }

    private static void runFile(String path) throws IOException {
        byte[] bytes = Files.readAllBytes(Paths.get(path));
        run(new String(bytes, Charset.defaultCharset()));

        if (hadError) System.exit(65);

        if (hadRuntimeError) System.exit(70);
    }

    private static void run(String source) {
        Scanner scanner = new Scanner(source);
        List<Token> tokens = scanner.scanTokens();
        run(tokens);

    }

    private static void run(List<Token> tokens) {
        List<Stmt> statements = new Parser(tokens).parse();

        if (hadError) return;

        Resolver resolver = new Resolver(interpreter);
        resolver.resolve(statements);

        if (hadError) return; // stop if had resolution error

        interpreter.interpreter(statements);
    }

    static void error(int line, String message) {
        report(line, "", message);
    }

    private static void report(int line, String where, String message) {
        System.err.println("[line " + line + "] Error" + where + ": " + message);
        hadError = true;
    }

    static void error(Token token, String message) {
        if (token.type == TokenType.EOF) {
            report(token.line, " at end", message);
        } else {
            report(token.line, " at '" + token.lexeme + "'", message);
        }
    }

    static void runtimeError(RuntimeError e) {
        System.err.println(e.getMessage() + "\n[line " + e.token.line + "]");
        hadRuntimeError = true;
    }


}
