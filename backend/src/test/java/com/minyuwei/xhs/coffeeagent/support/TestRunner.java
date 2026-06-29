package com.minyuwei.xhs.coffeeagent.support;

public final class TestRunner {
    private TestRunner() {
    }

    public static void main(String[] args) {
        FoundationSmokeTest.run();
        System.out.println("backend behavior tests passed");
    }
}
