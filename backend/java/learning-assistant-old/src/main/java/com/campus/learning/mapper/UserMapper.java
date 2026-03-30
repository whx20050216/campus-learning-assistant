package com.campus.learning.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.campus.learning.entity.User;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface UserMapper extends BaseMapper<User> {
}