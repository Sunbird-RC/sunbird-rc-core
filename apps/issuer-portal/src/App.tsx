import { useCallback, useEffect, useState } from 'react'
import { createFarmer, getSession, listIssuers, nextFarmerId, UnauthorizedError } from './api'
import { AboutScreen } from './components/AboutScreen'
import { AppShell, type NavKey } from './components/AppShell'
import { EntityForm } from './components/EntityForm'
import { FarmerDetail } from './components/FarmerDetail'
import { FarmerList } from './components/FarmerList'
import { IssuerGallery } from './components/IssuerGallery'
import { LoginScreen } from './components/LoginScreen'
import { MyDetails } from './components/MyDetails'
import { Alert, Loading, Modal, Toast } from './components/ui'
import { holderFields } from './fields'
import type { Farmer, Issuer, Session } from './types'

type ToastState = { kind: 'ok' | 'err'; message: string; seq: number } | null

export function App() {
  const [session, setSession] = useState<Session>()
  const [sessionError, setSessionError] = useState<string>()
  const [nav, setNav] = useState<NavKey>('issuers')
  /**
   * The issuer being worked in. Undefined means "no issuer chosen" — the gallery
   * or the unscoped holder list. Held here rather than inside a screen because the
   * top bar, the sidebar and every holder screen all read it.
   */
  const [issuer, setIssuer] = useState<Issuer>()
  const [openFarmer, setOpenFarmer] = useState<string>()
  const [creating, setCreating] = useState(false)
  const [newHolderId, setNewHolderId] = useState<string>()
  const [holderCount, setHolderCount] = useState<number>()
  const [listKey, setListKey] = useState(0)
  const [toast, setToast] = useState<ToastState>(null)
  /** All issuers, for the issuer column on the unscoped holder list. */
  const [allIssuers, setAllIssuers] = useState<Issuer[]>()

  useEffect(() => {
    getSession()
      .then(setSession)
      .catch((e: Error) => {
        // A failed session probe is not the same as being signed out: the former
        // means the BFF is unreachable, and telling the user to sign in would
        // send them round a loop that cannot succeed.
        if (e instanceof UnauthorizedError) setSession({ authenticated: false })
        else {
          setSession({ authenticated: false })
          setSessionError(e.message)
        }
      })
  }, [])

  const isStaff = (session?.roles ?? []).includes('issuer-staff')

  // Only staff may list issuers; a citizen's probe would 403 and log noise.
  useEffect(() => {
    if (!isStaff) return
    listIssuers()
      .then((r) => setAllIssuers(r.issuers))
      .catch(() => {
        /* the gallery reports this properly; the column just degrades to an id */
      })
  }, [isStaff, listKey])

  // `seq` forces a re-mount so the same message twice in a row still animates.
  const say = useCallback((kind: 'ok' | 'err', message: string) => {
    setToast({ kind, message, seq: Date.now() })
  }, [])
  const ok = useCallback((m: string) => say('ok', m), [say])
  const err = useCallback((m: string) => say('err', m), [say])

  const invalidateList = useCallback(() => setListKey((n) => n + 1), [])

  /**
   * Opens the new-holder form with the id pre-filled from the issuer's series.
   *
   * The id is fetched BEFORE the modal opens, deliberately: EntityForm seeds its
   * state from `initial` on mount, so an id arriving afterwards would be ignored
   * and the field would sit empty. A failed lookup is not fatal — the field is
   * editable and the registry validates regardless — so the form still opens,
   * just blank.
   */
  const startCreate = useCallback(async () => {
    if (!issuer) return
    let suggested: string | undefined
    try {
      suggested = (await nextFarmerId(issuer.issuerId)).farmerId
    } catch {
      /* leave it blank; staff can type one */
    }
    setNewHolderId(suggested)
    setCreating(true)
  }, [issuer])

  if (!session) return <Loading label="Checking your session…" />
  if (!session.authenticated) return <LoginScreen error={sessionError} />

  const isCitizen = Boolean(session.farmerId)

  // A non-staff account gets a different application, not the staff console with
  // a warning on top. Previously the console rendered regardless of role, so a
  // citizen saw the full holder list — and, because the API was ungated too,
  // could actually read and write other people's records.
  //
  // The server enforces this independently; the branch here only avoids offering
  // actions that would be refused.
  if (!isStaff) {
    return (
      <>
        <AppShell
          session={session}
          nav={isCitizen ? 'me' : 'about'}
          onNav={(k) => setNav(k)}
          citizen={isCitizen}
        >
          {isCitizen ? (
            <MyDetails onError={err} />
          ) : (
            <>
              <div className="page-head">
                <div className="grow">
                  <h1 className="page-title">No access</h1>
                  <p className="page-sub">
                    You're signed in, but this account isn't set up to use this console.
                  </p>
                </div>
              </div>
              <Alert kind="warn">
                Your account has neither the <strong>issuer-staff</strong> role nor a linked holder
                record. If you're staff, ask an administrator for the role. If you're a holder, ask
                the issuing authority to link your account to your record.
              </Alert>
            </>
          )}
        </AppShell>
        {toast && (
          <Toast
            key={toast.seq}
            kind={toast.kind}
            message={toast.message}
            onClose={() => setToast(null)}
          />
        )}
      </>
    )
  }

  const holderNoun = issuer?.holderLabel ?? 'Holder'

  return (
    <>
      <AppShell
        session={session}
        nav={nav}
        onNav={(k) => {
          setNav(k)
          setOpenFarmer(undefined)
          // Leaving for the gallery or the unscoped list drops the issuer
          // context, so the top bar cannot claim an issuer that isn't in play.
          if (k === 'issuers' || k === 'holders') setIssuer(undefined)
        }}
        issuer={nav === 'holders' ? undefined : issuer}
        holderCount={holderCount}
      >
        {nav === 'about' ? (
          <AboutScreen />
        ) : openFarmer ? (
          <FarmerDetail
            farmerId={openFarmer}
            issuer={nav === 'holders' ? undefined : issuer}
            onBack={() => setOpenFarmer(undefined)}
            onToast={ok}
            onError={err}
            onMutated={invalidateList}
          />
        ) : nav === 'holders' ? (
          <FarmerList
            issuers={allIssuers}
            onOpen={setOpenFarmer}
            onNew={startCreate}
            onCount={setHolderCount}
            reloadKey={listKey}
          />
        ) : issuer ? (
          <FarmerList
            issuer={issuer}
            onOpen={setOpenFarmer}
            onNew={startCreate}
            onCount={setHolderCount}
            onLeaveIssuer={() => setIssuer(undefined)}
            reloadKey={listKey}
          />
        ) : (
          <IssuerGallery
            onOpen={(i) => {
              setIssuer(i)
              setOpenFarmer(undefined)
            }}
            onToast={ok}
            onError={err}
          />
        )}
      </AppShell>

      {creating && issuer && (
        <Modal title={`New ${holderNoun.toLowerCase()}`} onClose={() => setCreating(false)}>
          <p className="card-note" style={{ marginTop: 0 }}>
            Added to <strong>{issuer.name}</strong>. The ID is suggested from this issuer's series and
            can be changed.
          </p>
          <EntityForm
            fields={holderFields(issuer.holderLabel)}
            initial={newHolderId ? { farmerId: newHolderId } : {}}
            submitLabel={`Create ${holderNoun.toLowerCase()}`}
            onCancel={() => setCreating(false)}
            onSubmit={async (payload) => {
              const created = await createFarmer({
                ...(payload as unknown as Farmer),
                // Set here, not in the form: which issuer a holder belongs to is
                // the context staff are standing in, not a field to mistype.
                issuerId: issuer.issuerId,
              })
              setCreating(false)
              invalidateList()
              ok(`${holderNoun} ${created.farmerId} created`)
              // Straight into the new record: the next thing staff always do is
              // add a supporting record or link a login, and bouncing back to the
              // list hides both.
              setOpenFarmer(created.farmerId)
            }}
          />
        </Modal>
      )}

      {toast && (
        <Toast
          key={toast.seq}
          kind={toast.kind}
          message={toast.message}
          onClose={() => setToast(null)}
        />
      )}
    </>
  )
}
