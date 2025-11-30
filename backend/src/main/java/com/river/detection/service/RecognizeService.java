package com.river.detection.service;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.river.detection.client.InferenceClient;
import com.river.detection.config.InferenceProperties;
import com.river.detection.entity.DetectionResult;
import com.river.detection.entity.ImageTask;
import com.river.detection.entity.User;
import com.river.detection.entity.VideoTask;
import com.river.detection.mapper.DetectionResultMapper;
import com.river.detection.mapper.ImageTaskMapper;
import com.river.detection.mapper.UserMapper;
import com.river.detection.mapper.VideoTaskMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.util.*;

@Service
public class RecognizeService {

    @Autowired
    private ImageTaskMapper imageTaskMapper;

    @Autowired
    private VideoTaskMapper videoTaskMapper;

    @Autowired
    private DetectionResultMapper detectionResultMapper;

    @Autowired
    private UserMapper userMapper;

    @Autowired
    private InferenceProperties inferenceProperties;

    @Autowired
    private InferenceClient inferenceClient;

    @Autowired
    private ProgressService progressService;

    @Value("${file.upload-path}")
    private String uploadPath;

    @Value("${file.image-path}")
    private String imagePath;

    @Value("${file.video-path}")
    private String videoPath;

    @Value("${file.result-path}")
    private String resultPath;

    public ImageTask recognizeImage(MultipartFile file, String username, Double conf) throws IOException {
        // 获取用户ID
        QueryWrapper<User> userWrapper = new QueryWrapper<>();
        userWrapper.eq("username", username);
        User user = userMapper.selectOne(userWrapper);
        if (user == null) {
            throw new RuntimeException("用户不存在");
        }

        // 创建任务
        ImageTask task = new ImageTask();
        task.setUserId(user.getId());
        task.setStatus(1); // 进行中
        task.setCreateTime(LocalDateTime.now());
        task.setUpdateTime(LocalDateTime.now());

        // 保存原图
        String originalFilename = file.getOriginalFilename();
        String extension = originalFilename != null ? originalFilename.substring(originalFilename.lastIndexOf('.')) : "";
        String fileName = System.currentTimeMillis() + extension;
        Path imageDir = Paths.get(imagePath);
        if (!Files.exists(imageDir)) {
            Files.createDirectories(imageDir);
        }
        Path imageFile = imageDir.resolve(fileName);
        Files.write(imageFile, file.getBytes());
        task.setImagePath(imageFile.toString());

        imageTaskMapper.insert(task);

        // 异步处理
        new Thread(() -> {
            try {
                if (inferenceProperties.isEnabled()) {
                    // 调用外部推理服务
                    InferenceClient.InferenceResponse resp = inferenceClient.inferImage(inferenceProperties.getBaseUrl(), imageFile.toFile(), conf);
                    if (resp != null && resp.getDetections() != null) {
                        for (InferenceClient.DetectionDTO dto : resp.getDetections()) {
                            DetectionResult result = new DetectionResult();
                            result.setTaskId(task.getId());
                            result.setTaskType(0);
                            result.setObjectClass(dto.getObjectClass());
                            result.setConfidence(dto.getConfidence());
                            result.setLocation(dto.getLocation());
                            result.setCreateTime(LocalDateTime.now());
                            detectionResultMapper.insert(result);
                        }
                    }
                    task.setResultPath(resp != null ? resp.getResultPath() : null);
                } else {
                    // 模拟识别
                    for (int p = 0; p <= 100; p += 25) {
                        progressService.set(0, task.getId(), p, p, 100);
                        Thread.sleep(300);
                    }
                    List<DetectionResult> results = simulateDetection(task.getId(), 0);
                    for (DetectionResult r : results) {
                        detectionResultMapper.insert(r);
                    }
                    // 将模拟结果写入统一结果目录，并返回可访问URL
                    try {
                        Path resultsDir = Paths.get(resultPath);
                        if (!Files.exists(resultsDir)) Files.createDirectories(resultsDir);
                        String baseName = String.valueOf(System.currentTimeMillis());
                        Path out = resultsDir.resolve(baseName + "_result.jpg");
                        Files.copy(Paths.get(task.getImagePath()), out);
                        task.setResultPath("/uploads/results/" + out.getFileName().toString());
                    } catch (Exception ignore) {
                        task.setResultPath(null);
                    }
                }

                task.setStatus(2); // 已完成
                task.setUpdateTime(LocalDateTime.now());
                imageTaskMapper.updateById(task);
                progressService.clear(0, task.getId());
            } catch (Exception e) {
                task.setStatus(3); // 失败
                task.setUpdateTime(LocalDateTime.now());
                imageTaskMapper.updateById(task);
                progressService.clear(0, task.getId());
            }
        }).start();

        return task;
    }

