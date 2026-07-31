'use client'

import Link from 'next/link'
import { usePathname } from 'next/navigation'
import { useEffect, useState } from 'react'
import { useFlags } from '@/components/providers/FlagsProvider'
import { buildNavSections } from '@/lib/shared/navSections'
import { Logo } from './Logo'

export function Sidebar() {
  const flags = useFlags()
  const pathname = usePathname()
  const sections = buildNavSections(flags)
  const [counts, setCounts] = useState<Record<string, number | null>>({})

  // Fetched client-side after mount, never in the root layout — the
  // sidebar renders on every page, so this must not block the shell.
  useEffect(() => {
    const timer = setTimeout(() => {
      fetch('/admin/api/nav-counts')
        .then((r) => r.json())
        .then(setCounts)
        .catch(() => {})
    }, 200)
    return () => clearTimeout(timer)
  }, [pathname])

  return (
    <aside className="flex w-[250px] flex-none flex-col overflow-hidden bg-ink text-ivory">
      <div className="flex flex-col gap-2.5 px-5 pb-4 pt-5">
        <Logo className="h-[26px] w-auto text-ivory" />
        <div className="flex items-center gap-1.5 text-xs uppercase tracking-wide text-wave">
          <span>Registry admin</span>
          <span className="rounded-full bg-white/10 px-1.5 py-0.5 normal-case tracking-normal">v1</span>
        </div>
      </div>

      <nav className="flex flex-1 flex-col gap-0.5 overflow-y-auto px-3 py-1.5">
        {sections.map((section) => (
          <div key={section.title}>
            <div className="px-2 pb-1.5 pt-3 text-[0.6875rem] font-medium uppercase tracking-wide text-white/45">
              {section.title}
            </div>
            {section.items.map((item) => {
              const active = pathname === item.href || pathname?.startsWith(`${item.href}/`)
              const disabled = Boolean(item.disabledReason)
              const Icon = item.icon
              const linkClasses = `flex items-center gap-2.5 rounded-sm px-2.5 py-2 text-sm font-medium transition ${
                disabled
                  ? 'cursor-not-allowed text-white/35'
                  : active
                    ? 'bg-brick text-white'
                    : 'text-ivory/90 hover:bg-white/10'
              }`
              const count = counts[item.id]
              const countBadge =
                count !== undefined && count !== null ? (
                  item.id === 'claims' && count > 0 ? (
                    <span className="ml-auto rounded-full bg-brick px-1.5 text-xs font-bold text-white">{count}</span>
                  ) : (
                    <span className="ml-auto text-xs opacity-60">{count}</span>
                  )
                ) : null
              return disabled ? (
                <div key={item.id} className={linkClasses} title={item.disabledReason}>
                  <Icon size={17} strokeWidth={2} className="flex-none" />
                  <span>{item.label}</span>
                  {countBadge}
                </div>
              ) : (
                <Link key={item.id} href={item.href} className={linkClasses}>
                  <Icon size={17} strokeWidth={2} className="flex-none" />
                  <span>{item.label}</span>
                  {countBadge}
                </Link>
              )
            })}
          </div>
        ))}
      </nav>

      <div className="flex flex-col gap-2 border-t border-white/10 px-5 py-3.5 text-xs text-white/70">
        <div className="flex items-center gap-2">
          <span className="h-1.5 w-1.5 rounded-full bg-moss" />
          <span>nginx gateway · same-origin</span>
        </div>
        <div className="flex flex-wrap gap-1.5">
          <code className="rounded-sm bg-white/10 px-1.5 py-0.5 text-[0.625rem]">
            claims: {flags.claims ? 'on' : 'off'}
          </code>
          <code className="rounded-sm bg-white/10 px-1.5 py-0.5 text-[0.625rem]">
            files: {flags.filestorage ? 'on' : 'off'}
          </code>
        </div>
      </div>
    </aside>
  )
}
