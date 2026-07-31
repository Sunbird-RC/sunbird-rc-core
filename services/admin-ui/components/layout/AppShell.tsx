import { Sidebar } from './Sidebar'

// Each page renders its own <Header> (title/subtitle/endpoint differ per
// screen) as the first thing inside children — the shell only owns the
// sidebar/drawer chrome that's identical across all 12 screens.
export function AppShell({ children }: { children: React.ReactNode }) {
  return (
    <div className="flex h-screen overflow-hidden bg-cream font-sans text-sm text-charcoal">
      <Sidebar />
      <div className="flex min-w-0 flex-1 flex-col overflow-hidden">{children}</div>
    </div>
  )
}
