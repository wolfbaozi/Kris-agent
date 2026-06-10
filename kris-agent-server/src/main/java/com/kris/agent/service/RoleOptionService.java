package com.kris.agent.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.kris.agent.entity.RoleOption;
import com.kris.agent.mapper.RoleOptionMapper;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class RoleOptionService {

    private final RoleOptionMapper roleOptionMapper;

    public RoleOptionService(RoleOptionMapper roleOptionMapper) {
        this.roleOptionMapper = roleOptionMapper;
    }

    public List<Map<String, Object>> list() {
        LambdaQueryWrapper<RoleOption> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(RoleOption::getEnabled, 1).orderByAsc(RoleOption::getSortOrder);
        List<RoleOption> options = roleOptionMapper.selectList(wrapper);
        return options.stream().map(o -> {
            Map<String, Object> map = new HashMap<>();
            map.put("roleKey", o.getRoleKey());
            map.put("roleLabel", o.getRoleLabel());
            map.put("roleDesc", o.getRoleDesc());
            return map;
        }).collect(Collectors.toList());
    }

    public Map<String, Object> create(Map<String, Object> body) {
        RoleOption option = new RoleOption();
        option.setRoleKey((String) body.get("roleKey"));
        option.setRoleLabel((String) body.get("roleLabel"));
        option.setRoleDesc((String) body.get("roleDesc"));
        option.setSortOrder(body.get("sortOrder") != null ? ((Number) body.get("sortOrder")).intValue() : 0);
        option.setEnabled(1);
        roleOptionMapper.insert(option);
        Map<String, Object> result = new HashMap<>();
        result.put("id", option.getId());
        result.put("roleKey", option.getRoleKey());
        result.put("roleLabel", option.getRoleLabel());
        return result;
    }

    public void update(Integer id, Map<String, Object> body) {
        RoleOption option = roleOptionMapper.selectById(id);
        if (option == null) throw new RuntimeException("角色选项不存在");
        if (body.get("roleLabel") != null) option.setRoleLabel((String) body.get("roleLabel"));
        if (body.get("roleDesc") != null) option.setRoleDesc((String) body.get("roleDesc"));
        if (body.get("sortOrder") != null) option.setSortOrder(((Number) body.get("sortOrder")).intValue());
        if (body.get("enabled") != null) option.setEnabled(((Number) body.get("enabled")).intValue());
        roleOptionMapper.updateById(option);
    }

    public void delete(Integer id) {
        roleOptionMapper.deleteById(id);
    }
}