    public VideoTask recognizeVideo(MultipartFile file, String username, Double conf, Integer frames, Integer stride) throws IOException {
        QueryWrapper<User> userWrapper = new QueryWrapper<>();
        userWrapper.eq("username", username);
        User user = userMapper.selectOne(userWrapper);
        if (user == null) {
            throw new RuntimeException("用户不存在");
        }

        VideoTask task = new VideoTask();
        task.setUserId(user.getId());
        task.setStatus(1);
        task.setCreateTime(LocalDateTime.now());
        task.setUpdateTime(LocalDateTime.now());

        String originalFilename = file.getOriginalFilename();
        String extension = originalFilename != null ? originalFilename.substring(originalFilename.lastIndexOf('.')) : "";
        String fileName = System.currentTimeMillis() + extension;
        Path videoDir = Paths.get(videoPath);
        if (!Files.exists(videoDir)) {
            Files.createDirectories(videoDir);
        }
        Path videoFile = videoDir.resolve(fileName);
        Files.write(videoFile, file.getBytes());
        task.setVideoPath(videoFile.toString());

        videoTaskMapper.insert(task);

        final int total = frames != null ? Math.max(60, Math.min(frames, 5000)) : 600;
        final int taskType = 1;
        final String callbackUrl = inferenceProperties.getCallbackBase() + "/api/recognize/progress/callback";

        // 异步视频识别
        new Thread(() -> {
            try {
                if (inferenceProperties.isEnabled()) {
                    InferenceClient.InferenceResponse resp = inferenceClient.inferVideoFile(
                            inferenceProperties.getBaseUrl(),
                            videoFile.toFile(),
                            conf,
                            frames,
                            stride,
                            callbackUrl,
                            task.getId(),
                            taskType,
                            total
                    );
                    if (resp != null && resp.getDetections() != null) {
                        for (InferenceClient.DetectionDTO dto : resp.getDetections()) {
                            DetectionResult result = new DetectionResult();
                            result.setTaskId(task.getId());
                            result.setTaskType(1);
                            result.setObjectClass(dto.getObjectClass());
                            result.setConfidence(dto.getConfidence());
                            result.setLocation(dto.getLocation());
                            result.setCreateTime(LocalDateTime.now());
                            detectionResultMapper.insert(result);
                        }
                    }
                    task.setResultPath(resp != null ? resp.getResultPath() : null);
                } else {
                    // 模拟：按阶段更新进度
                    for (int p = 0; p <= 100; p += 10) {
                        progressService.set(1, task.getId(), p, p, 100);
                        Thread.sleep(400);
                    }
                    List<DetectionResult> results = simulateDetection(task.getId(), 1);
                    for (DetectionResult r : results) {
                        detectionResultMapper.insert(r);
                    }
                    // 将模拟结果写入统一结果目录，并返回可访问URL
                    try {
                        Path resultsDir = Paths.get(resultPath);
                        if (!Files.exists(resultsDir)) Files.createDirectories(resultsDir);
                        String baseName = String.valueOf(System.currentTimeMillis());
                        Path out = resultsDir.resolve(baseName + "_result.mp4");
                        Files.copy(Paths.get(task.getVideoPath()), out);
                        task.setResultPath("/uploads/results/" + out.getFileName().toString());
                    } catch (Exception ignore) {
                        task.setResultPath(null);
                    }
                }

                task.setStatus(2);
                task.setUpdateTime(LocalDateTime.now());
                videoTaskMapper.updateById(task);
                progressService.clear(1, task.getId());
            } catch (Exception e) {
                task.setStatus(3);
                task.setUpdateTime(LocalDateTime.now());
                videoTaskMapper.updateById(task);
                progressService.clear(1, task.getId());
            }
        }).start();

        return task;
    }

