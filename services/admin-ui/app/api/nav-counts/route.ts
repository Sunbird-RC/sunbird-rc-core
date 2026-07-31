import { NextResponse } from 'next/server'
import { getNavCounts } from '@/lib/server/navCounts'

export async function GET() {
  return NextResponse.json(await getNavCounts())
}
