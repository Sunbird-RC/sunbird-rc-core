import { parseJsonPath, walkSegments, resolveJsonPath } from './jsonpath.util';

describe('parseJsonPath', () => {
  it('parses dot-segments', () => {
    expect(parseJsonPath('$.a.b.c')).toEqual(['a', 'b', 'c']);
  });

  it('parses a single-quoted bracket segment', () => {
    expect(parseJsonPath("$['org.iso.18013.5.1']")).toEqual(['org.iso.18013.5.1']);
  });

  it('parses a double-quoted bracket segment', () => {
    expect(parseJsonPath('$["org.iso.18013.5.1"]')).toEqual(['org.iso.18013.5.1']);
  });

  it('parses a numeric array index as a number', () => {
    expect(parseJsonPath('$[0]')).toEqual([0]);
  });

  it('mixes dot, bracket, and numeric-index segments', () => {
    expect(parseJsonPath("$.vp.verifiableCredential[0]['org.iso.18013.5.1']['given_name']")).toEqual([
      'vp',
      'verifiableCredential',
      0,
      'org.iso.18013.5.1',
      'given_name',
    ]);
  });

  it('returns an empty array for a path not starting with $', () => {
    expect(parseJsonPath('a.b.c')).toEqual([]);
  });

  it('returns an empty array for a non-string input', () => {
    expect(parseJsonPath(undefined as any)).toEqual([]);
    expect(parseJsonPath(null as any)).toEqual([]);
    expect(parseJsonPath(42 as any)).toEqual([]);
  });

  it('returns an empty array for the bare root path', () => {
    expect(parseJsonPath('$')).toEqual([]);
  });
});

describe('walkSegments', () => {
  it('walks nested dot and array segments', () => {
    const obj = { a: { b: [{ c: 'found' }] } };
    expect(walkSegments(obj, ['a', 'b', 0, 'c'])).toBe('found');
  });

  it('returns undefined when a segment does not exist', () => {
    const obj = { a: {} };
    expect(walkSegments(obj, ['a', 'b', 'c'])).toBeUndefined();
  });

  it('short-circuits to undefined when an intermediate value is null', () => {
    const obj = { a: null };
    expect(walkSegments(obj, ['a', 'b'])).toBeUndefined();
  });

  it('returns the object itself for an empty segment list', () => {
    const obj = { a: 1 };
    expect(walkSegments(obj, [])).toBe(obj);
  });

  it('handles a bracket key containing dots', () => {
    const obj = { 'org.iso.18013.5.1': { given_name: 'Alice' } };
    expect(walkSegments(obj, ['org.iso.18013.5.1', 'given_name'])).toBe('Alice');
  });
});

describe('resolveJsonPath', () => {
  it('resolves a full dot-path against a nested object', () => {
    const obj = { credentialSubject: { name: 'Alice' } };
    expect(resolveJsonPath(obj, '$.credentialSubject.name')).toBe('Alice');
  });

  it('resolves an mdoc bracket-notation namespace path', () => {
    const obj = { 'org.iso.18013.5.1': { given_name: 'Alice' } };
    expect(resolveJsonPath(obj, "$['org.iso.18013.5.1']['given_name']")).toBe('Alice');
  });

  it('resolves a top-level array index', () => {
    const obj = { items: ['first', 'second'] };
    expect(resolveJsonPath(obj, '$.items[1]')).toBe('second');
  });

  it('returns undefined when the path does not resolve', () => {
    const obj = { a: 1 };
    expect(resolveJsonPath(obj, '$.b.c')).toBeUndefined();
  });
});