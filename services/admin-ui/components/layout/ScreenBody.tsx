import { Drawer } from './Drawer'

// Wraps the per-screen <Header> + main content + shared Drawer. Used by
// every (console)/**/page.tsx so the main/drawer split is written once.
export function ScreenBody({ children }: { children: React.ReactNode }) {
  return (
    <div className="flex min-w-0 flex-1 overflow-hidden">
      <main className="flex-1 overflow-y-auto px-6 py-5">{children}</main>
      <Drawer />
    </div>
  )
}
