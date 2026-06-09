package com.kris.agent.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.kris.agent.entity.FileRecord;
import org.apache.ibatis.annotations.Mapper;

/**
 * 文件记录 Mapper（数据访问层）
 *
 * 【前端类比】相当于 prisma.fileRecord.findMany() / prisma.fileRecord.create() 等
 * 继承 BaseMapper 自动拥有 CRUD 方法
 */
@Mapper
public interface FileRecordMapper extends BaseMapper<FileRecord> {
}
