package lox;

import java.util.List;

/**
 * @author chenpeng
 * @since 2019-01-04 16:25
 */
public interface LoxCallable {
    int arity();
    Object call(Interpreter interpreter, List<Object> arguments);
}
