package com.river.detection.controller;

import com.river.detection.common.Result;
import com.river.detection.service.DashboardService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/dashboard")
@CrossOrigin
public class DashboardController {

    @Autowired
    private DashboardService dashboardService;

    @GetMapping("/statistics")
    public Result<Map<String, Object>> getStatistics() {
        Map<String, Object> statistics = dashboardService.getStatistics();
        return Result.success(statistics);
    }

    @GetMapping("/charts")
    public Result<Map<String, Object>> getCharts(@RequestParam(required = false) Integer days) {
        if (days == null) {
            days = 30;
        }
        Map<String, Object> charts = dashboardService.getCharts(days);
        return Result.success(charts);
    }

    @GetMapping("/latest")
    public Result<Map<String, Object>> getLatest() {
        Map<String, Object> latest = dashboardService.getLatest();
        return Result.success(latest);
    }
}

