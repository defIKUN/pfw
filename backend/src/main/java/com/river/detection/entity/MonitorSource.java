package com.river.detection.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("monitor_source")
public class MonitorSource {

    private Long id;
    private String name;
    private String region;
    private String description;
    private Integer status;
    private Long videoTaskId;
    private String videoPath;
    private String resultPath;
    private String coverImage;
    // 经纬度（中山市坐标系使用 WGS84）
    private Double latitude;   // 纬度
    private Double longitude;  // 经度
    /**
     * JSON 文本，存储识别结果汇总（前端可解析）
     */
    private String summary;
    private LocalDateTime lastDetectTime;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
}






