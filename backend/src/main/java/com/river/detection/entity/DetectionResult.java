package com.river.detection.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("detection_result")
public class DetectionResult {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long taskId;
    private Integer taskType; // 0:图片，1:视频
    private String objectClass;
    private Double confidence;
    private String location; // JSON格式
    private LocalDateTime createTime;
}

