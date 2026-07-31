export type BadgeVariant = 'tint' | 'solid' | 'outline' | 'status'
export type BadgeColor = 'default' | 'success' | 'warning' | 'danger' | 'info'

const COLOR_CLASSES: Record<BadgeColor, { bg: string; fg: string; bd: string }> = {
  default: { bg: 'bg-tint-93', fg: 'text-obsidian', bd: 'border-tint-border' },
  success: { bg: 'bg-success-bg', fg: 'text-[#2f5e3a]', bd: 'border-forest' },
  warning: { bg: 'bg-warning-bg', fg: 'text-warning-text', bd: 'border-sunflower' },
  danger: { bg: 'bg-danger-bg', fg: 'text-danger', bd: 'border-danger' },
  info: { bg: 'bg-[#eef5f7]', fg: 'text-ink', bd: 'border-wave' },
}

// tint/solid/outline/status per the DS Badge spec. `status` renders a
// 0.4rem dot colored as the border color before the label — the design's
// "● ACTIVE" pill.
export function Badge({
  variant = 'tint',
  color = 'default',
  children,
}: {
  variant?: BadgeVariant
  color?: BadgeColor
  children: React.ReactNode
}) {
  const c = COLOR_CLASSES[color]

  const variantClass =
    variant === 'solid'
      ? 'bg-brick text-white border border-transparent'
      : variant === 'outline'
        ? `bg-transparent ${c.fg} border ${c.bd}`
        : `${c.bg} ${c.fg} border ${c.bd}`

  return (
    <span
      className={`inline-flex items-center gap-1.5 whitespace-nowrap rounded-full px-3 py-1 text-sm font-medium leading-tight ${variantClass}`}
    >
      {variant === 'status' && <span className={`h-[0.4rem] w-[0.4rem] rounded-full ${c.bd.replace('border-', 'bg-')}`} />}
      {children}
    </span>
  )
}
