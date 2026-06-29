package com.minyuwei.xhs.coffeeagent.agent.application;

import java.util.ArrayList;
import java.util.List;

public class InterviewAgent {
    public InterviewResult askMissingFacts(String latestUserContent) {
        List<String> questions = new ArrayList<>();
        if (!latestUserContent.contains("豆") && !latestUserContent.contains("烘焙")) {
            questions.add("豆名或烘焙商是什么？");
        }
        if (!latestUserContent.contains("水温") && !latestUserContent.contains("粉水比") && !latestUserContent.contains("92")) {
            questions.add("水温、粉水比或冲煮参数是多少？");
        }
        if (!latestUserContent.contains("克制") && !latestUserContent.contains("夸张") && !latestUserContent.contains("锐评")) {
            questions.add("你想先看克制版、夸张版还是锐评版？");
        }
        String message = questions.isEmpty()
                ? "信息已经足够，我会基于已确认事实生成三版文案。"
                : "还需要确认豆子信息、冲煮参数和你想要的文案风格。";
        return new InterviewResult(message, questions);
    }

    public record InterviewResult(String assistantMessage, List<String> pendingQuestions) {
    }
}
