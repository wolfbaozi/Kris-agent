import { McpServer } from '@modelcontextprotocol/sdk/server/mcp.js'
import { StdioServerTransport } from '@modelcontextprotocol/sdk/server/stdio.js'
import { z } from 'zod'

const server = new McpServer({
  name: 'calculator-mcp',
  version: '1.0.0',
})

server.tool(
  'add',
  '两个数相加',
  { a: z.number(), b: z.number() },
  async ({ a, b }) => ({
    content: [{ type: 'text', text: String(a + b) }],
  })
)

server.tool(
  'multiply',
  '两个数相乘',
  { x: z.number(), y: z.number() },
  async ({ x, y }) => ({
    content: [{ type: 'text', text: String(x * y) }],
  })
)

server.tool(
  'get_current_time',
  '获取当前系统时间',
  {},
  async () => {
    const now = new Date()
    return {
      content: [{ type: 'text', text: now.toLocaleString('zh-CN') }],
    }
  }
)

const transport = new StdioServerTransport()
await server.connect(transport)
console.error('Calculator MCP Server started')
