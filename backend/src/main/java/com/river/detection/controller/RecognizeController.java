package com.river.detection.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.river.detection.common.Result;
import com.river.detection.entity.ImageTask;
import com.river.detection.entity.VideoTask;
import com.river.detection.service.RecognizeService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.Map;

@RestController
@RequestMapping("/api/recognize")
@CrossOrigin
public class RecognizeController {

    @Autowired
    private RecognizeService recognizeService;

    @PostMapping("/image")
    public Result<ImageTask> recognizeImage(@RequestParam("file") MultipartFile file,
                                             @RequestParam(value = "conf", required = false) Double conf,
                                             Authentication authentication) {
        try {
            String username = authentication.getName();
            ImageTask task = recognizeService.recognizeImage(file, username, conf);
            return Result.success(task);
        } catch (Exception e) {
            return Result.error(e.getMessage());
        }
    }

    @PostMapping("/video")
    public Result<VideoTask> recognizeVideo(@RequestParam(value = "file", required = false) MultipartFile file,
                                             @RequestParam(value = "videoUrl", required = false) String videoUrl,
                                             @RequestParam(value = "conf", required = false) Double conf,
                                             @RequestParam(value = "frames", required = false) Integer frames,
                                             @RequestParam(value = "stride", required = false) Integer stride,
                                             Authentication authentication) {
        try {
            String username = authentication.getName();
            VideoTask task;
            if (file != null) {
                task = recognizeService.recognizeVideo(file, username, conf, frames, stride);
            } else if (videoUrl != null && !videoUrl.isEmpty()) {
                task = recognizeService.recognizeVideoFromUrl(videoUrl, username, conf, frames, stride);
            } else {
                return Result.error("请上传视频文件或输入视频流地址");
            }
            return Result.success(task);
        } catch (Exception e) {
            return Result.error(e.getMessage());
        }
    }


}

