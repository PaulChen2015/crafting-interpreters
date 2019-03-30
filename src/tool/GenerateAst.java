package tool;

import java.io.IOException;
import java.io.PrintWriter;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.List;

/**
 * @author chenpeng
 * @since 2018-12-28 10:49
 */
public class GenerateAst {

    private static final String TAB = "    ";

    public static void main(String[] args) throws IOException {
        if (args.length != 1) {
            System.err.println("Usage: generate_ast <output directory>");
            System.exit(1);
        }

        String outputDir = args[0];

        // expression
        defExpr(outputDir);

        //statement
        defineStmt(outputDir);
    }

    private static void defineStmt(String outputDir) throws IOException {
        defineAst(outputDir, "Stmt", Arrays.asList(
                "Block      : List<Stmt> statements",
                "Class      : Token name, List<Stmt.Function> methods, List<Stmt.Function> staticMethods",
                "Expression : Expr expression",
                "Function   : Token name, List<Token> params, List<Stmt> body",
                "If         : Expr condition, Stmt thenBranch, Stmt elseBranch",
                "While      : Expr condition, Stmt body",
                "Print      : Expr expression",
                "Return     : Token keyword, Expr value",
                "Var        : Token name, Expr initializer",
                "MultiVar   : List<Stmt.Var> vars"
        ));
    }

    private static void defExpr(String outputDir) throws IOException {
        defineAst(outputDir, "Expr", Arrays.asList(
                "Assign   : Token name, Expr value",
                "Ternary  : Expr condition, Expr thenBranch, Expr elseBranch",
                "Binary   : Expr left, Token operator, Expr right",
                "Call     : Expr callee, Token paren, List<Expr> arguments",
                "Get      : Expr object, Token name",
                "Grouping : Expr expression",
                "Literal  : Object value",
                "Logical  : Expr left, Token operator, Expr right",
                "Set      : Expr object, Token name, Expr value",
                "This     : Token keyword",
                "Unary    : Token operator, Expr right",
                "Variable : Token name",
                "Lambda   : Token name, List<Token> params, List<Stmt> body"
        ));
    }

    private static void defineAst(String outputDir, String baseName, List<String> types) throws IOException {

        String path = outputDir + "/" + baseName + ".java";
        try (PrintWriter writer = new PrintWriter(path, StandardCharsets.UTF_8)) {
            writerLine(writer, 0, "package lox;");
            writer.println();
            writerLine(writer, 0, "import java.util.List;");
            writer.println();
            writerLine(writer, 0, "abstract class ", baseName, " {");

            defineVisitor(writer, baseName, types);

            for (String type : types) {
                String className = type.split(":")[0].trim();
                String fields = type.split(":")[1].trim();
                defineType(writer, baseName, className, fields);
            }

            writer.println();
            writerLine(writer, 1, "abstract <R> R accept(Visitor<R> visitor);");

            writerLine(writer, 0, "}");
        }
    }

    private static void defineVisitor(PrintWriter writer, String baseName, List<String> types) {

        writerLine(writer, 1, "interface Visitor<R> {");

        for (String type : types) {
            String typeName = type.split(":")[0].trim();
            writerLine(writer, 2, "R visit", typeName, baseName, "(",
            typeName, " ", baseName.toLowerCase(), ");");
        }

        writerLine(writer, 1, "}");
        writer.println();
    }

    private static void defineType(PrintWriter writer, String baseName, String className, String fieldList) {
        writerLine(writer, 1, "static class ", className, " extends ", baseName, " {");
        writerLine(writer, 2, className, "(", fieldList, ") {");

        String[] fields = fieldList.split(", ");

        for (String field : fields) {
            String name = field.split(" ")[1];
            writerLine(writer, 3, "this.", name, " = ", name, ";");
        }
        writerLine(writer, 2, "}");

        writer.println();
        writerLine(writer, 2, "@Override");
        writerLine(writer, 2, "<R> R accept(Visitor<R> visitor) {");
        writerLine(writer, 3, "return visitor.visit", className, baseName, "(this);");
        writerLine(writer, 2, "}");

        writer.println();
        for (String field : fields) {
            writerLine(writer, 2, "final ", field, ";");
        }

        writerLine(writer, 1, "}");
        writer.println();
    }

    private static void writerLine(PrintWriter writer, int tabs, String... terms) {
        StringBuilder ret = new StringBuilder();
        for (int i = 0; i < tabs; i++) {
            ret.append(TAB);
        }

        for (String term : terms) {
            ret.append(term);
        }
        writer.println(ret.toString());
    }

}
