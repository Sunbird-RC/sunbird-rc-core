import { NextResponse } from 'next/server'
import { getOid4vciConfigs } from '@/lib/server/clients/credentialSchemaClient'
import { ApiError } from '@/lib/server/http'

export async function GET() {
  try {
    return NextResponse.json(await getOid4vciConfigs())
  } catch (e) {
    if (e instanceof ApiError) return NextResponse.json({ message: e.message }, { status: e.status })
    throw e
  }
}
