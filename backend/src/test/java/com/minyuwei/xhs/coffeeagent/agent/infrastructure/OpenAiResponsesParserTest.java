package com.minyuwei.xhs.coffeeagent.agent.infrastructure;

import com.minyuwei.xhs.coffeeagent.agent.application.ModelMessageType;
import com.minyuwei.xhs.coffeeagent.agent.application.RecoverableModelError;
import com.minyuwei.xhs.coffeeagent.agent.infrastructure.fixtures.ModelResponseFixtures;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class OpenAiResponsesParserTest {
    private final OpenAiResponsesParser parser = new OpenAiResponsesParser();

    @Test
    void parsesConversationMessageWithTalkAndQuestions() {
        var message = parser.parseMessage(ModelResponseFixtures.conversation());

        assertEquals(ModelMessageType.CONVERSATION, message.messageType());
        assertTrue(message.talk().contains("最明显的风味"));
        assertNotNull(message.conversation());
        assertEquals(1, message.conversation().questions().size());
        assertEquals(3, message.conversation().answerOptions().size());
        assertEquals("柑橘感", message.conversation().answerOptions().getFirst().label());
        assertEquals(0, message.variants().size());
    }

    @Test
    void keepsMissingAnswerOptionsBackwardCompatible() {
        String payload = ModelResponseFixtures.conversation().replace("""
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
                """, "");

        var message = parser.parseMessage(payload);

        assertEquals(0, message.conversation().answerOptions().size());
    }

    @Test
    void parsesPostMessageWithCompleteStyleSet() {
        var message = parser.parseMessage(ModelResponseFixtures.post());

        assertEquals(ModelMessageType.POST, message.messageType());
        assertTrue(message.talk().contains("三版文案"));
        assertNotNull(message.post());
        assertEquals(3, message.post().variants().size());
        assertTrue(message.post().variants().stream().anyMatch(variant -> "RESTRAINED".equals(variant.style().name())));
        assertTrue(message.post().variants().stream().anyMatch(variant -> "EXAGGERATED".equals(variant.style().name())));
        assertTrue(message.post().variants().stream().anyMatch(variant -> "SHARP_REVIEW".equals(variant.style().name())));
    }

    @Test
    void rejectsPostWithMissingStyle() {
        ModelGatewayException exception = assertThrows(ModelGatewayException.class,
                () -> parser.parseMessage(ModelResponseFixtures.invalidPostMissingStyle()));

        assertEquals(RecoverableModelError.Code.MODEL_FORMAT_INVALID, exception.code());
    }

    @Test
    void rejectsMissingTalk() {
        String payload = ModelResponseFixtures.conversation().replace("\"talk\": \"听起来不错，你这杯喝到最明显的风味是什么？\",", "");

        ModelGatewayException exception = assertThrows(ModelGatewayException.class, () -> parser.parseMessage(payload));

        assertEquals(RecoverableModelError.Code.MODEL_FORMAT_INVALID, exception.code());
    }

    @Test
    void rejectsConversationCarryingPostDrafts() {
        String payload = ModelResponseFixtures.post().replace("\"messageType\": \"POST\"", "\"messageType\": \"CONVERSATION\"");

        ModelGatewayException exception = assertThrows(ModelGatewayException.class, () -> parser.parseMessage(payload));

        assertEquals(RecoverableModelError.Code.MODEL_FORMAT_INVALID, exception.code());
    }

    @Test
    void rejectsIllegalMessageType() {
        String payload = ModelResponseFixtures.conversation().replace("\"messageType\": \"CONVERSATION\"", "\"messageType\": \"PUBLISH\"");

        ModelGatewayException exception = assertThrows(ModelGatewayException.class, () -> parser.parseMessage(payload));

        assertEquals(RecoverableModelError.Code.MODEL_FORMAT_INVALID, exception.code());
    }

    @Test
    void rejectsConversationWithTooManyQuestions() {
        String payload = ModelResponseFixtures.conversation().replace(
                "\"questions\": [\"这杯你喝到最明显的风味是什么？\"]",
                "\"questions\": [\"这杯你喝到最明显的风味是什么？\", \"是否知道产区或处理法？\"]"
        );

        ModelGatewayException exception = assertThrows(ModelGatewayException.class, () -> parser.parseMessage(payload));

        assertEquals(RecoverableModelError.Code.MODEL_FORMAT_INVALID, exception.code());
    }

    @Test
    void rejectsConversationWithTooManyAnswerOptions() {
        String payload = ModelResponseFixtures.conversation().replace(
                """
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
                """,
                """
                    "answerOptions": [
                      {"id": "one", "label": "一", "content": "一"},
                      {"id": "two", "label": "二", "content": "二"},
                      {"id": "three", "label": "三", "content": "三"},
                      {"id": "four", "label": "四", "content": "四"},
                      {"id": "five", "label": "五", "content": "五"}
                    ],
                """
        );

        ModelGatewayException exception = assertThrows(ModelGatewayException.class, () -> parser.parseMessage(payload));

        assertEquals(RecoverableModelError.Code.MODEL_FORMAT_INVALID, exception.code());
    }
}
