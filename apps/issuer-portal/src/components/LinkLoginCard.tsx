import { useEffect, useState } from 'react'
import { linkLogin, listKeycloakUsers, unlinkLogin } from '../api'
import type { Farmer } from '../types'
import { Alert, Badge, Field, Spinner } from './ui'

/**
 * Links a farmer record to a Keycloak login.
 *
 * This is the hinge of the whole design, so it gets its own card rather than
 * being a field on the farmer form: linking writes the `farmerId` attribute onto
 * the Keycloak user, and that attribute is what the wallet's access token
 * carries. Without the link a citizen can sign into their wallet and still have
 * nothing to be issued — which looks like a bug and isn't one.
 */
export function LinkLoginCard({
  farmer,
  holderLabel = 'Holder',
  onChanged,
  onError,
}: {
  farmer: Farmer
  /** The issuer's own noun for the person, so the copy fits the issuer. */
  holderLabel?: string
  onChanged: (message: string) => void
  onError: (message: string) => void
}) {
  const holder = holderLabel.toLowerCase()
  const [username, setUsername] = useState('')
  const [busy, setBusy] = useState(false)
  const [known, setKnown] = useState<string[]>([])
  // Set when a link attempt found no such account, so the UI can offer to make
  // one instead of leaving staff at a dead end.
  const [missing, setMissing] = useState<string>()
  const [password, setPassword] = useState('')
  const linked = Boolean(farmer.keycloakUsername)

  useEffect(() => {
    if (linked) return
    listKeycloakUsers()
      .then((r) => setKnown(r.users))
      .catch(() => {
        /* suggestions are a convenience; typing still works */
      })
  }, [linked])

  async function link(create = false) {
    const name = username.trim()
    if (!name) return
    if (create && !password) {
      onError('Set a temporary password for the new login')
      return
    }
    setBusy(true)
    try {
      await linkLogin(farmer.farmerId, name, create ? { password } : undefined)
      setUsername('')
      setPassword('')
      setMissing(undefined)
      onChanged(create ? `Created and linked ${name}` : `Linked to ${name}`)
    } catch (e) {
      const msg = (e as Error).message
      // A 404 here is not a failure so much as a fork in the road.
      if (/No Keycloak user named/i.test(msg)) setMissing(name)
      else onError(msg)
    } finally {
      setBusy(false)
    }
  }

  async function unlink() {
    setBusy(true)
    try {
      await unlinkLogin(farmer.farmerId)
      onChanged('Login unlinked')
    } catch (e) {
      onError((e as Error).message)
    } finally {
      setBusy(false)
    }
  }

  return (
    <div className="card">
      <div className="card-head">
        <div className="grow">
          <h2 className="card-title">
            <span aria-hidden style={{ marginRight: 7 }}>
              🔑
            </span>
            Wallet login
          </h2>
          <p className="card-note">
            Lets this {holder} add their own credential from a wallet, with no staff involvement.
          </p>
        </div>
        {linked ? (
          <Badge kind="ok" dot>
            Linked
          </Badge>
        ) : (
          <Badge kind="neutral">Not linked</Badge>
        )}
      </div>

      <div className="card-body">
        {linked ? (
          <>
            <Alert kind="ok">
              Linked to <strong>{farmer.keycloakUsername}</strong>. When they sign into a wallet with
              that account, they'll be issued <strong>only</strong> this {holder}'s credential.
            </Alert>
            <div className="form-actions">
              <button className="btn-ghost btn-danger" onClick={unlink} disabled={busy}>
                {busy ? 'Working…' : 'Unlink login'}
              </button>
            </div>
          </>
        ) : (
          <>
            <Alert kind="info">
              Until a login is linked, this {holder}'s credential can only be issued by staff, as a
              QR code and PIN.
            </Alert>
            <div className="grid2">
              <Field
                label="Keycloak username"
                help={`The account the ${holder} signs into their wallet with.`}
              >
                <input
                  value={username}
                  disabled={busy}
                  placeholder="farmer.ravi"
                  list="kc-usernames"
                  onChange={(e) => {
                    setUsername(e.target.value)
                    setMissing(undefined)
                  }}
                  onKeyDown={(e) => {
                    if (e.key === 'Enter') {
                      e.preventDefault()
                      void link()
                    }
                  }}
                />
                <datalist id="kc-usernames">
                  {known.map((u) => (
                    <option key={u} value={u} />
                  ))}
                </datalist>
              </Field>
              <div className="field" style={{ justifyContent: 'flex-end' }}>
                <button className="btn" onClick={() => link()} disabled={busy || !username.trim()}>
                  {busy ? (
                    <>
                      <Spinner /> Linking…
                    </>
                  ) : (
                    'Link login'
                  )}
                </button>
              </div>
            </div>

            {missing && (
              <div style={{ marginTop: 16 }}>
                <Alert kind="warn">
                  There's no Keycloak account called <strong>{missing}</strong> yet. Create it here
                  and the {holder} can sign into their wallet with it straight away.
                </Alert>
                <div className="grid2">
                  <Field
                    label="Temporary password"
                    help={`The ${holder} is asked to change it on first sign-in.`}
                  >
                    <input
                      type="text"
                      value={password}
                      disabled={busy}
                      placeholder="e.g. Welcome123!"
                      onChange={(e) => setPassword(e.target.value)}
                    />
                  </Field>
                  <div className="field" style={{ justifyContent: 'flex-end' }}>
                    <button className="btn" onClick={() => link(true)} disabled={busy || !password}>
                      {busy ? (
                        <>
                          <Spinner /> Creating…
                        </>
                      ) : (
                        `Create ${missing} and link`
                      )}
                    </button>
                  </div>
                </div>
              </div>
            )}
          </>
        )}
      </div>
    </div>
  )
}
