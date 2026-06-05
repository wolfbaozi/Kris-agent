import cors from 'cors'
import { config } from 'dotenv'
import { fileURLToPath } from 'url'
import { resolve, dirname } from 'path'
import express from 'express'
import { readdirSync } from 'fs'

const __dirname = dirname(fileURLToPath(import.meta.url))
config({ path: resolve(__dirname, '.env') })

const app = express()
const PORT = process.env.PORT || 3001

app.use(cors())
app.use(express.json())

const routesDir = resolve(__dirname, 'routes')
const routeFiles = readdirSync(routesDir).filter(f => f.endsWith('.js'))

const routeMap = {
  auth:   '/api/auth',
  apikeys:'/api/apikeys',
  chat:   '/api/chat',
  mcps:   '/api/mcps',
  skills: '/api/skills',
}

for (const file of routeFiles) {
  const name = file.replace('.js', '')
  const prefix = routeMap[name]
  if (!prefix) continue
  try {
    const mod = await import(`file://${resolve(routesDir, file)}`)
    const handler = mod.default || mod
    app.use(prefix, handler)
    console.log(`Route /api/${name} registered`)
  } catch (err) {
    console.error(`Failed to load route ${file}:`, err.message)
  }
}

app.get('/api/health', (_req, res) => {
  res.json({ ok: true })
})

app.listen(PORT, () => {
  console.log(`Agent BFF running at http://localhost:${PORT}`)
})
