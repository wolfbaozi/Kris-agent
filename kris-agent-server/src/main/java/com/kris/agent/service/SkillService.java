package com.kris.agent.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.kris.agent.dto.SkillRequest;
import com.kris.agent.entity.Skill;
import com.kris.agent.mapper.SkillMapper;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class SkillService {

    private final SkillMapper skillMapper;
    private final ObjectMapper objectMapper;

    public SkillService(SkillMapper skillMapper, ObjectMapper objectMapper) {
        this.skillMapper = skillMapper;
        this.objectMapper = objectMapper;
    }

    public List<Map<String, Object>> list(Long userId) {
        LambdaQueryWrapper<Skill> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Skill::getEnabled, 1)
                .and(w -> w.eq(Skill::getUserId, userId).or().eq(Skill::getIsGlobal, 1));
        List<Skill> skills = skillMapper.selectList(wrapper);
        return skills.stream().map(s -> toMap(s, userId)).collect(Collectors.toList());
    }

    public Map<String, Object> create(Long userId, SkillRequest request) {
        if (request.getName() == null || request.getName().trim().isEmpty()) {
            throw new RuntimeException("Skill名称不能为空");
        }
        LambdaQueryWrapper<Skill> dupWrapper = new LambdaQueryWrapper<>();
        dupWrapper.eq(Skill::getUserId, userId).eq(Skill::getName, request.getName());
        if (skillMapper.selectCount(dupWrapper) > 0) {
            throw new RuntimeException("Skill名称\"" + request.getName() + "\"已存在");
        }
        Skill skill = new Skill();
        skill.setUserId(userId);
        skill.setName(request.getName());
        skill.setSkillType(request.getSkillType());
        skill.setSourceType(request.getSourceType() != null ? request.getSourceType() : "database");
        if (request.getToolSchema() != null) {
            try {
                skill.setToolSchema(objectMapper.writeValueAsString(request.getToolSchema()));
            } catch (JsonProcessingException e) {
                throw new RuntimeException("toolSchema序列化失败");
            }
        }
        skill.setToolCode(request.getToolCode());
        skill.setPromptContent(request.getPromptContent());
        skill.setFilePath(request.getFilePath() != null ? request.getFilePath() : "");
        skillMapper.insert(skill);
        Map<String, Object> result = new HashMap<>();
        result.put("id", skill.getId());
        result.put("name", skill.getName());
        return result;
    }

    public void update(Long userId, Long id, SkillRequest request) {
        LambdaQueryWrapper<Skill> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Skill::getUserId, userId).eq(Skill::getId, id);
        Skill skill = skillMapper.selectOne(wrapper);
        if (skill == null) {
            throw new RuntimeException("Skill配置不存在或无权限修改");
        }
        if (request.getName() != null && !request.getName().equals(skill.getName())) {
            LambdaQueryWrapper<Skill> dupWrapper = new LambdaQueryWrapper<>();
            dupWrapper.eq(Skill::getUserId, userId)
                    .eq(Skill::getName, request.getName())
                    .ne(Skill::getId, id);
            if (skillMapper.selectCount(dupWrapper) > 0) {
                throw new RuntimeException("Skill名称\"" + request.getName() + "\"已存在");
            }
            skill.setName(request.getName());
        }
        if (request.getSkillType() != null) {
            skill.setSkillType(request.getSkillType());
        }
        if (request.getSourceType() != null) {
            skill.setSourceType(request.getSourceType());
        }
        if (request.getToolSchema() != null) {
            try {
                skill.setToolSchema(objectMapper.writeValueAsString(request.getToolSchema()));
            } catch (JsonProcessingException e) {
                throw new RuntimeException("toolSchema序列化失败");
            }
        }
        if (request.getToolCode() != null) {
            skill.setToolCode(request.getToolCode());
        }
        if (request.getPromptContent() != null) {
            skill.setPromptContent(request.getPromptContent());
        }
        if (request.getFilePath() != null) {
            skill.setFilePath(request.getFilePath());
        }
        if (request.getIsGlobal() != null) {
            skill.setIsGlobal(request.getIsGlobal());
        }
        skillMapper.updateById(skill);
    }

    public void delete(Long userId, Long id) {
        LambdaQueryWrapper<Skill> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Skill::getUserId, userId).eq(Skill::getId, id);
        if (skillMapper.selectCount(wrapper) == 0) {
            throw new RuntimeException("Skill配置不存在或无权限删除");
        }
        skillMapper.deleteById(id);
    }

    public Map<String, Object> toggle(Long userId, Long id) {
        LambdaQueryWrapper<Skill> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Skill::getUserId, userId).eq(Skill::getId, id);
        Skill skill = skillMapper.selectOne(wrapper);
        if (skill == null) {
            throw new RuntimeException("Skill配置不存在或无权限操作");
        }
        int newEnabled = (skill.getEnabled() != null && skill.getEnabled() == 1) ? 0 : 1;
        skill.setEnabled(newEnabled);
        skillMapper.updateById(skill);
        Map<String, Object> result = new HashMap<>();
        result.put("enabled", newEnabled == 1);
        return result;
    }

    private Map<String, Object> toMap(Skill skill, Long currentUserId) {
        Map<String, Object> map = new HashMap<>();
        map.put("id", skill.getId());
        map.put("userId", skill.getUserId());
        map.put("name", skill.getName());
        map.put("isGlobal", skill.getIsGlobal());
        map.put("skillType", skill.getSkillType());
        map.put("sourceType", skill.getSourceType());
        map.put("promptContent", skill.getPromptContent());
        map.put("filePath", skill.getFilePath());
        map.put("enabled", skill.getEnabled());
        map.put("createdAt", skill.getCreatedAt());
        if (skill.getIsGlobal() != 1 && skill.getUserId().equals(currentUserId)) {
            map.put("toolSchema", skill.getToolSchema());
            map.put("toolCode", skill.getToolCode());
        }
        return map;
    }
}
