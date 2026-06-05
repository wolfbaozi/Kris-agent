import pool from './db.js'

const userId = 4 // 假设 Kris 的 user_id = 1

async function seed() {
  // 插入 Calculator MCP
  await pool.query(
    `INSERT INTO mcp_servers (user_id, name, is_global, run_env, command, args, enabled)
     VALUES (?, ?, 1, 'local', 'node', ?, 1)`,
    [userId, 'Calculator', JSON.stringify(['./mcp-examples/calculator-mcp.js'])]
  )
  console.log('Calculator MCP seeded.')

  // 插入一个 Prompt Skill 示例
  await pool.query(
    `INSERT INTO skills (user_id, name, is_global, skill_type, prompt_content, enabled)
     VALUES (?, '代码审查助手', 1, 'prompt', ?, 1)`,
    [userId, '你是一名经验丰富的代码审查专家，当用户询问代码相关问题时：\n1. 先判断代码的正确性\n2. 指出潜在的性能问题和安全隐患\n3. 给出简洁的改进建议\n4. 用中文回复']
  )
  console.log('Code Review Skill seeded.')

  await pool.end()
  console.log('Seeding complete.')
}

seed().catch(e => {
  console.error('Seeding failed:', e.message)
  process.exit(1)
})
