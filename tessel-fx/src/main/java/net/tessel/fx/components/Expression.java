package net.tessel.fx.components;

/**
 * A tiny arithmetic evaluator for {@link NumberBox}: numbers, {@code + - * / % ^}, unary signs and parentheses.
 * Both {@code 1.5} and {@code 1,5} are understood as decimals.
 */
final class Expression {

    private final String text;
    private int pos;

    private Expression(String text) {
        this.text = text;
    }

    /** @throws NumberFormatException when the text is not a valid expression */
    static double evaluate(String text) {
        Expression e = new Expression(text.trim());
        double v = e.sum();
        e.skip();
        if (e.pos != e.text.length()) {
            throw new NumberFormatException(text);
        }
        return v;
    }

    /** Makes a number string parseable: accepts a comma as the decimal separator and drops grouping. */
    static String normalize(String number) {
        String s = number.trim().replace(" ", "").replace(" ", "");
        int comma = s.lastIndexOf(',');
        int dot = s.lastIndexOf('.');
        if (comma >= 0 && dot >= 0) {
            // the later one is the decimal separator, the other one groups thousands
            return comma > dot ? s.replace(".", "").replace(',', '.') : s.replace(",", "");
        }
        if (comma >= 0) {
            return s.indexOf(',') == comma ? s.replace(',', '.') : s.replace(",", "");
        }
        return s;
    }

    private void skip() {
        while (pos < text.length() && Character.isWhitespace(text.charAt(pos))) {
            pos++;
        }
    }

    private boolean accept(char c) {
        skip();
        if (pos < text.length() && text.charAt(pos) == c) {
            pos++;
            return true;
        }
        return false;
    }

    private double sum() {
        double v = product();
        while (true) {
            if (accept('+')) {
                v += product();
            } else if (accept('-')) {
                v -= product();
            } else {
                return v;
            }
        }
    }

    private double product() {
        double v = power();
        while (true) {
            if (accept('*') || accept('×')) {
                v *= power();
            } else if (accept('/') || accept('÷')) {
                v /= power();
            } else if (accept('%')) {
                v %= power();
            } else {
                return v;
            }
        }
    }

    private double power() {
        double base = unary();
        if (accept('^')) {
            return Math.pow(base, power());
        }
        return base;
    }

    private double unary() {
        if (accept('-')) {
            return -unary();
        }
        if (accept('+')) {
            return unary();
        }
        return primary();
    }

    private double primary() {
        skip();
        if (accept('(')) {
            double v = sum();
            if (!accept(')')) {
                throw new NumberFormatException(text);
            }
            return v;
        }
        int start = pos;
        while (pos < text.length() && (Character.isDigit(text.charAt(pos)) || text.charAt(pos) == '.'
                || text.charAt(pos) == ',')) {
            pos++;
        }
        if (start == pos) {
            throw new NumberFormatException(text);
        }
        return Double.parseDouble(normalize(text.substring(start, pos)));
    }
}
