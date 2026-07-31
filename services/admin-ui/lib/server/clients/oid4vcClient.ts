import 'server-only'
import { env } from '../env'
import { json } from '../http'

const base = () => env.oid4vcBaseUrl

export type OfferRequest = {
  credential_configuration_id: string
  subject?: string
  expires_in?: number
  batch_size?: number
  tx_code_required?: boolean
}

export type OfferResponse = {
  credential_offer_uri: string
  qr_data: string
  tx_code?: string
}

// One of only two guarded routes on oid4vc-service — the correct admin
// boundary. Everything else there (token, credential, nonce, deferred,
// notification) is @Public() and wallet-driven.
export async function createOffer(req: OfferRequest): Promise<OfferResponse> {
  return json(`${base()}/oid4vc/offer`, {
    method: 'POST',
    headers: { 'content-type': 'application/json' },
    body: JSON.stringify(req),
  })
}
