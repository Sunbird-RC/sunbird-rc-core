// Minimal JSONPath subset shared by PexService (constraints.fields[].path)
// and Oid4vpService (descriptor_map[].path / path_nested.path resolution).
// Supports dot-segments, ['bracket']/["bracket"] segments (needed for mdoc
// namespace keys that contain dots, e.g. $['org.iso.18013.5.1']['given_name']),
// and numeric [n] array indices.
const TOKEN_RE = /\.([^.[\]]+)|\['([^']*)'\]|\["([^"]*)"\]|\[(\d+)\]/g;

export function parseJsonPath(path: string): Array<string | number> {
  if (typeof path !== 'string' || !path.startsWith('$')) return [];
  const rest = path.slice(1);
  const segments: Array<string | number> = [];
  TOKEN_RE.lastIndex = 0;
  let match: RegExpExecArray | null;
  while ((match = TOKEN_RE.exec(rest))) {
    const [, dotSeg, singleQuoted, doubleQuoted, index] = match;
    segments.push(index !== undefined ? Number(index) : (dotSeg ?? singleQuoted ?? doubleQuoted ?? ''));
  }
  return segments;
}

export function walkSegments(obj: any, segments: Array<string | number>): any {
  let cur = obj;
  for (const seg of segments) {
    if (cur == null) return undefined;
    cur = cur[seg as any];
  }
  return cur;
}

export function resolveJsonPath(obj: any, path: string): any {
  return walkSegments(obj, parseJsonPath(path));
}