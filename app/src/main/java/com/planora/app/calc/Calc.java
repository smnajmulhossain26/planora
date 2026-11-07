package com.planora.app.calc;

import java.math.BigDecimal;
import java.math.MathContext;

/** Small recursive-descent expression evaluator for the scientific calculator. */
public class Calc {
    private final String s;
    private final boolean deg;
    private int pos;

    private Calc(String s, boolean deg) {
        this.s = s.replace("×", "*").replace("÷", "/").replace("−", "-").replace(" ", "");
        this.deg = deg;
    }

    public static String eval(String expr, boolean deg) throws Exception {
        Calc c = new Calc(expr, deg);
        double v = c.parseExpr();
        if (c.pos < c.s.length()) throw new Exception("Syntax error");
        return format(v);
    }

    public static String format(double v) throws Exception {
        if (Double.isNaN(v) || Double.isInfinite(v)) throw new Exception("Math error");
        if (Math.abs(v) < 1e-12) return "0";
        BigDecimal bd = new BigDecimal(v).round(new MathContext(12)).stripTrailingZeros();
        if (Math.abs(v) >= 1e12 || Math.abs(v) < 1e-9) return bd.toString();
        return bd.toPlainString();
    }

    private char peek() { return pos < s.length() ? s.charAt(pos) : '\0'; }

    private double parseExpr() throws Exception {
        double v = parseTerm();
        while (true) {
            char c = peek();
            if (c == '+') { pos++; v += parseTerm(); }
            else if (c == '-') { pos++; v -= parseTerm(); }
            else return v;
        }
    }

    private double parseTerm() throws Exception {
        double v = parseUnary();
        while (true) {
            char c = peek();
            if (c == '*') { pos++; v *= parseUnary(); }
            else if (c == '/') { pos++; double d = parseUnary(); if (d == 0) throw new Exception("Cannot divide by 0"); v /= d; }
            else if (c == '(' || Character.isLetter(c) || c == 'π' || c == '√') { v *= parseUnary(); } // implicit multiplication
            else return v;
        }
    }

    private double parseUnary() throws Exception {
        if (peek() == '-') { pos++; return -parseUnary(); }
        if (peek() == '+') { pos++; return parseUnary(); }
        return parsePower();
    }

    private double parsePower() throws Exception {
        double base = parsePostfix();
        if (peek() == '^') { pos++; double ex = parseUnary(); return Math.pow(base, ex); }
        return base;
    }

    private double parsePostfix() throws Exception {
        double v = parsePrimary();
        while (true) {
            char c = peek();
            if (c == '!') { pos++; v = fact(v); }
            else if (c == '%') { pos++; v = v / 100.0; }
            else return v;
        }
    }

    private double fact(double v) throws Exception {
        if (v < 0 || v != Math.floor(v) || v > 170) throw new Exception("Math error");
        double r = 1;
        for (int i = 2; i <= (int) v; i++) r *= i;
        return r;
    }

    private double parsePrimary() throws Exception {
        char c = peek();
        if (c == '(') {
            pos++;
            double v = parseExpr();
            if (peek() == ')') pos++;   // allow missing closing bracket
            return v;
        }
        if (c == 'π') { pos++; return Math.PI; }
        if (c == '√') { pos++; double v = parsePostfix(); if (v < 0) throw new Exception("Math error"); return Math.sqrt(v); }
        if (Character.isDigit(c) || c == '.') {
            int st = pos;
            while (Character.isDigit(peek()) || peek() == '.') pos++;
            try { return Double.parseDouble(s.substring(st, pos)); } catch (Exception e) { throw new Exception("Syntax error"); }
        }
        if (Character.isLetter(c)) {
            int st = pos;
            while (Character.isLetter(peek()) && peek() != 'π') pos++;
            String name = s.substring(st, pos);
            if (name.equals("e")) return Math.E;
            double arg = parsePrimaryArg();
            switch (name) {
                case "sin": return Math.sin(deg ? Math.toRadians(arg) : arg);
                case "cos": return Math.cos(deg ? Math.toRadians(arg) : arg);
                case "tan":
                    if (deg && Math.abs(arg % 180) == 90) throw new Exception("Math error");
                    return Math.tan(deg ? Math.toRadians(arg) : arg);
                case "asin": if (arg < -1 || arg > 1) throw new Exception("Math error"); return deg ? Math.toDegrees(Math.asin(arg)) : Math.asin(arg);
                case "acos": if (arg < -1 || arg > 1) throw new Exception("Math error"); return deg ? Math.toDegrees(Math.acos(arg)) : Math.acos(arg);
                case "atan": return deg ? Math.toDegrees(Math.atan(arg)) : Math.atan(arg);
                case "ln": if (arg <= 0) throw new Exception("Math error"); return Math.log(arg);
                case "log": if (arg <= 0) throw new Exception("Math error"); return Math.log10(arg);
                default: throw new Exception("Syntax error");
            }
        }
        throw new Exception("Syntax error");
    }

    private double parsePrimaryArg() throws Exception {
        if (peek() == '(') return parsePrimary();
        return parsePostfix();
    }
}
