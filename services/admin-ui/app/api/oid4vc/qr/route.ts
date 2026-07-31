import { NextRequest, NextResponse } from 'next/server'
import QRCode from 'qrcode'

// No client-side QR dependency needed in the browser bundle — rendered
// server-side, same idea as oid4vc-service's own /qr renderer.
export async function GET(req: NextRequest) {
  const data = req.nextUrl.searchParams.get('data')
  if (!data) return NextResponse.json({ message: 'data is required' }, { status: 400 })
  const png = await QRCode.toBuffer(data, { type: 'png', width: 220 })
  return new NextResponse(new Uint8Array(png), { headers: { 'content-type': 'image/png' } })
}
