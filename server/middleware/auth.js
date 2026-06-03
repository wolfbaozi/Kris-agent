import jwt from 'jsonwebtoken'

const JWT_SECRET = process.env.JWT_SECRET || 'kris-agent-jwt-secret'

export function authMiddleware(req, res, next) {
  const header = req.headers.authorization || ''
  const token = header.startsWith('Bearer ') ? header.slice(7) : ''
  if (!token) {
    return res.status(401).json({ error: '未提供认证令牌' })
  }
  try {
    req.user = jwt.verify(token, JWT_SECRET)
    next()
  } catch {
    return res.status(401).json({ error: '令牌无效或已过期' })
  }
}

export function signToken(userId, username) {
  return jwt.sign({ id: userId, username }, JWT_SECRET, { expiresIn: '7d' })
}
