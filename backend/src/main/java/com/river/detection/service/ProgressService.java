package com.river.detection.service;

import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class ProgressService {

    public static class Progress {
        public int progress; // 0-100
        public long processed; // 已处理帧数
        public long total; // 总帧数（或预估）
    }

    private final Map<String, Progress> store = new ConcurrentHashMap<>();

    private String key(int taskType, long taskId) {
        return taskType + ":" + taskId;
    }

    public void set(int taskType, long taskId, int progress, long processed, long total) {
        Progress p = new Progress();
        p.progress = Math.max(0, Math.min(100, progress));
        p.processed = Math.max(0, processed);
        p.total = Math.max(0, total);
        store.put(key(taskType, taskId), p);
    }

    public Progress get(int taskType, long taskId) {
        return store.get(key(taskType, taskId));
    }

    public void clear(int taskType, long taskId) {
        store.remove(key(taskType, taskId));
    }
}



