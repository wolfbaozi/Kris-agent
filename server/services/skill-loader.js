import { tool } from 'ai'
import { z } from 'zod'
import pool from '../db.js'

export async function getSkillConfigs(userId, skillIds) {
  let sql = `SELECT * FROM skills
    WHERE enabled = 1
      AND (user_id = ? OR is_global = 1)`
  const params = [userId]

  if (skillIds && skillIds.length > 0) {
    const placeholders = skillIds.map(() => '?').join(',')
    sql += ` AND id IN (${placeholders})`
    params.push(...skillIds)
  }

  const [rows] = await pool.query(sql, params)
  return rows
}

export async function loadSkills(userId, skillIds) {
  const skills = await getSkillConfigs(userId, skillIds)
  const tools = {}
  const promptParts = []

  for (const skill of skills) {
    if (skill.skill_type === 'tool') {
      try {
        tools[skill.name] = buildToolSkill(skill)
      } catch (err) {
        console.error(`Skill "${skill.name}" 加载失败:`, err.message)
      }
    } else {
      promptParts.push(skill.prompt_content || '')
    }
  }

  return { tools, systemPromptAppend: promptParts.filter(Boolean).join('\n\n') }
}

function buildToolSkill(skill) {
  let schema = skill.tool_schema
  if (typeof schema === 'string') {
    schema = JSON.parse(schema)
  }

  const params = schemaToZod(schema || {})

  return tool({
    description: schema?.description || skill.name,
    parameters: params,
    execute: async (args) => {
      const fn = new Function('params', 'z', skill.tool_code)
      return fn(args)
    },
  })
}

function schemaToZod(schema) {
  const properties = schema.parameters?.properties || schema.properties
  const required = schema.parameters?.required || schema.required || []

  if (!properties) {
    return z.object({}).passthrough()
  }

  const shape = {}

  for (const [key, prop] of Object.entries(properties)) {
    let zodType = z.string()

    switch (prop.type) {
      case 'number':
      case 'integer':
        zodType = z.number()
        break
      case 'boolean':
        zodType = z.boolean()
        break
      case 'array':
        zodType = z.array(z.any())
        break
      case 'object':
        zodType = z.record(z.any())
        break
      default:
        zodType = z.string()
    }

    if (prop.description) {
      zodType = zodType.describe(prop.description)
    }
    if (!required.includes(key)) {
      zodType = zodType.optional()
    }

    shape[key] = zodType
  }

  if (Object.keys(shape).length === 0) {
    return z.object({}).passthrough()
  }

  return z.object(shape)
}
