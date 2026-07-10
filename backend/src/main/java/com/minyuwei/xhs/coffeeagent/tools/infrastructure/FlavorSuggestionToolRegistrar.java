package com.minyuwei.xhs.coffeeagent.tools.infrastructure;

import com.minyuwei.xhs.coffeeagent.flavor.application.FlavorSuggestionService;
import com.minyuwei.xhs.coffeeagent.tools.application.ToolRegistry;

public class FlavorSuggestionToolRegistrar {
    public void register(ToolRegistry registry, FlavorSuggestionService service) {
        registry.register(
                new ToolRegistry.ToolDefinition(
                        FlavorSuggestionToolAdapter.TOOL_NAME,
                        "根据用户给出的模糊咖啡风味词，返回待用户确认的具体风味联想候选。结果只能作为待确认联想，不能写入已确认事实。",
                        ToolRegistry.RiskLevel.LOW,
                        false,
                        inputSchema(),
                        outputSchema(),
                        ToolRegistry.ResultBoundary.PENDING_ASSOCIATION,
                        ToolRegistry.SideEffectType.NONE,
                        true
                ),
                new FlavorSuggestionToolAdapter(service)
        );
    }

    private String inputSchema() {
        return """
                {
                  "type": "object",
                  "additionalProperties": false,
                  "required": ["sessionId", "inputTerm"],
                  "properties": {
                    "sessionId": {
                      "type": "string",
                      "description": "当前咖啡品鉴会话 ID，仅用于工具调用追踪。"
                    },
                    "inputTerm": {
                      "type": "string",
                      "description": "用户已经表达过的模糊风味词，例如 柑橘、红茶、坚果。"
                    },
                    "temperatureStage": {
                      "type": "string",
                      "enum": ["HOT", "WARM", "COOL"],
                      "description": "风味出现的温度段；不确定时使用 HOT。"
                    },
                    "senseType": {
                      "type": "string",
                      "enum": ["AROMA", "TASTE"],
                      "description": "风味属于香气或味道；不确定时使用 TASTE。"
                    },
                    "limit": {
                      "type": "integer",
                      "minimum": 1,
                      "maximum": 8,
                      "description": "最多返回多少个候选。"
                    }
                  }
                }
                """;
    }

    private String outputSchema() {
        return """
                {
                  "type": "object",
                  "additionalProperties": false,
                  "required": ["tool", "executed", "inputTerm", "resultBoundary", "suggestions"],
                  "properties": {
                    "tool": {"type": "string"},
                    "executed": {"type": "boolean"},
                    "inputTerm": {"type": "string"},
                    "resultBoundary": {"type": "string", "enum": ["PENDING_ASSOCIATION"]},
                    "suggestions": {
                      "type": "array",
                      "items": {
                        "type": "object",
                        "additionalProperties": false,
                        "required": ["id", "name", "description", "basisType", "confirmationStatus", "sendStatus", "reason"],
                        "properties": {
                          "id": {"type": "string"},
                          "name": {"type": "string"},
                          "description": {"type": "string"},
                          "temperatureStage": {"type": "string"},
                          "senseType": {"type": "string"},
                          "polarity": {"type": "string"},
                          "sensoryDimensions": {"type": "array", "items": {"type": "string"}},
                          "status": {"type": "string"},
                          "basisType": {"type": "string", "enum": ["PENDING_ASSOCIATION"]},
                          "confirmationStatus": {"type": "string", "enum": ["PENDING_CONFIRMATION"]},
                          "sendStatus": {"type": "string", "enum": ["SEND_AFTER_CONFIRMATION"]},
                          "reason": {"type": "string"}
                        }
                      }
                    }
                  }
                }
                """;
    }
}
