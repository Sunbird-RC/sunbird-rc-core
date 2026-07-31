/** @type {import('next').NextConfig} */
const nextConfig = {
  // Mounted at /admin/ behind the main nginx gateway (see nginx/nginx.conf).
  basePath: '/admin',
  output: 'standalone',
  reactStrictMode: true,
}

export default nextConfig
