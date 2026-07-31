export function getPath(obj: Record<string, unknown>, path: string): unknown {
  return path.split('.').reduce<unknown>((acc, key) => (acc as Record<string, unknown> | undefined)?.[key], obj)
}

export function setPath(obj: Record<string, unknown>, path: string, value: unknown): Record<string, unknown> {
  const keys = path.split('.')
  const root: Record<string, unknown> = { ...obj }
  let cursor = root
  keys.forEach((key, i) => {
    if (i === keys.length - 1) {
      cursor[key] = value
    } else {
      cursor[key] = { ...(cursor[key] as Record<string, unknown> | undefined) }
      cursor = cursor[key] as Record<string, unknown>
    }
  })
  return root
}
