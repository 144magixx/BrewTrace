package com.minyuwei.xhs.coffeeagent.agent.infrastructure;

import com.minyuwei.xhs.coffeeagent.agent.application.FactUpdate;
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
    void parsesTypedFactUpdatesWithEvidenceAndKeepsMissingUpdatesBackwardCompatible() {
        var message = parser.parseMessage(ModelResponseFixtures.conversationWithFactUpdates());

        assertEquals(2, message.factUpdates().size());
        assertEquals(FactUpdate.Action.ADD_CONFIRMED_FACT, message.factUpdates().getFirst().action());
        assertEquals("message-1", message.factUpdates().getFirst().sourceMessageId());
        assertEquals("柑橘感", message.factUpdates().getFirst().sourceQuote());
        assertTrue(parser.parseMessage(ModelResponseFixtures.conversation()).factUpdates().isEmpty());
    }

    @Test
    void keepsMissingAnswerOptionsBackwardCompatible() {
        var message = parser.parseMessage(ModelResponseFixtures.conversationWithoutAnswerOptions());

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
        ModelGatewayException exception = assertThrows(ModelGatewayException.class,
                () -> parser.parseMessage(ModelResponseFixtures.conversationWithoutTalk()));

        assertEquals(RecoverableModelError.Code.MODEL_FORMAT_INVALID, exception.code());
    }

    @Test
    void rejectsConversationCarryingPostDrafts() {
        ModelGatewayException exception = assertThrows(ModelGatewayException.class,
                () -> parser.parseMessage(ModelResponseFixtures.conversationCarryingPostDrafts()));

        assertEquals(RecoverableModelError.Code.MODEL_FORMAT_INVALID, exception.code());
    }

    @Test
    void rejectsIllegalMessageType() {
        ModelGatewayException exception = assertThrows(ModelGatewayException.class,
                () -> parser.parseMessage(ModelResponseFixtures.illegalMessageType()));

        assertEquals(RecoverableModelError.Code.MODEL_FORMAT_INVALID, exception.code());
    }

    @Test
    void rejectsConversationWithTooManyQuestions() {
        ModelGatewayException exception = assertThrows(ModelGatewayException.class,
                () -> parser.parseMessage(ModelResponseFixtures.conversationWithTooManyQuestions()));

        assertEquals(RecoverableModelError.Code.MODEL_FORMAT_INVALID, exception.code());
    }

    @Test
    void rejectsConversationWithTooManyAnswerOptions() {
        ModelGatewayException exception = assertThrows(ModelGatewayException.class,
                () -> parser.parseMessage(ModelResponseFixtures.conversationWithTooManyAnswerOptions()));

        assertEquals(RecoverableModelError.Code.MODEL_FORMAT_INVALID, exception.code());
    }
}
