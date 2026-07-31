'use client'

import { useFlags } from '@/components/providers/FlagsProvider'
import { useSession } from '@/components/providers/SessionProvider'
import { useState } from 'react'
import { AlertTriangle, X } from 'lucide-react'

export function Header({ title, subtitle, endpoint }: { title: string; subtitle?: string; endpoint?: string }) {
  const flags = useFlags()
  const session = useSession()
  const [bannerOpen, setBannerOpen] = useState(!flags.authentication)

  return (
    <div className="flex-none">
      <header className="flex items-center gap-4 border-b border-gray-100 bg-ivory px-6 py-4">
        <div className="min-w-0">
          <div className="font-serif text-xl font-medium leading-tight text-ink">{title}</div>
          {subtitle && <div className="mt-0.5 text-sm text-gray-500">{subtitle}</div>}
        </div>
        <div className="ml-auto flex items-center gap-3.5">
          {endpoint && (
            <code className="rounded-full border border-wave bg-[#eef5f7] px-2.5 py-1 font-mono text-xs text-ink">
              {endpoint}
            </code>
          )}
          <div className="flex items-center gap-3 border-l border-gray-100 pl-3.5">
            <div className="text-right leading-tight">
              <div className="text-sm font-medium text-ink">{session?.name ?? 'Operator'}</div>
              <div className="text-xs text-gray-500">Registry operator</div>
            </div>
            {session && (
              <a href="/admin/api/auth/logout" className="text-xs text-gray-400 hover:text-brick">
                Sign out
              </a>
            )}
          </div>
        </div>
      </header>

      {bannerOpen && (
        <div className="flex items-start gap-3 border-b border-warning bg-warning-bg px-6 py-3">
          <AlertTriangle size={18} className="mt-0.5 flex-none text-warning-text" />
          <div className="flex-1 text-sm leading-snug text-warning-text">
            <strong className="font-medium">This console is unauthenticated.</strong> credentials-service has no
            auth guard at all, and the other three services verify the JWT signature only. Keep <code>/admin/</code>{' '}
            on a trusted network until Phase 1 auth lands.
          </div>
          <button
            type="button"
            onClick={() => setBannerOpen(false)}
            className="text-warning-text/60 hover:text-warning-text"
          >
            <X size={16} />
          </button>
        </div>
      )}
    </div>
  )
}
