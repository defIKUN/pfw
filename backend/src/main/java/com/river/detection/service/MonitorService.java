package com.river.detection.service;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.river.detection.entity.DetectionResult;
import com.river.detection.entity.MonitorSource;
import com.river.detection.entity.VideoTask;
import com.river.detection.mapper.DetectionResultMapper;
import com.river.detection.mapper.MonitorSourceMapper;
import com.river.detection.mapper.VideoTaskMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.*;

@Service
public class MonitorService {

    @Autowired
    private MonitorSourceMapper monitorSourceMapper;

    @Autowired
    private VideoTaskMapper videoTaskMapper;

    @Autowired
    private DetectionResultMapper detectionResultMapper;

    private final ObjectMapper objectMapper = new ObjectMapper();

    public List<Map<String, Object>> listSources() {
        QueryWrapper<MonitorSource> wrapper = new QueryWrapper<>();
        wrapper.orderByDesc("update_time");
        List<MonitorSource> sources = monitorSourceMapper.selectList(wrapper);
        List<Map<String, Object>> result = new ArrayList<>();
        for (MonitorSource source : sources) {
            Map<String, Object> item = new HashMap<>();
            item.put("id", source.getId());
            item.put("name", source.getName());
            item.put("region", source.getRegion());
            item.put("description", source.getDescription());
            item.put("status", source.getStatus());
            item.put("videoTaskId", source.getVideoTaskId());
            item.put("videoPath", source.getVideoPath());
            item.put("resultPath", source.getResultPath());
            item.put("coverImage", source.getCoverImage());
            item.put("latitude", source.getLatitude());
            item.put("longitude", source.getLongitude());
            item.put("lastDetectTime", source.getLastDetectTime());
            item.put("createTime", source.getCreateTime());
            item.put("updateTime", source.getUpdateTime());
            Map<String, Integer> summary = parseSummary(source.getSummary());
            item.put("summary", summary);
            result.add(item);
        }
        return result;
    }

    public MonitorSource createSource(MonitorSource payload) {
        LocalDateTime now = LocalDateTime.now();
        payload.setCreateTime(now);
        payload.setUpdateTime(now);
        if (payload.getStatus() == null) {
            payload.setStatus(1);
        }
        fillTaskInfo(payload);
        monitorSourceMapper.insert(payload);
        return monitorSourceMapper.selectById(payload.getId());
    }

    public MonitorSource updateSource(Long id, MonitorSource payload) {
        MonitorSource existing = monitorSourceMapper.selectById(id);
        if (existing == null) {
            throw new RuntimeException("监控点不存在");
        }
        if (payload.getName() != null) {
            existing.setName(payload.getName());
        }
        if (payload.getRegion() != null) {
            existing.setRegion(payload.getRegion());
        }
        if (payload.getDescription() != null) {
            existing.setDescription(payload.getDescription());
        }
        if (payload.getStatus() != null) {
            existing.setStatus(payload.getStatus());
        }
        if (payload.getCoverImage() != null) {
            existing.setCoverImage(payload.getCoverImage());
        }
        if (payload.getLatitude() != null) {
            existing.setLatitude(payload.getLatitude());
        }
        if (payload.getLongitude() != null) {
            existing.setLongitude(payload.getLongitude());
        }
        boolean taskChanged = payload.getVideoTaskId() != null &&
                !payload.getVideoTaskId().equals(existing.getVideoTaskId());
        if (payload.getVideoTaskId() != null) {
            existing.setVideoTaskId(payload.getVideoTaskId());
        }
        existing.setUpdateTime(LocalDateTime.now());
        if (taskChanged) {
            fillTaskInfo(existing);
        } else {
            if (payload.getVideoPath() != null) {
                existing.setVideoPath(payload.getVideoPath());
            }
            if (payload.getResultPath() != null) {
                existing.setResultPath(payload.getResultPath());
            }
            if (payload.getSummary() != null) {
                existing.setSummary(payload.getSummary());
            }
            if (payload.getLastDetectTime() != null) {
                existing.setLastDetectTime(payload.getLastDetectTime());
            }
        }
        monitorSourceMapper.updateById(existing);
        return existing;
    }

    public void deleteSource(Long id) {
        monitorSourceMapper.deleteById(id);
    }

    public Map<String, Object> getDetail(Long id) {
        MonitorSource source = monitorSourceMapper.selectById(id);
        if (source == null) {
            throw new RuntimeException("监控点不存在");
        }
        Map<String, Integer> summary = parseSummary(source.getSummary());
        if ((summary == null || summary.isEmpty()) && source.getVideoTaskId() != null) {
            summary = aggregateSummary(source.getVideoTaskId());
        }

        List<DetectionResult> detections = Collections.emptyList();
        if (source.getVideoTaskId() != null) {
            QueryWrapper<DetectionResult> wrapper = new QueryWrapper<>();
            wrapper.eq("task_id", source.getVideoTaskId())
                    .eq("task_type", 1)
                    .orderByDesc("create_time");
            detections = detectionResultMapper.selectList(wrapper);
        }
        VideoTask task = source.getVideoTaskId() != null ? videoTaskMapper.selectById(source.getVideoTaskId()) : null;

        Map<String, Object> detail = new HashMap<>();
        detail.put("monitor", source);
        detail.put("summary", summary);
        detail.put("detections", detections);
        detail.put("task", task);
        return detail;
    }

    private void fillTaskInfo(MonitorSource source) {
        if (source.getVideoTaskId() == null) {
            return;
        }
        VideoTask task = videoTaskMapper.selectById(source.getVideoTaskId());
        if (task == null) {
            return;
        }
        source.setVideoPath(task.getVideoPath());
        if (task.getResultPath() != null) {
            source.setResultPath(task.getResultPath());
        }
        source.setLastDetectTime(task.getUpdateTime());
        Map<String, Integer> summary = aggregateSummary(task.getId());
        source.setSummary(writeSummary(summary));
    }

    private Map<String, Integer> aggregateSummary(Long taskId) {
        QueryWrapper<DetectionResult> wrapper = new QueryWrapper<>();
        wrapper.eq("task_id", taskId)
                .eq("task_type", 1);
        List<DetectionResult> results = detectionResultMapper.selectList(wrapper);
        Map<String, Integer> summary = new LinkedHashMap<>();
        for (DetectionResult result : results) {
            summary.merge(result.getObjectClass(), 1, Integer::sum);
        }
        return summary;
    }

    private Map<String, Integer> parseSummary(String summaryJson) {
        if (summaryJson == null || summaryJson.isEmpty()) {
            return Collections.emptyMap();
        }
        try {
            return objectMapper.readValue(summaryJson, new TypeReference<Map<String, Integer>>() {});
        } catch (Exception e) {
            return Collections.emptyMap();
        }
    }

    private String writeSummary(Map<String, Integer> summary) {
        try {
            return objectMapper.writeValueAsString(summary);
        } catch (Exception e) {
            return "{}";
        }
    }
}






