import fs from 'node:fs';
try {
  const value=fs.readFileSync('/run/secrets/bff_key','utf8').trim();
  const key=Buffer.from(value,'base64');
  if(key.length!==32 || key.toString('base64')!==value)throw new Error();
  for(const name of ['client_a','koiki_bff',...(process.env.BFF_RPS==='a,b'?['client_b']:[])])if(!fs.readFileSync('/run/secrets/'+name,'utf8').trim())throw new Error();
} catch {
  console.error('Required BFF runtime secret is missing or invalid');process.exit(1);
}
