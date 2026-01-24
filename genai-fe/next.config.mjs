/** @type {import('next').NextConfig} */
const nextConfig = {
  reactStrictMode: true,
  async rewrites() {
    return [
      {
        source: '/api/:path*',
        destination: 'http://localhost:8080/api/:path*', // Backend API
      },
      {
        source: '/documents/:path*',
        destination: 'http://localhost:8080/documents/:path*', // Backend Documents
      },
    ];
  },
};

export default nextConfig;