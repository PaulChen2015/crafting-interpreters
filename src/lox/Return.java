package lox;

/**
 * @author chenpeng
 * @since 2019-01-07 11:06
 */
public class Return extends RuntimeException {
    final Object value;

    Return(Object value) {
        super(null, null, false, false);
        this.value = value;
    }
}
