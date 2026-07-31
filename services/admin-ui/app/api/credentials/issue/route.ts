import { NextRequest, NextResponse } from 'next/server'
import { issueCredential } from '@/lib/server/clients/credentialsClient'
import { ApiError } from '@/lib/server/http'

export async function POST(req: NextRequest) {
  const body = await req.json()
  try {
    return NextResponse.json(await issueCredential(body))
  } catch (e) {
    if (e instanceof ApiError) return NextResponse.json({ message: e.message }, { status: e.status })
    throw e
  }
}
