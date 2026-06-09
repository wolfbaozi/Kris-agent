package com.kris.agent.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.kris.agent.entity.Skill;
import org.apache.ibatis.annotations.Mapper;

/**
 * Skill Mapper（数据访问层）
 *
 * 【前端类比】相当于 prisma.skill.findMany() / prisma.skill.create() 等
 * 继承 BaseMapper 自动拥有 CRUD 方法
 */
@Mapper
public interface SkillMapper extends BaseMapper<Skill> {
}
