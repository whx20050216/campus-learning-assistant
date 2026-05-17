package com.campus.learning.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.campus.learning.entity.User;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface UserMapper extends BaseMapper<User> {

    boolean existsByUsername(@Param("username") String username);

    boolean existsByStudentNo(@Param("studentNo") String studentNo);
}
