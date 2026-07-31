import { Header } from '@/components/layout/Header'
import { ScreenBody } from '@/components/layout/ScreenBody'
import { CredentialList } from '@/components/credentials/List'

export default function CredentialsPage() {
  return (
    <>
      <Header
        title="Credentials"
        subtitle="Search, issue, verify and revoke credentials"
        endpoint="POST /credentials/search"
      />
      <ScreenBody>
        <CredentialList />
      </ScreenBody>
    </>
  )
}
