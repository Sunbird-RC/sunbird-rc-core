import type { Metadata } from 'next'
import { Rubik } from 'next/font/google'
import { getFlags } from '@/lib/server/flags'
import { getSession } from '@/lib/server/session'
import { FlagsProvider } from '@/components/providers/FlagsProvider'
import { ToastProvider } from '@/components/providers/ToastProvider'
import { DrawerProvider } from '@/components/providers/DrawerProvider'
import { SessionProvider } from '@/components/providers/SessionProvider'
import './globals.css'

// Brand font — weight 500 is the workhorse for titles/buttons per the DS spec.
const rubik = Rubik({ subsets: ['latin'], weight: ['300', '400', '500', '700'], variable: '--font-rubik' })

export const metadata: Metadata = {
  title: 'Sunbird RC Admin Console',
}

export default async function RootLayout({ children }: { children: React.ReactNode }) {
  const [flags, session] = await Promise.all([getFlags(), getSession()])

  return (
    <html lang="en" className={rubik.variable}>
      <body>
        <SessionProvider session={session ? { name: session.name, email: session.email } : null}>
          <FlagsProvider flags={flags}>
            <ToastProvider>
              <DrawerProvider>{children}</DrawerProvider>
            </ToastProvider>
          </FlagsProvider>
        </SessionProvider>
      </body>
    </html>
  )
}
