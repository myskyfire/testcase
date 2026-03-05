package com.example.demo.agent;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import org.springframework.http.client.SimpleClientHttpRequestFactory;

@Service
public class AgentExecutor {
    private final RestTemplate restTemplate;
    private final ObjectMapper mapper = new ObjectMapper();
    private final TemplateEngine engine = new TemplateEngine();
    private final Map<String, CBState> circuit = new HashMap<>();

    private static class CBState {
        int failures;
        long openUntil;
        int halfOpenCalls;
    }

    public AgentExecutor(RestTemplate restTemplate) {
        this.restTemplate = restTemplate;
    }

    public Map<String, Object> executeFromUrl(String url, Map<String, Object> input) throws Exception {
        String specStr = restTemplate.getForObject(url, String.class);
        AgentSpec spec = mapper.readValue(specStr.getBytes(StandardCharsets.UTF_8), AgentSpec.class);
        RestTemplate rt = restTemplate;
        if (spec.getGlobalTimeoutMs() != null && spec.getGlobalTimeoutMs() > 0) {
            SimpleClientHttpRequestFactory f = new SimpleClientHttpRequestFactory();
            f.setConnectTimeout(spec.getGlobalTimeoutMs());
            f.setReadTimeout(spec.getGlobalTimeoutMs());
            RestTemplate tmp = new RestTemplate(restTemplate.getMessageConverters());
            tmp.setRequestFactory(f);
            rt = tmp;
        }
        Map<String, Object> ctx = new LinkedHashMap<>();
        if (spec.getDefaults() != null) ctx.putAll(spec.getDefaults());
        if (input != null) ctx.putAll(input);
        List<Map<String, Object>> stepsOut = new ArrayList<>();
        if (spec.getSteps() != null) {
            for (Step s : spec.getSteps()) {
                Map<String, Object> stepRes = new LinkedHashMap<>();
                stepRes.put("id", s.getId());
                try {
                    if (!shouldRun(s, ctx)) {
                        stepRes.put("skipped", true);
                        stepsOut.add(stepRes);
                        continue;
                    }
                    Object res = execStep(s, ctx, spec, rt);
                    stepRes.put("ok", true);
                    stepRes.put("result", res);
                } catch (Exception e) {
                    stepRes.put("ok", false);
                    stepRes.put("error", e.getMessage());
                    stepsOut.add(stepRes);
                    if (!s.isContinueOnError()) {
                        Map<String, Object> out = new LinkedHashMap<>();
                        out.put("context", ctx);
                        out.put("steps", stepsOut);
                        return out;
                    }
                    continue;
                }
                stepsOut.add(stepRes);
            }
        }
        Map<String, Object> out = new LinkedHashMap<>();
        out.put("context", ctx);
        out.put("steps", stepsOut);
        return out;
    }

