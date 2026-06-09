package com.kris.agent.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.kris.agent.entity.User;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface UserMapper extends BaseMapper<User> {
}
