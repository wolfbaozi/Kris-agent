package com.kris.agent.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.kris.agent.entity.ApiKey;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface ApiKeyMapper extends BaseMapper<ApiKey> {
}
