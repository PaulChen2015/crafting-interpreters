package lox;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Stack;

/**
 * @author chenpeng
 * @since 2019-01-08 09:14
 */
public class Resolver implements Expr.Visitor<Void>, Stmt.Visitor<Void> {

    private final Interpreter interpreter;
    private final Stack<Map<String, Boolean>> scopes = new Stack<>();
    private FunctionType currentFunction = FunctionType.NONE;

    private Stack<Map<Token, Boolean>> variables = new Stack<>();

    Resolver(Interpreter interpreter) {
        this.interpreter = interpreter;
    }


    private enum FunctionType {
        NONE,
        FUNCTION,
        INITIALIZER,
        METHOD,
        STATIC_METHOD
    }

    private enum ClassType {
        NONE,
        CLASS
    }

    private ClassType currentClass = ClassType.NONE;

    @Override
    public Void visitAssignExpr(Expr.Assign expr) {
        resolve(expr.value);
        resolveLocal(expr, expr.name);
        return null;
    }

    @Override
    public Void visitTernaryExpr(Expr.Ternary expr) {
        resolve(expr.condition);
        resolve(expr.thenBranch);
        resolve(expr.elseBranch);
        return null;
    }

    @Override
    public Void visitBinaryExpr(Expr.Binary expr) {
        resolve(expr.left);
        resolve(expr.right);
        return null;
    }

    @Override
    public Void visitCallExpr(Expr.Call expr) {
        resolve(expr.callee);
        for (Expr argument : expr.arguments) {
            resolve(argument);
        }
        return null;
    }

    @Override
    public Void visitGetExpr(Expr.Get expr) {
        resolve(expr.object);
        return null;
    }

    @Override
    public Void visitGroupingExpr(Expr.Grouping expr) {
        resolve(expr.expression);
        return null;
    }

    @Override
    public Void visitLiteralExpr(Expr.Literal expr) {
        return null;
    }

    @Override
    public Void visitLogicalExpr(Expr.Logical expr) {
        resolve(expr.left);
        resolve(expr.right);
        return null;
    }

    @Override
    public Void visitSetExpr(Expr.Set expr) {
        resolve(expr.value);
        resolve(expr.object);
        return null;
    }

    @Override
    public Void visitThisExpr(Expr.This expr) {
        if (currentClass != ClassType.CLASS) {
            Lox.error(expr.keyword, "Cannot use 'this' outside of class.");
            return null;
        }

        if (currentFunction == FunctionType.STATIC_METHOD) {
            Lox.error(expr.keyword, "Cannot use 'this' in a static context.");
        }

        resolveLocal(expr, expr.keyword);
        return null;
    }

    @Override
    public Void visitUnaryExpr(Expr.Unary expr) {
        resolve(expr.right);
        return null;
    }

    @Override
    public Void visitVariableExpr(Expr.Variable expr) {
        if (!scopes.empty() && scopes.peek().get(expr.name.lexeme) == Boolean.FALSE) {
            Lox.error(expr.name, "Cannot read local variable in its own initializer.");
        }

        resolveLocal(expr, expr.name);
        useVar(expr.name);
        return null;
    }

    private void useVar(Token name) {
        if (variables.empty()) return;
        variables.peek().put(name, true);
    }


    private void resolveLocal(Expr expr, Token name) {
        for (int i = scopes.size() - 1; i >= 0; i--) {
            if (scopes.get(i).containsKey(name.lexeme)) {
                interpreter.resolve(expr, scopes.size() - 1 - i);
                return;
            }
        }
    }

    @Override
    public Void visitLambdaExpr(Expr.Lambda expr) {
        visitFunctionStmt(new Stmt.Function(expr.name, expr.params, expr.body));
        return null;
    }

    @Override
    public Void visitBlockStmt(Stmt.Block stmt) {
        beginScope();
        resolve(stmt.statements);
        endScope();
        return null;
    }

