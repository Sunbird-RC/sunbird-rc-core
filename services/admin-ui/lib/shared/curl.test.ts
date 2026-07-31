import { describe, expect, it } from 'vitest'
import { buildCurl, DEFAULT_HOST_MAP, parseHostMapOverride, rewriteUrl } from './curl'

describe('rewriteUrl', () => {
  it('rewrites the registry internal DNS name to its non-obvious published port 8091', () => {
    expect(rewriteUrl('http://registry:8081/api/v1/Student')).toBe('http://localhost:8091/api/v1/Student')
  })

  it('rewrites credential-schema (same port internal/external)', () => {
    expect(rewriteUrl('http://credential-schema:3333/credential-schema?tags=education')).toBe(
      'http://localhost:3333/credential-schema?tags=education',
    )
  })

  it('leaves an already-public or unknown URL untouched', () => {
    expect(rewriteUrl('http://localhost:8091/api/v1/Student')).toBe('http://localhost:8091/api/v1/Student')
    expect(rewriteUrl('http://unknown-service:9999/x')).toBe('http://unknown-service:9999/x')
  })
})

describe('parseHostMapOverride', () => {
  it('parses a comma-separated internal=public list', () => {
    expect(parseHostMapOverride('http://a:1=http://localhost:11,http://b:2=http://localhost:22')).toEqual({
      'http://a:1': 'http://localhost:11',
      'http://b:2': 'http://localhost:22',
    })
  })

  it('returns an empty object for undefined/empty input', () => {
    expect(parseHostMapOverride(undefined)).toEqual({})
    expect(parseHostMapOverride('')).toEqual({})
  })

  it('merges over defaults without mutating them', () => {
    const merged = { ...DEFAULT_HOST_MAP, ...parseHostMapOverride('http://registry:8081=http://localhost:9999') }
    expect(merged['http://registry:8081']).toBe('http://localhost:9999')
    expect(DEFAULT_HOST_MAP['http://registry:8081']).toBe('http://localhost:8091')
  })
})

describe('buildCurl', () => {
  it('builds a GET command with the rewritten URL', () => {
    const cmd = buildCurl({ method: 'GET', url: 'http://registry:8081/api/v1/Student/search' })
    expect(cmd).toBe("curl -i -X GET 'http://localhost:8091/api/v1/Student/search'")
  })

  it('adds content-type, a bearer placeholder, and --data-raw when a body is present', () => {
    const cmd = buildCurl({
      method: 'POST',
      url: 'http://credential-schema:3333/credential-schema',
      requestBody: '{"name":"Test"}',
    })
    expect(cmd).toContain("-H 'content-type: application/json'")
    expect(cmd).toContain("-H 'Authorization: Bearer $TOKEN'")
    expect(cmd).toContain("--data-raw '{\"name\":\"Test\"}'")
  })

  it('single-quote escapes an embedded single quote in the body', () => {
    const cmd = buildCurl({ method: 'POST', url: 'http://credential:3000/credentials/issue', requestBody: `{"note":"it's fine"}` })
    expect(cmd).toContain(`'{"note":"it'\\''s fine"}'`)
  })
})
