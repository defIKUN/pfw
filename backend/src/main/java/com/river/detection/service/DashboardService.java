package com.river.detection.service;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.river.detection.entity.DetectionResult;
import com.river.detection.entity.ImageTask;
import com.river.detection.entity.VideoTask;
import com.river.detection.mapper.DetectionResultMapper;
import com.river.detection.mapper.ImageTaskMapper;
import com.river.detection.mapper.VideoTaskMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class DashboardService {

    @Autowired
    private ImageTaskMapper imageTaskMapper;

    @Autowired
    private VideoTaskMapper videoTaskMapper;

    @Autowired
    private DetectionResultMapper detectionResultMapper;

    public Map<String, Object> getStatistics() {
        LocalDate today = LocalDate.now();
        LocalDateTime startOfDay = today.atStartOfDay();
        LocalDateTime endOfDay = today.plusDays(1).atStartOfDay();

        // 今日识别图片数
        QueryWrapper<ImageTask> imageWrapper = new QueryWrapper<>();
        imageWrapper.between("create_time", startOfDay, endOfDay);
        Long todayImageCount = imageTaskMapper.selectCount(imageWrapper);

        // 今日识别视频数
        QueryWrapper<VideoTask> videoWrapper = new QueryWrapper<>();
        videoWrapper.between("create_time", startOfDay, endOfDay);
        Long todayVideoCount = videoTaskMapper.selectCount(videoWrapper);

        // 识别漂浮物总数
        Long totalDetections = detectionResultMapper.selectCount(null);

        Map<String, Object> statistics = new HashMap<>();
        statistics.put("todayImageCount", todayImageCount);
        statistics.put("todayVideoCount", todayVideoCount);
        statistics.put("totalDetections", totalDetections);
        return statistics;
    }

    public Map<String, Object> getCharts(Integer days) {
        LocalDate endDate = LocalDate.now();
        LocalDate startDate = endDate.minusDays(days - 1);
        LocalDateTime startDateTime = startDate.atStartOfDay();
        LocalDateTime endDateTime = endDate.plusDays(1).atStartOfDay();

        // 历史识别数量趋势（按日期统计）
        List<Map<String, Object>> trendData = new ArrayList<>();
        for (LocalDate date = startDate; !date.isAfter(endDate); date = date.plusDays(1)) {
            LocalDateTime dayStart = date.atStartOfDay();
            LocalDateTime dayEnd = date.plusDays(1).atStartOfDay();

            QueryWrapper<ImageTask> imageWrapper = new QueryWrapper<>();
            imageWrapper.between("create_time", dayStart, dayEnd);
            Long imageCount = imageTaskMapper.selectCount(imageWrapper);

            QueryWrapper<VideoTask> videoWrapper = new QueryWrapper<>();
            videoWrapper.between("create_time", dayStart, dayEnd);
            Long videoCount = videoTaskMapper.selectCount(videoWrapper);

            Map<String, Object> dayData = new HashMap<>();
            dayData.put("date", date.toString());
            dayData.put("imageCount", imageCount);
            dayData.put("videoCount", videoCount);
            dayData.put("totalCount", imageCount + videoCount);
            trendData.add(dayData);
        }

        // 漂浮物类别比例
        QueryWrapper<DetectionResult> resultWrapper = new QueryWrapper<>();
        resultWrapper.between("create_time", startDateTime, endDateTime);
        List<DetectionResult> results = detectionResultMapper.selectList(resultWrapper);

        Map<String, Long> classCountMap = results.stream()
                .collect(Collectors.groupingBy(DetectionResult::getObjectClass, Collectors.counting()));

        List<Map<String, Object>> pieData = new ArrayList<>();
        for (Map.Entry<String, Long> entry : classCountMap.entrySet()) {
            Map<String, Object> pieItem = new HashMap<>();
            pieItem.put("name", entry.getKey());
            pieItem.put("value", entry.getValue());
            pieData.add(pieItem);
        }

        Map<String, Object> charts = new HashMap<>();
        charts.put("trendData", trendData);
        charts.put("pieData", pieData);
        return charts;
    }

    public Map<String, Object> getLatest() {
        // 获取最新的图片识别结果
        QueryWrapper<ImageTask> imageWrapper = new QueryWrapper<>();
        imageWrapper.eq("status", 2).orderByDesc("create_time").last("LIMIT 5");
        List<ImageTask> latestImages = imageTaskMapper.selectList(imageWrapper);

        // 获取最新的视频识别结果
        QueryWrapper<VideoTask> videoWrapper = new QueryWrapper<>();
        videoWrapper.eq("status", 2).orderByDesc("create_time").last("LIMIT 5");
        List<VideoTask> latestVideos = videoTaskMapper.selectList(videoWrapper);

        Map<String, Object> latest = new HashMap<>();
        latest.put("images", latestImages);
        latest.put("videos", latestVideos);
        return latest;
    }
}

