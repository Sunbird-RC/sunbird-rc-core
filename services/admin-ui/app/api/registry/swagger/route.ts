import { NextResponse } from 'next/server'
import { fetchSwagger } from '@/lib/server/swagger/fetchSwagger'
import { parseEntitySchema } from '@/lib/server/swagger/parseEntitySchema'

export async function GET() {
  try {
    const doc = await fetchSwagger()
    return NextResponse.json(parseEntitySchema(doc))
  } catch (e) {
    return NextResponse.json({ message: e instanceof Error ? e.message : String(e) }, { status: 502 })
  }
}
