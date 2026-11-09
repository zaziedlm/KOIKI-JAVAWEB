import type {NextConfig} from 'next';
const nextConfig:NextConfig={output:'standalone',poweredByHeader:false,experimental:{cpus:1},
  async headers(){return [{source:'/a/:path*',headers:[{key:'Cache-Control',value:'private, no-store, max-age=0'},{key:'Pragma',value:'no-cache'}]}];}};
export default nextConfig;
