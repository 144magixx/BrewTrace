package com.minyuwei.xhs.coffeeagent.support;

import com.minyuwei.xhs.coffeeagent.flavor.application.FlavorSuggestionGenerator;

import java.util.List;

public final class FakeFlavorSuggestionGenerator implements FlavorSuggestionGenerator {
    private final List<FlavorCandidate> candidates;
    private final RuntimeException failure;
    private GenerationRequest lastRequest;

    public FakeFlavorSuggestionGenerator(List<FlavorCandidate> candidates) {
        this(candidates, null);
    }

    private FakeFlavorSuggestionGenerator(List<FlavorCandidate> candidates, RuntimeException failure) {
        this.candidates = candidates;
        this.failure = failure;
    }

    public static FakeFlavorSuggestionGenerator failing(RuntimeException failure) {
        return new FakeFlavorSuggestionGenerator(List.of(), failure);
    }

    @Override
    public List<FlavorCandidate> generate(GenerationRequest request) {
        lastRequest = request;
        if (failure != null) {
            throw failure;
        }
        return candidates;
    }

    public GenerationRequest lastRequest() {
        return lastRequest;
    }
}
