package com.kris.agent.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.kris.agent.entity.McpServer;
import org.apache.ibatis.annotations.Mapper;

/**
 * MCP 服务器 Mapper（数据访问层）
 *
 * 【前端类比】相当于 prisma.mcpServer.findMany() / prisma.mcpServer.create() 等
 * 继承 BaseMapper 自动拥有 CRUD 方法
 */
@Mapper
public interface McpServerMapper extends BaseMapper<McpServer> {
}