    private Object execStep(Step s, Map<String, Object> ctx, AgentSpec spec, RestTemplate rt) throws Exception {
        if (s.getSteps() != null && !s.getSteps().isEmpty() && (s.getType() == null || "group".equalsIgnoreCase(s.getType())) && (s.getForeach() == null || s.getForeach().isEmpty())) {
            Map<String, Object> rctx = buildRenderCtx(ctx, s);
            List<Map<String, Object>> sub = executeSubSteps(s.getSteps(), rctx, spec, rt);
            if (s.getAssign() != null && !s.getAssign().isEmpty()) {
                ctx.put(s.getAssign(), sub);
            }
            return sub;
        }
        if ("switch".equalsIgnoreCase(s.getType())) {
            String key = engine.render(s.getSwitchExpr(), ctx);
            List<Map<String, Object>> subOut;
            List<Step> matched = null;
            if (s.getCases() != null && key != null) {
                if (s.getCases().get(key) != null) {
                    matched = s.getCases().get(key);
                } else {
                    for (java.util.Map.Entry<String, java.util.List<Step>> e : s.getCases().entrySet()) {
                        String k = e.getKey();
                        if (k != null && k.startsWith("re:")) {
                            String pat = k.substring(3);
                            try {
                                if (java.util.regex.Pattern.compile(pat).matcher(key).matches()) {
                                    matched = e.getValue();
                                    break;
                                }
                            } catch (Exception ignored) {
                            }
                        }
                    }
                }
            }
            if (matched != null) {
                subOut = executeSubSteps(matched, ctx, spec, rt);
            } else {
                subOut = executeSubSteps(s.getDefaultSteps(), ctx, spec, rt);
            }
            if (s.getAssign() != null && !s.getAssign().isEmpty()) {
                ctx.put(s.getAssign(), subOut);
            }
            return subOut;
        }
        if ("parallel".equalsIgnoreCase(s.getType())) {
            if (s.getParallel() == null || s.getParallel().isEmpty()) return new LinkedHashMap<>();
            int threads = s.getParallelMaxThreads() != null && s.getParallelMaxThreads() > 0
                    ? Math.min(s.getParallelMaxThreads(), s.getParallel().size())
                    : s.getParallel().size();
            ExecutorService pool = Executors.newFixedThreadPool(Math.max(1, threads));
            try {
                Map<String, Future<List<Map<String, Object>>>> futures = new LinkedHashMap<>();
                for (ParallelBranch b : s.getParallel()) {
                    final String name = b.getName();
                    final List<Step> sub = b.getSteps();
                    final Map<String, Object> local = new LinkedHashMap<>(ctx);
                    futures.put(name, pool.submit(new Callable<List<Map<String, Object>>>() {
                        @Override
                        public List<Map<String, Object>> call() throws Exception {
                            return executeSubSteps(sub, local, spec, rt);
                        }
                    }));
                }
                Map<String, Object> result = new LinkedHashMap<>();
                for (Map.Entry<String, Future<List<Map<String, Object>>>> e : futures.entrySet()) {
                    try {
                        result.put(e.getKey(), e.getValue().get());
                    } catch (ExecutionException ex) {
                        if (!s.isContinueOnError()) {
                            throw ex.getCause() instanceof Exception ? (Exception) ex.getCause() : new RuntimeException(ex.getCause());
                        }
                        result.put(e.getKey(), "error: " + ex.getCause().getMessage());
                    }
                }
                if (s.getAssign() != null && !s.getAssign().isEmpty()) {
                    ctx.put(s.getAssign(), result);
                }
                return result;
            } finally {
                pool.shutdownNow();
            }
        }
        if (s.getForeach() != null && !s.getForeach().isEmpty()) {
            Object arrObj = resolveCtxPath(ctx, s.getForeach());
            if (!(arrObj instanceof List)) {
                throw new IllegalStateException("foreach target is not a list");
            }
            List<?> arr = (List<?>) arrObj;
            List<Object> results = new ArrayList<>();
            String itemKey = s.getItemVar() == null || s.getItemVar().isEmpty() ? "item" : s.getItemVar();
            String indexKey = s.getIndexVar() == null || s.getIndexVar().isEmpty() ? "index" : s.getIndexVar();
            for (int i = 0; i < arr.size(); i++) {
                Object it = arr.get(i);
                Map<String, Object> local = new LinkedHashMap<>(ctx);
                local.put(itemKey, it);
                local.put(indexKey, i);
                Map<String, Object> rctx = buildRenderCtx(local, s);
                if (s.getSteps() != null && !s.getSteps().isEmpty()) {
                    List<Map<String, Object>> sub = executeSubSteps(s.getSteps(), rctx, spec, rt);
                    results.add(sub);
                } else {
                    Object r = execWithRetry(s, rctx, spec, rt);
                    if (s.getExtractPath() != null && !s.getExtractPath().isEmpty() && r != null) {
                        r = extractByPath(r, s.getExtractPath());
                    }
                    results.add(r);
                }
            }
            if (s.getAssign() != null && !s.getAssign().isEmpty()) {
                ctx.put(s.getAssign(), results);
            }
            return results;
        } else {
            Map<String, Object> rctx = buildRenderCtx(ctx, s);
            Object r = execWithRetry(s, rctx, spec, rt);
            if ((s.getAssignPath() != null && !s.getAssignPath().isEmpty()) || (s.getAssign() != null && !s.getAssign().isEmpty())) {
                Object v = r;
                if (s.getExtractPath() != null && !s.getExtractPath().isEmpty() && r != null) {
                    v = extractByPath(r, s.getExtractPath());
                }
                if (s.getAssignPath() != null && !s.getAssignPath().isEmpty()) {
                    putCtxPath(ctx, s.getAssignPath(), v);
                } else {
                    ctx.put(s.getAssign(), v);
                }
            }
            return r;
        }
    }

