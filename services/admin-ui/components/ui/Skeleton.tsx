export function SkeletonRows({ count = 5 }: { count?: number }) {
  return (
    <div className="overflow-hidden rounded-md bg-white py-2 shadow-md">
      {Array.from({ length: count }).map((_, i) => (
        <div key={i} className="flex items-center gap-4 border-b border-gray-50 px-5 py-4 last:border-0">
          <div className="shimmer h-3 w-1/4 rounded-full" />
          <div className="shimmer h-3 w-1/6 rounded-full" />
          <div className="shimmer ml-auto h-3 w-16 rounded-full" />
        </div>
      ))}
    </div>
  )
}
