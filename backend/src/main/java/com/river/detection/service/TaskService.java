package com.river.detection.service;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.river.detection.entity.DetectionResult;
import com.river.detection.entity.ImageTask;
import com.river.detection.entity.VideoTask;
import com.river.detection.mapper.DetectionResultMapper;
import com.river.detection.mapper.ImageTaskMapper;
import com.river.detection.mapper.VideoTaskMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

@Service
public class TaskService {

    @Autowired
    private ImageTaskMapper imageTaskMapper;

    @Autowired
    private VideoTaskMapper videoTaskMapper;

    @Autowired
    private DetectionResultMapper detectionResultMapper;

    @Value("${file.upload-path}")
    private String uploadRoot;

    public void deleteTask(Long id, Integer taskType) {
        if (taskType == null) throw new IllegalArgumentException("taskType 不能为空");

        if (taskType == 0) {
            ImageTask task = imageTaskMapper.selectById(id);
            if (task != null) {
                // 删除数据库 detection_result
                QueryWrapper<DetectionResult> wrapper = new QueryWrapper<>();
                wrapper.eq("task_id", id).eq("task_type", 0);
                detectionResultMapper.delete(wrapper);

                // 删除文件：原图与结果
                safeDeleteFsPath(task.getImagePath());
                safeDeleteResult(task.getResultPath());

                // 删除任务记录
                imageTaskMapper.deleteById(id);
            }
        } else if (taskType == 1) {
            VideoTask task = videoTaskMapper.selectById(id);
            if (task != null) {
                QueryWrapper<DetectionResult> wrapper = new QueryWrapper<>();
                wrapper.eq("task_id", id).eq("task_type", 1);
                detectionResultMapper.delete(wrapper);

                // 删除文件：原视频与结果
                safeDeleteFsPath(task.getVideoPath());
                safeDeleteResult(task.getResultPath());

                videoTaskMapper.deleteById(id);
            }
        } else {
            throw new IllegalArgumentException("无效 taskType: " + taskType);
        }
    }

    private void safeDeleteResult(String resultPath) {
        if (resultPath == null || resultPath.isEmpty()) return;
        // 如果是 /uploads/ 开头，映射到物理路径
        if (resultPath.startsWith("/uploads/")) {
            String relative = resultPath.substring("/uploads/".length());
            Path abs = Paths.get(uploadRoot, relative);
            safeDelete(abs);
        } else {
            // 可能是绝对路径或相对文件系统路径
            safeDelete(Paths.get(resultPath));
        }
    }

    private void safeDeleteFsPath(String fsPath) {
        if (fsPath == null || fsPath.isEmpty()) return;
        safeDelete(Paths.get(fsPath));
    }

    private void safeDelete(Path path) {
        try {
            if (path != null && Files.exists(path)) {
                Files.delete(path);
            }
        } catch (Exception ignore) {
        }
    }
}




