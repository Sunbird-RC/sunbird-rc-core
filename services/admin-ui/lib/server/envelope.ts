// The registry uses two distinct response shapes, verified against
// RegistryEntityController.java and live responses (curl http://localhost:8091/…):
//
// 1. Search/list results — built deep inside the search service, the
//    controller only appends prevPage/nextPage:
//      { totalCount: number, data: T[], prevPage?: string, nextPage?: string }
//    Constants.java: TOTAL_COUNT="totalCount", ENTITY_LIST="data".
//
// 2. The generic `Response` envelope — used by create/update/delete/
//    policies/invite/audit:
//      { id, ver, ets, params: {status, errmsg}, responseCode, result }
//
// Getting these two confused (e.g. reading `res.Schema` or `res[entityType]`
// on a search result) is exactly why screens rendered empty even when the
// registry had real data — the parse just looked at a key that was never
// there.

export type SearchEnvelope<T> = {
  totalCount: number
  data: T[]
  prevPage?: string
  nextPage?: string
}

export function parseSearchEnvelope<T>(res: unknown): { items: T[]; totalCount: number } {
  if (Array.isArray(res)) return { items: res as T[], totalCount: res.length }
  const envelope = res as Partial<SearchEnvelope<T>> | undefined
  const items = envelope?.data ?? []
  return { items, totalCount: envelope?.totalCount ?? items.length }
}

export type ResponseEnvelope<T> = {
  id?: string
  ver?: string
  ets?: number
  params?: { status?: string; errmsg?: string }
  responseCode?: string
  result?: T
}

export function parseResponseEnvelope<T>(res: unknown, fallback: T): T {
  if (Array.isArray(res)) return res as unknown as T
  const envelope = res as Partial<ResponseEnvelope<T>> | undefined
  return envelope?.result ?? fallback
}