    @Override
    public Void visitClassStmt(Stmt.Class stmt) {
        ClassType enclosingClass = currentClass;
        currentClass = ClassType.CLASS;
        declare(stmt.name);
        define(stmt.name);

        beginScope();
        scopes.peek().put("this", true);
        for (Stmt.Function method : stmt.methods) {
            FunctionType declaration = FunctionType.METHOD;
            if (method.name.lexeme.equals("init")) {
                declaration = FunctionType.INITIALIZER;
            }
            resolveFunction(method, declaration);
        }
        for (Stmt.Function method : stmt.staticMethods) {
            FunctionType declaration = FunctionType.STATIC_METHOD;
            resolveFunction(method, declaration);
        }
        endScope();

        currentClass = enclosingClass;
        return null;
    }

    private void checkVariable(Map<Token, Boolean> vars) {
        for (Map.Entry<Token, Boolean> entry : vars.entrySet()) {
            if (!entry.getValue()) {
                warn(entry.getKey(), "local variable never used.");
            }
        }
    }

    private void warn(Token token, String message) {
        System.out.printf("Line %d: %s, %s\n", token.line, token.lexeme, message);
    }

    void resolve(List<Stmt> statements) {
        for (Stmt stmt : statements) {
            resolve(stmt);
        }
    }

    private void resolve(Stmt stmt) {
        stmt.accept(this);
    }

    private void beginScope() {
        scopes.push(new HashMap<>());
        variables.push(new HashMap<>());
    }

    private void endScope() {
        scopes.pop();
        Map<Token, Boolean> vars = variables.pop();
        checkVariable(vars);
    }

    @Override
    public Void visitExpressionStmt(Stmt.Expression stmt) {
        resolve(stmt.expression);
        return null;
    }

    @Override
    public Void visitFunctionStmt(Stmt.Function stmt) {
        declare(stmt.name);
        define(stmt.name);
        resolveFunction(stmt, FunctionType.FUNCTION);
        return null;
    }

    private void resolveFunction(Stmt.Function function, FunctionType type) {

        FunctionType enclosingFunction = currentFunction;
        currentFunction = type;

        beginScope();
        for (Token param : function.params) {
            declare(param);
            define(param);
        }
        resolve(function.body);
        endScope();
        currentFunction = enclosingFunction;
    }

    @Override
    public Void visitIfStmt(Stmt.If stmt) {
        resolve(stmt.condition);
        resolve(stmt.thenBranch);

        if (stmt.elseBranch != null) resolve(stmt.elseBranch);
        return null;
    }

    @Override
    public Void visitWhileStmt(Stmt.While stmt) {
        resolve(stmt.condition);
        resolve(stmt.body);
        return null;
    }

    @Override
    public Void visitPrintStmt(Stmt.Print stmt) {
        return null;
    }

    @Override
    public Void visitReturnStmt(Stmt.Return stmt) {

        if (currentFunction == FunctionType.NONE) {
            Lox.error(stmt.keyword, "Cannot return from top-level code.");
        }

        if (stmt.value != null) {
            if (currentFunction == FunctionType.INITIALIZER) {
                Lox.error(stmt.keyword, "Cannot return a value from an initializer.");
            }
            resolve(stmt.value);
        }
        return null;
    }

    @Override
    public Void visitVarStmt(Stmt.Var stmt) {
        declareVar(stmt.name);
        declare(stmt.name);
        if (stmt.initializer != null) {
            resolve(stmt.initializer);
        }
        define(stmt.name);
        return null;
    }

    private void declareVar(Token name) {
        if (variables.empty()) return;

        variables.peek().put(name, false);
    }

    private void resolve(Expr expr) {
        expr.accept(this);
    }

    private void declare(Token name) {
        if (scopes.empty()) return;

        Map<String, Boolean> scope = scopes.peek();

        if (scope.containsKey(name.lexeme)) {
            Lox.error(name, "Variable with this name already declared in this scope.");
        }

        scope.put(name.lexeme, false);
    }

    private void define(Token name) {
        if (scopes.empty()) return;
        scopes.peek().put(name.lexeme, true);
    }

    @Override
    public Void visitMultiVarStmt(Stmt.MultiVar stmt) {
        for (Stmt.Var var : stmt.vars) {
            visitVarStmt(var);
        }
        return null;
    }
}
