export default function LoginPage({ searchParams }: { searchParams: { error?: string } }) {
  return (
    <div className="flex h-screen items-center justify-center bg-ink">
      <div className="w-full max-w-sm rounded-md bg-ivory p-8 text-center shadow-lg">
        <div className="font-serif text-xl font-medium text-ink">Sunbird RC Admin Console</div>
        <p className="mt-2 text-sm text-gray-500">Sign in with your registry operator account.</p>
        {searchParams.error && (
          <p className="mt-3 rounded-sm bg-danger-bg px-3 py-2 text-xs text-danger">
            Sign-in failed ({searchParams.error}). Try again.
          </p>
        )}
        <a
          href="/admin/api/auth/login"
          className="mt-6 inline-block w-full rounded-sm bg-brick px-4 py-2.5 text-sm font-medium text-white hover:bg-brick/90"
        >
          Continue with Keycloak
        </a>
      </div>
    </div>
  )
}
