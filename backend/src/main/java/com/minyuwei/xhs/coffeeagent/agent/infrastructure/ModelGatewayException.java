package com.minyuwei.xhs.coffeeagent.agent.infrastructure;

import com.minyuwei.xhs.coffeeagent.agent.application.RecoverableModelError;

public class ModelGatewayException extends RuntimeException {
    private final RecoverableModelError.Code code;

    public ModelGatewayException(RecoverableModelError.Code code, String message) {
        super(message);
        this.code = code;
    }

    public RecoverableModelError.Code code() {
        return code;
    }
}
