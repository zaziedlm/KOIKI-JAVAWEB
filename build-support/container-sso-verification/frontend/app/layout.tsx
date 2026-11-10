export default function Layout({children}:{children:React.ReactNode}) {
  return <html lang="ja"><body style={{fontFamily:'sans-serif',maxWidth:720,margin:'3rem auto',lineHeight:1.7}}>{children}</body></html>;
}
