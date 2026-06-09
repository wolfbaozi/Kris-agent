package com.kris.agent.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.kris.agent.entity.User;
import org.apache.ibatis.annotations.Mapper;

/**
 * 用户 Mapper（数据访问层）
 *
 * 【前端类比】相当于 Prisma 的 prisma.user.findMany() / prisma.user.create() 等 ORM 操作
 * Mapper = 数据库操作的封装，继承 BaseMapper 后自动拥有以下方法（不用手写 SQL）：
 *   - selectById(id)          -> 按主键查询（类似 findById）
 *   - selectList(wrapper)     -> 条件查询（类似 where().findMany()）
 *   - selectCount(wrapper)    -> 计数查询（类似 count()）
 *   - selectOne(wrapper)      -> 查询单条（类似 findFirst）
 *   - insert(entity)          -> 插入（类似 create()）
 *   - updateById(entity)      -> 按主键更新（类似 update()）
 *   - deleteById(id)          -> 按主键删除（类似 delete()）
 *
 * LambdaQueryWrapper 是 MyBatis-Plus 的类型安全条件构造器
 * 【前端类比】相当于 Prisma 的 where 条件：
 *   wrapper.eq(User::getUsername, "admin")  ->  where: { username: "admin" }
 */
@Mapper
public interface UserMapper extends BaseMapper<User> {
}
