package com.kris.agent.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.kris.agent.entity.ApiKey;
import org.apache.ibatis.annotations.Mapper;

/**
 * API Key Mapper（数据访问层）
 *
 * 【前端类比】相当于 prisma.apiKey.findMany() / prisma.apiKey.create() 等
 * 继承 BaseMapper 自动拥有 CRUD 方法，不用手写 SQL
 */
@Mapper
public interface ApiKeyMapper extends BaseMapper<ApiKey> {
}
