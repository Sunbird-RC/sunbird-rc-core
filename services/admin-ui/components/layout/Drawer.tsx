'use client'

import { ChevronLeft, ChevronRight, X } from 'lucide-react'
import { useDrawer } from '@/components/providers/DrawerProvider'
import { InspectorPanel } from '@/components/inspector/InspectorPanel'

export function Drawer() {
  const { drawer, isOpen, toggle, close } = useDrawer()

  return (
    <aside
      className={`flex flex-none flex-col overflow-y-auto border-l border-gray-100 bg-ivory transition-all ${
        isOpen ? 'w-[340px]' : 'w-[40px]'
      }`}
    >
      <div className="sticky top-0 z-10 flex items-center gap-2 border-b border-gray-100 bg-ivory px-3 py-3.5">
        <button type="button" onClick={toggle} className="flex-none text-gray-400 hover:text-brick" title="Toggle">
          {isOpen ? <ChevronRight size={16} /> : <ChevronLeft size={16} />}
        </button>
        {isOpen && (
          <div className="flex min-w-0 flex-1 items-center gap-2">
            <div className="min-w-0 flex-1 truncate text-xs font-medium uppercase tracking-wide text-gray-500">
              {drawer ? drawer.label : 'Request inspector'}
            </div>
            {drawer && (
              <button type="button" onClick={close} className="text-gray-400 hover:text-ink">
                <X size={15} />
              </button>
            )}
          </div>
        )}
      </div>

      {isOpen && drawer && <div className="animate-[riseIn_0.2s_ease-out] p-5">{drawer.content}</div>}

      {/* Default panel when nothing is selected — the design's Request
          Inspector, not a static "nothing selected" message. */}
      {isOpen && !drawer && <InspectorPanel />}
    </aside>
  )
}
