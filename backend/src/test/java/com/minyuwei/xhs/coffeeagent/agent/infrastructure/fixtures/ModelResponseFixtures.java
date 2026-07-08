package com.minyuwei.xhs.coffeeagent.agent.infrastructure.fixtures;

public final class ModelResponseFixtures {
    private ModelResponseFixtures() {
    }

    public static String conversation() {
        return """
                {
                  "messageType": "CONVERSATION",
                  "talk": "听起来不错，你这杯喝到最明显的风味是什么？",
                  "conversation": {
                    "questions": ["这杯你喝到最明显的风味是什么？"],
                    "answerOptions": [
                      {
                        "id": "citrus",
                        "label": "柑橘感",
                        "content": "我喝到比较明显的柑橘感。"
                      },
                      {
                        "id": "black_tea",
                        "label": "红茶感",
                        "content": "我喝到一点红茶感。"
                      },
                      {
                        "id": "not_sure",
                        "label": "说不清",
                        "content": "我暂时说不太清楚，只觉得整体比较干净。"
                      }
                    ],
                    "pendingConfirmations": [{
                      "expression": "主要风味仍需用户补充",
                      "basisType": "PENDING_ASSOCIATION",
                      "sourceReference": "model.routing",
                      "sourceId": "",
                      "confidenceLabel": "LOW"
                    }],
                    "warnings": ["当前信息不足，不能生成真实咖啡品鉴文案。"]
                  },
                  "post": null,
                  "warnings": []
                }
                """;
    }

    public static String post() {
        return """
                {
                  "messageType": "POST",
                  "talk": "信息已经够了，我先整理成三版文案，你可以选一版再继续改。",
                  "conversation": null,
                  "post": {
                    "variants": [
                      {
                        "style": "RESTRAINED",
                        "styleLabel": "克制版",
                        "title": "橙色茶感的水洗埃塞",
                        "body": "这杯水洗埃塞喝起来更偏橙柑和红茶感，整体干净。",
                        "tags": ["手冲咖啡", "咖啡品鉴"],
                        "factUsages": [{"expression":"水洗埃塞、橙柑和红茶感","basisType":"USER_CONFIRMED","sourceReference":"currentSession[0].content","sourceId":"context-1","confidenceLabel":"HIGH"}],
                        "inferences": [],
                        "pendingConfirmations": [],
                        "warnings": []
                      },
                      {
                        "style": "EXAGGERATED",
                        "styleLabel": "夸张版",
                        "title": "一杯橙色茶汤炸开了",
                        "body": "这支水洗埃塞像把橙柑和红茶一起推到杯口。",
                        "tags": ["手冲咖啡", "咖啡豆分享"],
                        "factUsages": [],
                        "inferences": [],
                        "pendingConfirmations": [],
                        "warnings": []
                      },
                      {
                        "style": "SHARP_REVIEW",
                        "styleLabel": "锐评版",
                        "title": "这杯水洗埃塞至少没乱装",
                        "body": "先说结论：这杯靠干净度和茶感站住。",
                        "tags": ["咖啡锐评", "手冲咖啡"],
                        "factUsages": [],
                        "inferences": [],
                        "pendingConfirmations": [],
                        "warnings": []
                      }
                    ],
                    "warnings": []
                  },
                  "warnings": []
                }
                """;
    }

    public static String invalidPostMissingStyle() {
        return post().replace("\"style\": \"SHARP_REVIEW\"", "\"style\": \"EXAGGERATED\"");
    }
}
