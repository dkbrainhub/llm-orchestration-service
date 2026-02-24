package com.example.llm.api;

import com.example.llm.api.dto.ChatRequest;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api")
public class ChatController {

    private final ChatModel chatModel;

    public ChatController(ChatModel chatModel) {
        this.chatModel = chatModel;
    }

    @PostMapping("/chat")
    public ResponseEntity<Map<String, String>> chat(@RequestBody ChatRequest req) {
        String prompt = (req == null || req.message() == null) ? "" : req.message();
        String answer = chatModel.call(prompt);
        return ResponseEntity.ok(Map.of("response", answer));
    }

    @GetMapping("/ping")
    public Map<String, String> ping() {
        return Map.of("status", "ok");
    }
}