    public VideoTask recognizeVideoFromUrl(String videoUrl, String username, Double conf, Integer frames, Integer stride) {
        QueryWrapper<User> userWrapper = new QueryWrapper<>();
        userWrapper.eq("username", username);
        User user = userMapper.selectOne(userWrapper);
        if (user == null) {
            throw new RuntimeException("用户不存在");
        }

        VideoTask task = new VideoTask();
        task.setUserId(user.getId());
        task.setStatus(1);
        task.setVideoPath(videoUrl);
        task.setCreateTime(LocalDateTime.now());
        task.setUpdateTime(LocalDateTime.now());

        videoTaskMapper.insert(task);

        final int total = frames != null ? Math.max(60, Math.min(frames, 5000)) : 600;
        final int taskType = 1;
        final String callbackUrl = inferenceProperties.getCallbackBase() + "/api/recognize/progress/callback";

        // 异步视频流识别
        new Thread(() -> {
            try {
                if (inferenceProperties.isEnabled()) {
                    InferenceClient.InferenceResponse resp = inferenceClient.inferVideoStream(
                            inferenceProperties.getBaseUrl(),
                            videoUrl,
                            conf,
                            frames,
                            stride,
                            callbackUrl,
                            task.getId(),
                            taskType,
                            total
                    );
                    if (resp != null && resp.getDetections() != null) {
                        for (InferenceClient.DetectionDTO dto : resp.getDetections()) {
                            DetectionResult result = new DetectionResult();
                            result.setTaskId(task.getId());
                            result.setTaskType(1);
                            result.setObjectClass(dto.getObjectClass());
                            result.setConfidence(dto.getConfidence());
                            result.setLocation(dto.getLocation());
                            result.setCreateTime(LocalDateTime.now());
                            detectionResultMapper.insert(result);
                        }
                    }
                    task.setResultPath(resp != null ? resp.getResultPath() : null);
                } else {
                    for (int p = 0; p <= 100; p += 10) {
                        progressService.set(1, task.getId(), p, p, 100);
                        Thread.sleep(400);
                    }
                    List<DetectionResult> results = simulateDetection(task.getId(), 1);
                    for (DetectionResult r : results) {
                        detectionResultMapper.insert(r);
                    }
                    task.setResultPath(videoUrl + "_result");
                }

                task.setStatus(2);
                task.setUpdateTime(LocalDateTime.now());
                videoTaskMapper.updateById(task);
                progressService.clear(1, task.getId());
            } catch (Exception e) {
                task.setStatus(3);
                task.setUpdateTime(LocalDateTime.now());
                videoTaskMapper.updateById(task);
                progressService.clear(1, task.getId());
            }
        }).start();

        return task;
    }

