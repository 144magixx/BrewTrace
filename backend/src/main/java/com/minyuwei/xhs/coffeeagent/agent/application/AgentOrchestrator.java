package com.minyuwei.xhs.coffeeagent.agent.application;

import com.minyuwei.xhs.coffeeagent.copywriting.domain.DraftCopy;
import com.minyuwei.xhs.coffeeagent.tasting.domain.TastingSession;

import java.util.List;

public class AgentOrchestrator {
    private final ContextAssembler contextAssembler = new ContextAssembler();
    private final ExplicitWorkflowPlanner planner = new ExplicitWorkflowPlanner();
    private final InterviewAgent interviewAgent = new InterviewAgent();
    private final DraftAgent draftAgent = new DraftAgent();
    private final ReviewAgent reviewAgent = new ReviewAgent();

    public TurnResult handleUserTurn(TastingSession session) {
        ContextAssembler.AgentContext context = contextAssembler.assemble(session);
        List<String> steps = planner.plan(context);
        InterviewAgent.InterviewResult interview = interviewAgent.askMissingFacts(session.latestUserContent());
        List<DraftCopy> drafts = interview.pendingQuestions().isEmpty() ? draftAgent.generate(context) : List.of();
        List<String> reviewNotes = drafts.stream().flatMap(draft -> reviewAgent.review(draft, context.unconfirmedFlavorCandidates()).stream()).distinct().toList();
        return new TurnResult(interview.assistantMessage(), interview.pendingQuestions(), drafts, steps, reviewNotes);
    }

    public List<DraftCopy> generateDrafts(TastingSession session) {
        return draftAgent.generate(contextAssembler.assemble(session));
    }

    public record TurnResult(
            String assistantMessage,
            List<String> pendingQuestions,
            List<DraftCopy> drafts,
            List<String> plannedSteps,
            List<String> reviewNotes
    ) {
    }
}
