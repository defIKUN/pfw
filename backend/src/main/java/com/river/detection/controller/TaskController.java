package com.river.detection.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.river.detection.common.Result;
import com.river.detection.service.RecognizeService;
import com.river.detection.service.TaskService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api")
@CrossOrigin
public class TaskController {

    @Autowired
    private RecognizeService recognizeService;

    @Autowired
    private TaskService taskService;

    // 与接口规范一致：GET /api/tasks
    @GetMapping("/tasks")
    public Result<Page<Map<String, Object>>> getTasks(@RequestParam(defaultValue = "1") Integer page,
                                                      @RequestParam(defaultValue = "10") Integer size,
                                                      @RequestParam(required = false) Integer taskType,
                                                      @RequestParam(required = false) Integer status) {
        Page<Map<String, Object>> tasks = recognizeService.getTasks(page, size, taskType, status);
        return Result.success(tasks);
    }

    // 与接口规范一致：GET /api/tasks/{id}
    @GetMapping("/tasks/{id}")
    public Result<Map<String, Object>> getTaskDetail(@PathVariable Long id,
                                                     @RequestParam Integer taskType) {
        Map<String, Object> task = recognizeService.getTaskDetail(id, taskType);
        return Result.success(task);
    }

    // 新增：DELETE /api/tasks/{id}?taskType=0|1
    @DeleteMapping("/tasks/{id}")
    public Result<String> deleteTask(@PathVariable Long id, @RequestParam Integer taskType) {
        taskService.deleteTask(id, taskType);
        return Result.success("删除成功");
    }
}
