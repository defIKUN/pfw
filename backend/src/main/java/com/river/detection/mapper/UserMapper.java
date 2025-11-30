package com.river.detection.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.river.detection.entity.User;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface UserMapper extends BaseMapper<User> {
}

