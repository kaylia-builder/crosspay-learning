package com.crosspay.ai;

import com.crosspay.common.dto.ApiResponse;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

/**
 * AI 助手 API
 *
 * 接收自然语言查询，返回结构化分析结果。
 * 前端可以用聊天界面展示，也可以作为 Slack Bot / 企微 Bot 的接口。
 */
@RestController
@RequestMapping("/api/ai")
public class AiAssistantController {

    private final AiAssistantService aiAssistantService;

    public AiAssistantController(AiAssistantService aiAssistantService) {
        this.aiAssistantService = aiAssistantService;
    }

    @PostMapping("/query")
    public ApiResponse<Map<String, String>> query(@RequestBody Map<String, String> body) {
        String question = body.getOrDefault("question", "");
        String answer = aiAssistantService.answer(question);

        Map<String, String> result = Map.of(
                "question", question,
                "answer", answer
        );
        return ApiResponse.ok(result);
    }
}
