'use client'

import { useState } from 'react'
import { Modal } from '@/components/ui/Modal'
import { Button } from '@/components/ui/Button'
import { useToast } from '@/components/providers/ToastProvider'
import { SAMPLE_CREDENTIAL_SUBJECT, SAMPLE_SUBJECT_DID } from '@/lib/shared/samples'
import type { CredentialFormat } from '@/lib/server/clients/credentialsClient'

const FORMATS: { id: CredentialFormat; label: string; hint: string }[] = [
  { id: 'ldp_vc', label: 'ldp_vc', hint: 'JSON-LD, Ed25519Signature2020 (existing path)' },
  { id: 'jwt_vc_json', label: 'jwt_vc_json', hint: 'W3C VC as JWT claims, ES256' },
  { id: 'vc+sd-jwt', label: 'vc+sd-jwt', hint: 'IETF SD-JWT VC, selective disclosure' },
  { id: 'mso_mdoc', label: 'mso_mdoc', hint: 'ISO mdoc format' },
]

export function IssueModal({ onClose, onIssued }: { onClose: () => void; onIssued: () => void }) {
  const [schemaId, setSchemaId] = useState('')
  const [format, setFormat] = useState<CredentialFormat>('ldp_vc')
  const [subject, setSubject] = useState(SAMPLE_SUBJECT_DID)
  const [claims, setClaims] = useState(SAMPLE_CREDENTIAL_SUBJECT)
  const [busy, setBusy] = useState(false)
  const toast = useToast()

  const loadSample = () => {
    setSubject(SAMPLE_SUBJECT_DID)
    setClaims(SAMPLE_CREDENTIAL_SUBJECT)
  }

  const submit = async () => {
    setBusy(true)
    try {
      const credentialSubject = JSON.parse(claims)
      const res = await fetch('/admin/api/credentials/issue', {
        method: 'POST',
        headers: { 'content-type': 'application/json' },
        body: JSON.stringify({ schemaId, format, subject, credentialSubject }),
      })
      if (!res.ok) throw new Error(await res.text())
      toast('Credential issued.', 'success')
      onIssued()
      onClose()
    } catch (e) {
      toast(`Issue failed: ${e instanceof Error ? e.message : String(e)}`, 'error')
    } finally {
      setBusy(false)
    }
  }

  return (
    <Modal
      title="Issue credential"
      subtitle="POST /credentials/issue — unauthenticated today"
      onClose={onClose}
      footer={
        <div className="flex items-center justify-between">
          <button type="button" onClick={loadSample} className="text-xs font-medium text-ink hover:underline">
            Load sample
          </button>
          <div className="flex gap-2">
            <Button variant="outline" onClick={onClose}>
              Cancel
            </Button>
            <Button disabled={busy || !schemaId || !subject} onClick={submit}>
              {busy ? 'Issuing…' : 'Issue'}
            </Button>
          </div>
        </div>
      }
    >
      <div className="flex flex-col gap-4">
        <input
          value={schemaId}
          onChange={(e) => setSchemaId(e.target.value)}
          placeholder="Schema id (must be PUBLISHED)"
          className="h-10 w-full rounded-sm border border-gray-200 px-3 text-sm"
        />
        <div className="grid grid-cols-2 gap-2">
          {FORMATS.map((f) => (
            <button
              key={f.id}
              type="button"
              onClick={() => setFormat(f.id)}
              className={`rounded-sm border p-3 text-left text-sm transition ${
                format === f.id ? 'border-brick bg-brick/5' : 'border-gray-200 hover:bg-gray-50'
              }`}
            >
              <div className="font-mono font-medium text-ink">{f.label}</div>
              <div className="mt-0.5 text-xs text-gray-500">{f.hint}</div>
            </button>
          ))}
        </div>
        <input
          value={subject}
          onChange={(e) => setSubject(e.target.value)}
          placeholder="Subject DID"
          className="h-10 w-full rounded-sm border border-gray-200 px-3 text-sm"
        />
        <textarea
          value={claims}
          onChange={(e) => setClaims(e.target.value)}
          rows={8}
          spellCheck={false}
          className="rounded-sm border border-gray-200 bg-gray-50 p-3 font-mono text-xs"
          placeholder="credentialSubject JSON"
        />
      </div>
    </Modal>
  )
}
