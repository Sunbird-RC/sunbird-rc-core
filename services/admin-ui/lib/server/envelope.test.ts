import { describe, expect, it } from 'vitest'
import { parseResponseEnvelope, parseSearchEnvelope } from './envelope'

describe('parseSearchEnvelope', () => {
  it('reads {totalCount, data} — the real live shape (GET /api/v1/Schema returns this even when empty)', () => {
    const res = { totalCount: 2, data: [{ a: 1 }, { a: 2 }] }
    expect(parseSearchEnvelope(res)).toEqual({ items: [{ a: 1 }, { a: 2 }], totalCount: 2 })
  })

  it('handles the empty case exactly as observed live: {"totalCount":0,"data":[]}', () => {
    expect(parseSearchEnvelope({ totalCount: 0, data: [] })).toEqual({ items: [], totalCount: 0 })
  })

  it('falls back to a bare array (some saved Postman examples show this shape)', () => {
    expect(parseSearchEnvelope([{ a: 1 }])).toEqual({ items: [{ a: 1 }], totalCount: 1 })
  })

  it('never reads a key named after the entity type — that was the original bug', () => {
    const res = { totalCount: 1, data: [{ osid: '1' }], Student: 'this key must never be read' }
    expect(parseSearchEnvelope(res).items).toEqual([{ osid: '1' }])
  })
})

describe('parseResponseEnvelope', () => {
  it('reads `result` from the generic Response envelope', () => {
    const res = {
      id: 'sunbird-rc.registry.post',
      params: { status: 'SUCCESSFUL' },
      responseCode: 'OK',
      result: { Student: { osid: '1-abc' } },
    }
    expect(parseResponseEnvelope(res, {})).toEqual({ Student: { osid: '1-abc' } })
  })

  it('returns the fallback when result is absent (e.g. PUT/DELETE responses)', () => {
    const res = { id: 'x', params: { status: 'SUCCESSFUL' }, responseCode: 'OK' }
    expect(parseResponseEnvelope(res, [])).toEqual([])
  })

  it('passes through a bare array unchanged', () => {
    expect(parseResponseEnvelope([{ a: 1 }], [])).toEqual([{ a: 1 }])
  })
})
