package com.river.detection.client;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;
import org.springframework.core.io.FileSystemResource;

import java.io.File;
import java.util.List;

@Component
public class InferenceClient {

    @Autowired
    private RestTemplate restTemplate;

    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class DetectionDTO {
        private String objectClass;
        private Double confidence;
        private String location;
    }

    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class InferenceResponse {
        private String resultPath;
        private List<DetectionDTO> detections;
    }

    public InferenceResponse inferImage(String baseUrl, File imageFile, Double conf) {
        String url = baseUrl + "/infer/image";
        MultiValueMap<String, Object> body = new LinkedMultiValueMap<>();
        body.add("file", new FileSystemResource(imageFile));
        if (conf != null) body.add("conf", conf.toString());
        return restTemplate.postForObject(url, body, InferenceResponse.class);
    }

    public InferenceResponse inferVideoFile(String baseUrl, File videoFile, Double conf, Integer frames, Integer stride,
                                            String callbackUrl, Long taskId, Integer taskType, Integer total) {
        String url = baseUrl + "/infer/video";
        MultiValueMap<String, Object> body = new LinkedMultiValueMap<>();
        body.add("file", new FileSystemResource(videoFile));
        if (conf != null) body.add("conf", conf.toString());
        if (frames != null) body.add("frames", frames.toString());
        if (stride != null) body.add("stride", stride.toString());
        if (callbackUrl != null) body.add("callbackUrl", callbackUrl);
        if (taskId != null) body.add("taskId", taskId.toString());
        if (taskType != null) body.add("taskType", taskType.toString());
        if (total != null) body.add("total", total.toString());
        return restTemplate.postForObject(url, body, InferenceResponse.class);
    }

    public InferenceResponse inferVideoStream(String baseUrl, String videoUrl, Double conf, Integer frames, Integer stride,
                                              String callbackUrl, Long taskId, Integer taskType, Integer total) {
        String url = baseUrl + "/infer/stream";
        MultiValueMap<String, Object> body = new LinkedMultiValueMap<>();
        body.add("videoUrl", videoUrl);
        if (conf != null) body.add("conf", conf.toString());
        if (frames != null) body.add("frames", frames.toString());
        if (stride != null) body.add("stride", stride.toString());
        if (callbackUrl != null) body.add("callbackUrl", callbackUrl);
        if (taskId != null) body.add("taskId", taskId.toString());
        if (taskType != null) body.add("taskType", taskType.toString());
        if (total != null) body.add("total", total.toString());
        return restTemplate.postForObject(url, body, InferenceResponse.class);
    }
}
