package com.minyuwei.xhs.coffeeagent.agent.application;

import java.time.Instant;
import java.util.List;

public interface ModelGateway {
    /**
     * 使用已保存状态快照执行主模型调用。
     *
     * @param contextPackage 本轮模型输入状态快照
     * @return 结构化模型消息、事实增量、预览或可恢复错误
     */
    ModelResult complete(ModelContextPackage contextPackage);

    record ModelResult(
            ModelMode mode,
            String outputType,
            String modelName,
            String statusLabel,
            String sourceBoundary,
            String content,
            ModelMessageType messageType,
            String talk,
            PostModelMessage post,
            ConversationModelMessage conversation,
            List<FactUpdate> factUpdates,
            List<String> warnings,
            List<CopyVariant> variants,
            ModelPreview.ModelRequestPreview requestPreview,
            ModelPreview.ModelResponsePreview responsePreview,
            RecoverableModelError recoverableError,
            Instant generatedAt
    ) {
        /**
         * 判断模型调用是否产生了可用的结构化业务结果。
         *
         * @return 不含可恢复错误时返回 {@code true}
         */
        public boolean succeeded() {
            return recoverableError == null;
        }
    }

    record ModelRequest(String purpose, String prompt, List<String> facts, List<String> boundaries) {
    }
}
