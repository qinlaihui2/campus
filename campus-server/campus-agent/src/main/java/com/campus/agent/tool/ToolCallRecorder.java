package com.campus.agent.tool;

import java.util.ArrayList;
import java.util.List;

/**
 * 工具调用记录器。每个工具执行时记录调用信息，
 * AgentService 读取后通过 SSE 发给前端展示工具卡片。
 */
public class ToolCallRecorder {

    private static final ThreadLocal<List<ToolCallEvent>> EVENTS = ThreadLocal.withInitial(ArrayList::new);

    public static void record(String name, String arguments, String result) {
        EVENTS.get().add(new ToolCallEvent(name, arguments, result));
    }

    public static List<ToolCallEvent> drain() {
        List<ToolCallEvent> events = EVENTS.get();
        EVENTS.remove();
        return events;
    }

    public record ToolCallEvent(String name, String arguments, String result) {}
}
