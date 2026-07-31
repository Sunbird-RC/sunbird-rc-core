import { Badge, type BadgeColor } from './Badge'

const COLOR_MAP: Record<string, BadgeColor> = {
  success: 'success',
  warning: 'warning',
  danger: 'danger',
  info: 'info',
  neutral: 'default',
}

// Thin wrapper over Badge's `status` variant (adds the DS's signature
// colored dot) — kept so every screen's `<StatusBadge label color>` call
// site is untouched.
export function StatusBadge({ label, color = 'neutral' }: { label: string; color?: keyof typeof COLOR_MAP }) {
  return (
    <Badge variant="status" color={COLOR_MAP[color] ?? 'default'}>
      {label}
    </Badge>
  )
}
