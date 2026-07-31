export const STATUS_COLOR: Record<string, 'success' | 'warning' | 'danger' | 'neutral' | 'info'> = {
  DRAFT: 'neutral',
  PUBLISHED: 'success',
  DEPRECATED: 'warning',
  REVOKED: 'danger',
  ISSUED: 'success',
  OPEN: 'info',
  GRANTED: 'success',
  DENIED: 'danger',
}
