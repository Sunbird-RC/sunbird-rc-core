import type { LucideIcon } from 'lucide-react'

export type NavItem = {
  id: string
  label: string
  href: string
  icon: LucideIcon
  disabledReason?: string
}

export type NavSection = {
  title: string
  items: NavItem[]
}
