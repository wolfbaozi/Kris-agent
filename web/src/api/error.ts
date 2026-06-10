type ErrorListener = (message: string) => void

const listeners: Set<ErrorListener> = new Set()

export function emitError(message: string) {
  console.error('[API Error]', message)
  listeners.forEach(listener => listener(message))
}

export function onError(listener: ErrorListener): () => void {
  listeners.add(listener)
  return () => listeners.delete(listener)
}
