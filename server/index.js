import cors from 'cors'
import { config } from 'dotenv'
import { fileURLToPath } from 'url'
import { resolve, dirname } from 'path'
import express from 'express'
import authRoutes from './routes/auth.js'
import apikeyRoutes from './routes/apikeys.js'
import chatRoutes from './routes/chat.js'

const __dirname = dirname(fileURLToPath(import.meta.url))
config({ path: resolve(__dirname, '.env') })

const app = express()
const PORT = process.env.PORT || 3001

app.use(cors())
app.use(express.json())

app.use('/api/auth', authRoutes)
app.use('/api/apikeys', apikeyRoutes)
app.use('/api/chat', chatRoutes)

app.get('/api/health', (_req, res) => {
  res.json({ ok: true })
})

app.listen(PORT, () => {
  console.log(`Agent BFF running at http://localhost:${PORT}`)
})
