package lox;

/**
 * @author chenpeng
 * @since 2018-12-29 13:47
 */
class RuntimeError extends RuntimeException {

    final Token token;

    RuntimeError(Token token, String message) {
        super(message);
        this.token = token;
    }
}
