package com.example.demo.agent;

import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.List;

public class TemplateEngine {
    private static final Pattern P = Pattern.compile("\\{\\{\\s*([\\u4e00-\\u9fa5\\w\\.-]+)\\s*\\}\\}");

    public String render(String template, Map<String, Object> ctx) {
        if (template == null) return null;
        Matcher m = P.matcher(template);
        StringBuffer sb = new StringBuffer();
        while (m.find()) {
            String key = m.group(1);
            Object v = null;
            if (ctx != null) {
                v = ctx.get(key);
                if (v == null && (key.contains(".") || key.contains("["))) {
                    v = resolvePath(ctx, key);
                }
            }
            if (v == null) v = System.getenv(key);
            String rep = v == null ? "" : String.valueOf(v);
            rep = rep.replace("\\", "\\\\").replace("$", "\\$");
            m.appendReplacement(sb, rep);
        }
        m.appendTail(sb);
        return sb.toString();
    }

    private Object resolvePath(Map<String, Object> ctx, String path) {
        if (path == null || path.isEmpty()) return null;
        String p = path;
        if (p.startsWith("$.")) p = p.substring(2);
        if (p.startsWith(".")) p = p.substring(1);
        String[] parts = p.split("\\.");
        Object cur = ctx;
        for (String part : parts) {
            if (cur == null) return null;
            if (part.endsWith("]") && part.contains("[")) {
                int i = part.indexOf('[');
                String name = part.substring(0, i);
                String idxStr = part.substring(i + 1, part.length() - 1);
                if (!name.isEmpty()) {
                    if (cur instanceof Map) {
                        cur = ((Map<?, ?>) cur).get(name);
                    } else {
                        return null;
                    }
                }
                int idx = Integer.parseInt(idxStr);
                if (cur instanceof List) {
                    List<?> list = (List<?>) cur;
                    if (idx < 0 || idx >= list.size()) return null;
                    cur = list.get(idx);
                } else {
                    return null;
                }
            } else {
                if (cur instanceof Map) {
                    cur = ((Map<?, ?>) cur).get(part);
                } else {
                    return null;
                }
            }
        }
        return cur;
    }
}