    public Page<Map<String, Object>> getTasks(Integer page, Integer size, Integer taskType, Integer status) {
        Page<Map<String, Object>> resultPage = new Page<>(page, size);
        List<Map<String, Object>> tasks = new ArrayList<>();

        if (taskType == null || taskType == 0) {
            // 查询图片任务
            QueryWrapper<ImageTask> imageWrapper = new QueryWrapper<>();
            if (status != null) {
                imageWrapper.eq("status", status);
            }
            imageWrapper.orderByDesc("create_time");
            Page<ImageTask> imagePage = new Page<>(page, size);
            imageTaskMapper.selectPage(imagePage, imageWrapper);

            for (ImageTask task : imagePage.getRecords()) {
                Map<String, Object> taskMap = new HashMap<>();
                taskMap.put("id", task.getId());
                taskMap.put("taskType", 0);
                taskMap.put("type", "图片");
                taskMap.put("status", task.getStatus());
                taskMap.put("statusText", getStatusText(task.getStatus()));
                taskMap.put("createTime", task.getCreateTime());
                taskMap.put("imagePath", task.getImagePath());
                taskMap.put("resultPath", task.getResultPath());
                tasks.add(taskMap);
            }
        }

        if (taskType == null || taskType == 1) {
            // 查询视频任务
            QueryWrapper<VideoTask> videoWrapper = new QueryWrapper<>();
            if (status != null) {
                videoWrapper.eq("status", status);
            }
            videoWrapper.orderByDesc("create_time");
            Page<VideoTask> videoPage = new Page<>(page, size);
            videoTaskMapper.selectPage(videoPage, videoWrapper);

            for (VideoTask task : videoPage.getRecords()) {
                Map<String, Object> taskMap = new HashMap<>();
                taskMap.put("id", task.getId());
                taskMap.put("taskType", 1);
                taskMap.put("type", "视频");
                taskMap.put("status", task.getStatus());
                taskMap.put("statusText", getStatusText(task.getStatus()));
                taskMap.put("createTime", task.getCreateTime());
                taskMap.put("videoPath", task.getVideoPath());
                taskMap.put("resultPath", task.getResultPath());
                tasks.add(taskMap);
            }
        }

        // 保持各自表的数据库排序（不再在内存中重新按历史排序）
        resultPage.setRecords(tasks);
        resultPage.setTotal(tasks.size());
        return resultPage;
    }

    public Map<String, Object> getTaskDetail(Long id, Integer taskType) {
        Map<String, Object> detail = new HashMap<>();

        String resultPathStr = null;
        if (taskType == 0) {
            ImageTask task = imageTaskMapper.selectById(id);
            detail.put("task", task);
            resultPathStr = task != null ? task.getResultPath() : null;
        } else {
            VideoTask task = videoTaskMapper.selectById(id);
            detail.put("task", task);
            resultPathStr = task != null ? task.getResultPath() : null;
        }

        // 计算本地保存的绝对路径（如果是以 /uploads 开头的URL路径，则映射到本地upload目录）
        if (resultPathStr != null && resultPathStr.startsWith("/uploads/")) {
            // uploadPath 末尾不带斜杠，去掉前缀 /uploads/
            String relative = resultPathStr.substring("/uploads/".length());
            String absolute = Paths.get(uploadPath, relative).toAbsolutePath().toString();
            detail.put("resultFilePath", absolute);
        } else if (resultPathStr != null) {
            // 已经是文件系统路径
            detail.put("resultFilePath", Paths.get(resultPathStr).toAbsolutePath().toString());
        } else {
            detail.put("resultFilePath", null);
        }

        // 获取识别结果
        QueryWrapper<DetectionResult> resultWrapper = new QueryWrapper<>();
        resultWrapper.eq("task_id", id);
        resultWrapper.eq("task_type", taskType);
        List<DetectionResult> results = detectionResultMapper.selectList(resultWrapper);
        detail.put("results", results);

        return detail;
    }

    private String getStatusText(Integer status) {
        switch (status) {
            case 0:
                return "未开始";
            case 1:
                return "进行中";
            case 2:
                return "已完成";
            case 3:
                return "失败";
            default:
                return "未知";
        }
    }

    // 模拟检测结果
    private List<DetectionResult> simulateDetection(Long taskId, Integer taskType) {
        List<DetectionResult> results = new ArrayList<>();
        String[] classes = {"塑料瓶", "船", "垃圾", "树叶", "泡沫箱"};
        Random random = new Random();

        int count = random.nextInt(5) + 1; // 1-5个检测结果
        for (int i = 0; i < count; i++) {
            DetectionResult result = new DetectionResult();
            result.setTaskId(taskId);
            result.setTaskType(taskType);
            result.setObjectClass(classes[random.nextInt(classes.length)]);
            result.setConfidence(0.5 + random.nextDouble() * 0.5); // 0.5-1.0
            result.setLocation(String.format("{\"x\":%d,\"y\":%d,\"width\":%d,\"height\":%d}",
                    random.nextInt(800), random.nextInt(600), 50 + random.nextInt(200), 50 + random.nextInt(200)));
            result.setCreateTime(LocalDateTime.now());
            results.add(result);
        }

        return results;
    }
}
