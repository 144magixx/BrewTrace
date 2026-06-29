package com.minyuwei.xhs.coffeeagent.shared.config;

public record XiaohongshuProperties(String cliPath, int maxReferenceCount) {
    public static XiaohongshuProperties defaults() {
        return new XiaohongshuProperties("/Users/minyuwei/.codex/skills/xiaohongshu-skills/scripts/cli.py", 5);
    }
}
