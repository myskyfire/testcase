package com.example.demo.agent;

import java.util.ArrayList;
import java.util.List;

public class ExpressionEvaluator {
    public static boolean eval(String expr) {
        if (expr == null) return false;
        String s = expr.trim();
        if (s.isEmpty()) return true;
        List<String> partsOr = splitTopLevel(s, "||");
        for (String orp : partsOr) {
            List<String> partsAnd = splitTopLevel(orp, "&&");
            boolean andOk = true;
            for (String ap : partsAnd) {
                if (!evalAtom(ap.trim())) {
                    andOk = false;
                    break;
                }
            }
            if (andOk) return true;
        }
        return false;
    }

    private static boolean evalAtom(String s) {
        if (s.isEmpty()) return true;
        String t = s.trim();
        if (t.startsWith("!")) {
            return !evalAtom(t.substring(1).trim());
        }
        if (t.equalsIgnoreCase("true") || t.equalsIgnoreCase("yes") || t.equals("1")) return true;
        if (t.equalsIgnoreCase("false") || t.equalsIgnoreCase("no") || t.equals("0")) return false;
        if (t.startsWith("contains(") && t.endsWith(")")) {
            List<String> args = splitArgs(t.substring(9, t.length() - 1));
            if (args.size() < 2) return false;
            String a = unquote(args.get(0).trim());
            String b = unquote(args.get(1).trim());
            return a.contains(b);
        }
        if (t.startsWith("startsWith(") && t.endsWith(")")) {
            List<String> args = splitArgs(t.substring(11, t.length() - 1));
            if (args.size() < 2) return false;
            String a = unquote(args.get(0).trim());
            String b = unquote(args.get(1).trim());
            return a.startsWith(b);
        }
        if (t.startsWith("endsWith(") && t.endsWith(")")) {
            List<String> args = splitArgs(t.substring(9, t.length() - 1));
            if (args.size() < 2) return false;
            String a = unquote(args.get(0).trim());
            String b = unquote(args.get(1).trim());
            return a.endsWith(b);
        }
        if (t.startsWith("in(") && t.endsWith(")")) {
            List<String> args = splitArgs(t.substring(3, t.length() - 1));
            if (args.size() < 2) return false;
            String x = unquote(args.get(0).trim());
            for (int i = 1; i < args.size(); i++) {
                if (x.equals(unquote(args.get(i).trim()))) return true;
            }
            return false;
        }
        int opIdx;
        String op = null;
        opIdx = findOp(t, "==");
        if (opIdx >= 0) op = "==";
        if (op == null) {
            opIdx = findOp(t, "!=");
            if (opIdx >= 0) op = "!=";
        }
        if (op == null) {
            opIdx = findOp(t, ">=");
            if (opIdx >= 0) op = ">=";
        }
        if (op == null) {
            opIdx = findOp(t, "<=");
            if (opIdx >= 0) op = "<=";
        }
        if (op == null) {
            opIdx = findOp(t, ">");
            if (opIdx >= 0) op = ">";
        }
        if (op == null) {
            opIdx = findOp(t, "<");
            if (opIdx >= 0) op = "<";
        }
        if (op != null) {
            String left = t.substring(0, opIdx).trim();
            String right = t.substring(opIdx + op.length()).trim();
            String l = unquote(left);
            String r = unquote(right);
            Double ln = parseNum(l);
            Double rn = parseNum(r);
            if (ln != null && rn != null) {
                int cmp = Double.compare(ln, rn);
                if (op.equals("==")) return cmp == 0;
                if (op.equals("!=")) return cmp != 0;
                if (op.equals(">")) return cmp > 0;
                if (op.equals("<")) return cmp < 0;
                if (op.equals(">=")) return cmp >= 0;
                if (op.equals("<=")) return cmp <= 0;
            } else {
                int cmp = l.compareTo(r);
                if (op.equals("==")) return cmp == 0;
                if (op.equals("!=")) return cmp != 0;
                if (op.equals(">")) return cmp > 0;
                if (op.equals("<")) return cmp < 0;
                if (op.equals(">=")) return cmp >= 0;
                if (op.equals("<=")) return cmp <= 0;
            }
            return false;
        }
        return truthy(unquote(t));
    }

    private static List<String> splitTopLevel(String s, String sep) {
        List<String> out = new ArrayList<>();
        StringBuilder cur = new StringBuilder();
        boolean inS = false, inD = false;
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            if (c == '\'' && !inD) inS = !inS;
            else if (c == '\"' && !inS) inD = !inD;
            if (!inS && !inD && s.startsWith(sep, i)) {
                out.add(cur.toString());
                cur.setLength(0);
                i += sep.length() - 1;
            } else {
                cur.append(c);
            }
        }
        out.add(cur.toString());
        return out;
    }

    private static List<String> splitArgs(String s) {
        List<String> out = new ArrayList<>();
        StringBuilder cur = new StringBuilder();
        boolean inS = false, inD = false;
        int depth = 0;
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            if (c == '(' && !inS && !inD) depth++;
            else if (c == ')' && !inS && !inD && depth > 0) depth--;
            if (c == '\'' && !inD) inS = !inS;
            else if (c == '"' && !inS) inD = !inD;
            if (!inS && !inD && depth == 0 && c == ',') {
                out.add(cur.toString());
                cur.setLength(0);
            } else {
                cur.append(c);
            }
        }
        if (cur.length() > 0) out.add(cur.toString());
        return out;
    }

    private static int findOp(String s, String op) {
        boolean inS = false, inD = false;
        for (int i = 0; i <= s.length() - op.length(); i++) {
            char c = s.charAt(i);
            if (c == '\'' && !inD) inS = !inS;
            else if (c == '"' && !inS) inD = !inD;
            if (!inS && !inD && s.startsWith(op, i)) return i;
        }
        return -1;
    }

    private static String unquote(String s) {
        String t = s.trim();
        if ((t.startsWith("\"") && t.endsWith("\"")) || (t.startsWith("'") && t.endsWith("'"))) {
            return t.substring(1, t.length() - 1);
        }
        return t;
    }

    private static Double parseNum(String s) {
        try {
            return Double.parseDouble(s);
        } catch (Exception e) {
            return null;
        }
    }

    private static boolean truthy(String s) {
        if (s == null) return false;
        String v = s.trim().toLowerCase();
        if (v.isEmpty()) return true;
        if (v.equals("true") || v.equals("1") || v.equals("yes") || v.equals("y")) return true;
        if (v.equals("false") || v.equals("0") || v.equals("no") || v.equals("n")) return false;
        return !v.isEmpty();
    }
}
