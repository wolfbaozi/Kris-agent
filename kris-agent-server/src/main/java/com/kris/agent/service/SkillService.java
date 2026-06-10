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

/**
 * Skill 管理服务 —— CRUD + 启用/禁用
 *
 * 【前端类比】相当于前端的 useSkills() composable
 * 结构与 McpService 高度相似（标准的 CRUD Service 模式）
 */
@Service
public class SkillService {

    private final SkillMapper skillMapper;
    private final ObjectMapper objectMapper;
    private final AiGenService aiGenService;

    public SkillService(SkillMapper skillMapper, ObjectMapper objectMapper, AiGenService aiGenService) {
        this.skillMapper = skillMapper;
        this.objectMapper = objectMapper;
        this.aiGenService = aiGenService;
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

    /**
     * 通过自然语言描述 + AI 生成来创建 Skill
     * 适用于产品经理等不写代码的角色
     */
    public Map<String, Object> createFromDescription(Long userId, String description, String role) {
        if (description == null || description.trim().isEmpty()) {
            throw new RuntimeException("请提供 Skill 功能描述");
        }
        String safeRole = (role == null || role.trim().isEmpty()) ? "developer" : role;
        String roleGuide = "product_manager".equals(safeRole)
                ? "用户是产品经理，不会写代码。请生成 prompt 类型的 Skill，用自然语言描述清楚指令内容。"
                : "designer".equals(safeRole)
                ? "用户是设计师，可能不熟悉代码。优先生成 prompt 类型的 Skill。"
                : "用户是开发者，可以生成 tool 或 prompt 类型的 Skill。";

        String prompt = "你是一个 Skill 配置生成器。根据用户的自然语言描述，生成一个 Skill 配置。\n"
                + roleGuide + "\n\n"
                + "Skill 有两种类型：\n"
                + "1. tool - 函数式工具，需要 toolSchema（JSON Schema 描述参数）和 toolCode（JavaScript 执行代码）\n"
                + "2. prompt - 纯文本指令，只需要 promptContent（注入到系统提示词的文本）\n\n"
                + "只返回纯 JSON，不要包含 markdown 代码块标记，格式如下：\n"
                + "tool 类型: {\"skillType\":\"tool\",\"name\":\"名称\",\"description\":\"描述\",\"toolSchema\":{\"description\":\"...\",\"parameters\":{\"type\":\"object\",\"properties\":{},\"required\":[]}},\"toolCode\":\"return {}\"}\n"
                + "prompt 类型: {\"skillType\":\"prompt\",\"name\":\"名称\",\"promptContent\":\"完整的提示词内容\"}\n\n"
                + "用户描述: " + description.trim();

        String jsonResult = aiGenService.callAi(userId, prompt);
        String cleaned = jsonResult.replaceAll("```json\\s*", "").replaceAll("```\\s*", "").trim();
        try {
            Map aiResult = objectMapper.readValue(cleaned, Map.class);
            SkillRequest req = new SkillRequest();
            req.setName((String) aiResult.get("name"));
            String skillType = (String) aiResult.getOrDefault("skillType", "prompt");
            req.setSkillType(skillType);
            req.setSourceType("ai_gen");
            if ("tool".equals(skillType)) {
                req.setToolSchema(aiResult.get("toolSchema"));
                req.setToolCode((String) aiResult.get("toolCode"));
            } else {
                req.setPromptContent((String) aiResult.get("promptContent"));
            }
            return create(userId, req);
        } catch (RuntimeException e) {
            throw e;
        } catch (Exception e) {
            throw new RuntimeException("AI 生成结果解析失败: " + e.getMessage());
        }
    }

    /**
     * 通过文件内容创建 Skill
     * JSON 文件 -> 直接解析为 SkillRequest
     * 文本文件 -> 当作描述，调用 AI 生成
     */
    public Map<String, Object> createFromFile(Long userId, String fileName, String fileContent, String role) {
        if (fileName == null || fileContent == null || fileContent.trim().isEmpty()) {
            throw new RuntimeException("文件内容为空");
        }
        String lowerName = fileName.toLowerCase();
        if (lowerName.endsWith(".json")) {
            try {
                SkillRequest req = objectMapper.readValue(fileContent, SkillRequest.class);
                if (req.getSourceType() == null) req.setSourceType("file");
                return create(userId, req);
            } catch (RuntimeException e) {
                throw e;
            } catch (Exception e) {
                throw new RuntimeException("JSON 文件解析失败: " + e.getMessage());
            }
        }
        return createFromDescription(userId, fileContent, role);
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

    public Map<String, Object> export(Long userId, Long id) {
        LambdaQueryWrapper<Skill> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Skill::getUserId, userId).eq(Skill::getId, id);
        Skill skill = skillMapper.selectOne(wrapper);
        if (skill == null) {
            throw new RuntimeException("Skill配置不存在或无权限导出");
        }
        Map<String, Object> data = new HashMap<>();
        data.put("name", skill.getName());
        data.put("skillType", skill.getSkillType());
        if ("tool".equals(skill.getSkillType())) {
            try {
                data.put("toolSchema", skill.getToolSchema() != null 
                    ? objectMapper.readValue(skill.getToolSchema(), Map.class) 
                    : null);
            } catch (Exception e) {
                data.put("toolSchema", null);
            }
            data.put("toolCode", skill.getToolCode());
        } else {
            data.put("promptContent", skill.getPromptContent());
        }
        return data;
    }

    /**
     * Entity -> Map 转换
     * 全局 Skill 不暴露 toolSchema/toolCode（安全考虑，只返回 prompt 内容）
     */
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
            try {
                map.put("toolSchema", skill.getToolSchema() != null 
                    ? objectMapper.readValue(skill.getToolSchema(), Map.class) 
                    : null);
            } catch (Exception e) {
                map.put("toolSchema", null);
            }
            map.put("toolCode", skill.getToolCode());
        }
        return map;
    }
}
