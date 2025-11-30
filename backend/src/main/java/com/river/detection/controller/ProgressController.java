package com.river.detection.controller;

import com.river.detection.common.Result;
import com.river.detection.service.ProgressService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/recognize/progress")
@CrossOrigin
public class ProgressController {

    @Autowired
    private ProgressService progressService;

    public static class ProgressPayload {
        public Long taskId;
        public Integer taskType; // 0: image, 1: video
        public Integer progress; // 0-100
        public Long processed;   // processed frames
        public Long total;       // total frames
    }

    @PostMapping("/callback")
    public Result<String> callback(@RequestBody ProgressPayload payload) {
        if (payload == null || payload.taskId == null || payload.taskType == null || payload.progress == null) {
            return Result.error("参数不完整");
        }
        int p = Math.max(0, Math.min(100, payload.progress));
        long processed = payload.processed == null ? 0 : payload.processed;
        long total = payload.total == null ? 0 : payload.total;
        progressService.set(payload.taskType, payload.taskId, p, processed, total);
        return Result.success("ok");
    }

    @GetMapping
    public Result<Map<String, Object>> get(@RequestParam Long taskId, @RequestParam Integer taskType) {
        ProgressService.Progress p = progressService.get(taskType, taskId);
        Map<String, Object> data = new HashMap<>();
        data.put("progress", p == null ? 0 : p.progress);
        data.put("processed", p == null ? 0 : p.processed);
        data.put("total", p == null ? 0 : p.total);
        return Result.success(data);
    }
}



