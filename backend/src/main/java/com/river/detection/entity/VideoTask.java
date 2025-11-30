package com.river.detection.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("video_task")
public class VideoTask {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long userId;
    private String videoPath;
    private String resultPath;
    private Integer status; // 0:未开始，1:进行中，2:已完成，3:失败
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
}

