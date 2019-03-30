package lox;

/**
 * @author chenpeng
 * @since 2018-12-27 14:07
 */
public enum TokenType {

    LEFT_PAREN, RIGHT_PAREN, LEFT_BRACE, RIGHT_BRACE,
    COMMA, DOT, MINUS, PLUS, SEMICOLON, SLASH, STAR,

    QUESTION, COLON,

    MODULO,

    PLUS_EQUAL, MINUS_EQUAL, SLASH_EQUAL, STAR_EQUAL,
    INCREMENT, DECREMENT, LEFT_SHIFT, RIGHT_SHIFT,

    BREAK, CONTINUE,

    LAMBDA,

    BANG, BANG_EQUAL,
    EQUAL, EQUAL_EQUAL,
    GREATER, GREATER_EQUAL,
    LESS, LESS_EQUAL,

    IDENTIFIER, STRING, NUMBER,

    AND, CLASS, ELSE, FALSE, FUN, FOR, IF, NIL, OR,
    PRINT, RETURN, SUPER, THIS, TRUE, VAR, WHILE,

    EOF
}