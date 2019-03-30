package lox;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * @author chenpeng
 * @since 2019-01-09 11:16
 */
class LoxClass extends LoxInstance implements LoxCallable {
    final String name;
    private final Map<String, LoxFunction> methods;
    private static Map<String, LoxFunction> staticMethod = new HashMap<>();

    LoxClass(String name, Map<String, LoxFunction> methods) {
        super();
        this.name = name;
        this.methods = methods;
    }

    LoxFunction findMethod(LoxInstance instance, String name) {
        if (methods.containsKey(name)) {
            return methods.get(name).bind(instance);
        }

        return null;
    }

    static void add(String name, LoxFunction method) {
        staticMethod.put(name, method);
    }

    @Override
    public String toString() {
        return name;
    }

    @Override
    public int arity() {
        LoxFunction initializer = methods.get("init");
        if (initializer == null) return 0;

        return initializer.arity();
    }

    @Override
    public Object call(Interpreter interpreter, List<Object> arguments) {
        LoxInstance instance = new LoxInstance(this);
        LoxFunction initializer = methods.get("init");
        if (initializer != null) {
            initializer.bind(instance).call(interpreter, arguments);
        }
        return instance;
    }

    @Override
    Object get(Token name) {
        return staticMethod.get(name.lexeme);
    }
}
