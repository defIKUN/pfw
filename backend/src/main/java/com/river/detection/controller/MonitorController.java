package com.river.detection.controller;

import com.river.detection.common.Result;
import com.river.detection.entity.MonitorSource;
import com.river.detection.service.MonitorService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/monitor/sources")
@CrossOrigin
public class MonitorController {

    @Autowired
    private MonitorService monitorService;

    @GetMapping
    public Result<List<Map<String, Object>>> listSources() {
        return Result.success(monitorService.listSources());
    }

    @PostMapping
    public Result<MonitorSource> createSource(@RequestBody MonitorSource monitorSource) {
        MonitorSource source = monitorService.createSource(monitorSource);
        return Result.success(source);
    }

    @GetMapping("/{id}")
    public Result<Map<String, Object>> getDetail(@PathVariable Long id) {
        return Result.success(monitorService.getDetail(id));
    }

    @PutMapping("/{id}")
    public Result<MonitorSource> updateSource(@PathVariable Long id, @RequestBody MonitorSource monitorSource) {
        MonitorSource source = monitorService.updateSource(id, monitorSource);
        return Result.success(source);
    }

    @DeleteMapping("/{id}")
    public Result<String> deleteSource(@PathVariable Long id) {
        monitorService.deleteSource(id);
        return Result.success("删除成功");
    }
}