    private Object execWithRetry(Step s, Map<String, Object> ctx, AgentSpec spec, RestTemplate rt) throws Exception {
        int times = s.getRetryTimes() != null ? Math.max(0, s.getRetryTimes()) :
                (spec.getGlobalRetryTimes() == null ? 0 : Math.max(0, spec.getGlobalRetryTimes()));
        int baseDelay = s.getRetryDelayMs() != null ? Math.max(0, s.getRetryDelayMs()) :
                (spec.getGlobalRetryDelayMs() == null ? 0 : Math.max(0, spec.getGlobalRetryDelayMs()));
        Integer initial = s.getBackoffInitialDelayMs();
        Double mult = s.getBackoffMultiplier();
        Integer maxD = s.getBackoffMaxDelayMs();
        int attempt = 0;
        while (true) {
            try {
                return execHttp(spec, s, s.getRequest(), ctx, rt);
            } catch (Exception e) {
                if (attempt >= times) {
                    throw e;
                }
                attempt++;
                int delay;
                if (initial != null && initial > 0) {
                    double m = (mult == null || mult <= 0) ? 2.0 : mult;
                    double d = initial * Math.pow(m, attempt - 1);
                    delay = (int) Math.round(d);
                    if (maxD != null && maxD > 0 && delay > maxD) delay = maxD;
                } else {
                    delay = baseDelay;
                }
                if (delay > 0) {
                    try {
                        Thread.sleep(delay);
                    } catch (InterruptedException ignored) {
                    }
                }
            }
        }
    }

    private Object execHttp(AgentSpec spec, Step s, RequestSpec r, Map<String, Object> ctx, RestTemplate rt) throws Exception {
        if (r == null) return null;
        String method = r.getMethod() == null ? "GET" : r.getMethod().toUpperCase();
        String url = engine.render(r.getUrl(), ctx);
        String key = method + " " + url;
        CBState st = circuit.get(key);
        long now = System.currentTimeMillis();
        if (st != null) {
            if (st.openUntil > now) {
                throw new RuntimeException("circuit open");
            }
            if (st.openUntil > 0 && st.openUntil <= now) {
                int halfLimit = spec.getBreakerHalfOpenMaxCalls() == null ? 1 : Math.max(1, spec.getBreakerHalfOpenMaxCalls());
                if (st.halfOpenCalls >= halfLimit) {
                    throw new RuntimeException("circuit half-open limit reached");
                }
                st.halfOpenCalls++;
            }
        }
        RestTemplate useRt = rt;
        if (s.getTimeoutMs() != null && s.getTimeoutMs() > 0) {
            SimpleClientHttpRequestFactory f = new SimpleClientHttpRequestFactory();
            f.setConnectTimeout(s.getTimeoutMs());
            f.setReadTimeout(s.getTimeoutMs());
            RestTemplate tmp = new RestTemplate(rt.getMessageConverters());
            tmp.setRequestFactory(f);
            useRt = tmp;
        }
        HttpHeaders headers = new HttpHeaders();
        if (r.getHeaders() != null) {
            for (Map.Entry<String, String> e : r.getHeaders().entrySet()) {
                headers.add(engine.render(e.getKey(), ctx), engine.render(e.getValue(), ctx));
            }
        }
        Object body = r.getBody();
        Object finalBody = body;
        if (body instanceof String) {
            finalBody = engine.render((String) body, ctx);
        } else if (body instanceof Map) {
            finalBody = renderMap((Map<?, ?>) body, ctx);
        } else if (body instanceof java.util.List) {
            finalBody = renderList((java.util.List<?>) body, ctx);
        }
        if (headers.getFirst(HttpHeaders.CONTENT_TYPE) == null) {
            headers.setContentType(MediaType.APPLICATION_JSON);
        }
        HttpMethod hm;
        try {
            hm = HttpMethod.valueOf(method);
        } catch (IllegalArgumentException ex) {
            hm = HttpMethod.GET;
        }
        HttpEntity<?> entity = finalBody != null ? new HttpEntity<>(finalBody, headers) : new HttpEntity<>(headers);
        try {
            ResponseEntity<String> resp = useRt.exchange(url, hm, entity, String.class);
            String bodyStr = resp.getBody();
            if (st != null) {
                st.failures = 0;
                st.openUntil = 0;
                st.halfOpenCalls = 0;
            }
            if (bodyStr == null) return null;
            try {
                return mapper.readValue(bodyStr, Object.class);
            } catch (Exception ex) {
                return bodyStr;
            }
        } catch (Exception ex) {
            int threshold = s.getFailureThreshold() != null ? Math.max(0, s.getFailureThreshold())
                    : (spec.getBreakerFailureThreshold() == null ? 0 : Math.max(0, spec.getBreakerFailureThreshold()));
            int openMs = s.getOpenMs() != null ? Math.max(0, s.getOpenMs())
                    : (spec.getBreakerOpenMs() == null ? 10000 : Math.max(0, spec.getBreakerOpenMs()));
            if (threshold > 0) {
                CBState state = st;
                if (state == null) {
                    state = new CBState();
                    circuit.put(key, state);
                }
                state.failures++;
                if (state.failures >= threshold) {
                    state.openUntil = System.currentTimeMillis() + openMs;
                    state.halfOpenCalls = 0;
                }
            }
            throw ex;
        }
    }

