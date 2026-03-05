package com.example.demo.controller;

import com.example.demo.agent.AgentExecutor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/agent")
public class AgentController {
    private final AgentExecutor executor;

    public AgentController(AgentExecutor executor) {
        this.executor = executor;
    }

    @PostMapping("/execute")
    public ResponseEntity<?> execute(@RequestParam("agentUrl") String agentUrl, @RequestBody(required = false) Map<String, Object> body) throws Exception {
        Map<String, Object> input = new HashMap<>();
        if (body != null) input.putAll(body);
        Map<String, Object> result = executor.executeFromUrl(agentUrl, input);
        return ResponseEntity.ok(result);
    }
}
