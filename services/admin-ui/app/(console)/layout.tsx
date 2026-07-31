import { AppShell } from '@/components/layout/AppShell'

// Each page under (console)/** renders its own <Header> followed by
// <ScreenBody> — see components/layout/ScreenBody.tsx.
export default function ConsoleLayout({ children }: { children: React.ReactNode }) {
  return <AppShell>{children}</AppShell>
}