    private List<Map<String, Object>> executeSubSteps(List<Step> steps, Map<String, Object> ctx, AgentSpec spec, RestTemplate rt) throws Exception {
        List<Map<String, Object>> out = new ArrayList<>();
        if (steps == null) return out;
        for (Step s : steps) {
            Map<String, Object> stepRes = new LinkedHashMap<>();
            stepRes.put("id", s.getId());
            try {
                if (!shouldRun(s, ctx)) {
                    stepRes.put("skipped", true);
                    out.add(stepRes);
                    continue;
                }
                Object res = execStep(s, ctx, spec, rt);
                stepRes.put("ok", true);
                stepRes.put("result", res);
            } catch (Exception e) {
                stepRes.put("ok", false);
                stepRes.put("error", e.getMessage());
                out.add(stepRes);
                if (!s.isContinueOnError()) {
                    break;
                }
                continue;
            }
            out.add(stepRes);
        }
        return out;
    }
    private boolean shouldRun(Step s, Map<String, Object> ctx) {
        String expr = s.getWhen();
        if (expr == null || expr.isEmpty()) return true;
        String rendered = engine.render(expr, ctx);
        if (rendered == null) return false;
        try {
            return ExpressionEvaluator.eval(rendered);
        } catch (Exception e) {
            String v = rendered.trim().toLowerCase();
            if (v.isEmpty()) return true;
            if (v.equals("true") || v.equals("1") || v.equals("yes") || v.equals("y")) return true;
            return false;
        }
    }

    private Object resolveCtxPath(Map<String, Object> ctx, String path) {
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
    private Object extractByPath(Object res, String path) throws Exception {
        if (res == null || path == null || path.isEmpty()) return null;
        JsonNode node = mapper.valueToTree(res);
        String p = path;
        if (p.startsWith("$.")) p = p.substring(2);
        if (p.startsWith(".")) p = p.substring(1);
        String[] parts = p.split("\\.");
        JsonNode cur = node;
        for (String part : parts) {
            if (part.endsWith("]") && part.contains("[")) {
                int i = part.indexOf('[');
                String name = part.substring(0, i);
                String idxStr = part.substring(i + 1, part.length() - 1);
                if (!name.isEmpty()) cur = cur.path(name);
                int idx = Integer.parseInt(idxStr);
                cur = cur.path(idx);
            } else {
                cur = cur.path(part);
            }
        }
        if (cur.isMissingNode() || cur.isNull()) return null;
        if (cur.isValueNode()) return cur.asText();
        return mapper.convertValue(cur, Object.class);
    }

    private Map<String, Object> renderMap(Map<?, ?> map, Map<String, Object> ctx) {
        Map<String, Object> out = new HashMap<>();
        for (Map.Entry<?, ?> e : map.entrySet()) {
            Object k = e.getKey();
            Object v = e.getValue();
            String key = k == null ? "" : engine.render(String.valueOf(k), ctx);
            Object value = renderValue(v, ctx);
            out.put(key, value);
        }
        return out;
    }

    private Map<String, Object> buildRenderCtx(Map<String, Object> base, Step s) {
        Map<String, Object> rctx = new LinkedHashMap<>(base);
        if (s.getParams() != null && !s.getParams().isEmpty()) {
            Map<String, Object> renderedParams = renderMap(s.getParams(), rctx);
            rctx.putAll(renderedParams);
        }
        return rctx;
    }

    private List<Object> renderList(List<?> list, Map<String, Object> ctx) {
        List<Object> out = new ArrayList<>();
        for (Object v : list) {
            out.add(renderValue(v, ctx));
        }
        return out;
    }

    private Object renderValue(Object v, Map<String, Object> ctx) {
        if (v instanceof String) {
            return engine.render((String) v, ctx);
        } else if (v instanceof Map) {
            return renderMap((Map<?, ?>) v, ctx);
        } else if (v instanceof List) {
            return renderList((List<?>) v, ctx);
        } else {
            return v;
        }
    }

    private void putCtxPath(Map<String, Object> ctx, String path, Object value) {
        if (path == null || path.isEmpty()) return;
        String p = path;
        if (p.startsWith("$.")) p = p.substring(2);
        if (p.startsWith(".")) p = p.substring(1);
        String[] parts = p.split("\\.");
        Map<String, Object> cur = ctx;
        for (int i = 0; i < parts.length; i++) {
            String part = parts[i];
            boolean last = i == parts.length - 1;
            if (last) {
                cur.put(part, value);
            } else {
                Object next = cur.get(part);
                if (!(next instanceof Map)) {
                    Map<String, Object> m = new LinkedHashMap<>();
                    cur.put(part, m);
                    next = m;
                }
                cur = (Map<String, Object>) next;
            }
        }
    }
}
