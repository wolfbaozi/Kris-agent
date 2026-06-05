import { Client } from '@modelcontextprotocol/sdk/client/index.js'
import { StdioClientTransport } from '@modelcontextprotocol/sdk/client/stdio.js'
import { tool } from 'ai'
import { z } from 'zod'
import pool from '../db.js'

const currentEnv = process.env.RUN_ENV || 'production'

export async function getMcpConfigs(userId, mcpIds) {
  let sql = `SELECT * FROM mcp_servers
    WHERE enabled = 1
      AND (run_env = 'all' OR run_env = ?)
      AND (user_id = ? OR is_global = 1)`
  const params = [currentEnv, userId]

  if (mcpIds && mcpIds.length > 0) {
    const placeholders = mcpIds.map(() => '?').join(',')
    sql += ` AND id IN (${placeholders})`
    params.push(...mcpIds)
  }

  const [rows] = await pool.query(sql, params)
  return rows
}

export async function loadMcpTools(userId, mcpIds) {
  const configs = await getMcpConfigs(userId, mcpIds)
  const allTools = {}

  for (const config of configs) {
    try {
      const tools = await startMcpProcess(config)
      Object.assign(allTools, tools)
    } catch (err) {
      console.error(`MCP "${config.name}" (id=${config.id}) 加载失败:`, err.message)
    }
  }

  return allTools
}

async function startMcpProcess(config) {
  let args = []
  if (config.args) {
    args = typeof config.args === 'string' ? JSON.parse(config.args) : config.args
  }

  let envVars = {}
  if (config.env) {
    envVars = typeof config.env === 'string' ? JSON.parse(config.env) : config.env
  }

  const transport = new StdioClientTransport({
    command: config.command,
    args,
    env: { ...process.env, ...envVars },
  })

  const client = new Client({ name: 'kris-agent', version: '1.0.0' })
  await client.connect(transport)

  const { tools: mcpTools } = await client.listTools()
  const result = {}

  for (const mcpTool of mcpTools) {
    const toolName = `${config.name}_${mcpTool.name}`
    const paramSchema = mcpTool.inputSchema || {}

    result[toolName] = tool({
      description: mcpTool.description || `来自 MCP "${config.name}" 的工具`,
      parameters: jsonSchemaToZod(paramSchema),
      execute: async (params) => {
        const response = await client.callTool({
          name: mcpTool.name,
          arguments: params,
        })
        const textContent = (response.content || [])
          .filter(c => c.type === 'text')
          .map(c => c.text)
          .join('\n')
        return textContent || JSON.stringify(response.content)
      },
    })
  }

  return result
}

function jsonSchemaToZod(schema) {
  if (!schema || !schema.properties) {
    return z.object({}).passthrough()
  }

  const shape = {}
  const props = schema.properties || {}
  const required = schema.required || []

  for (const [key, prop] of Object.entries(props)) {
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
