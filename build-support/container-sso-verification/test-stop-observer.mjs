import {test} from 'node:test';
import assert from 'node:assert/strict';
import {spawnSync} from 'node:child_process';
import {fileURLToPath} from 'node:url';

test('observer preserves HTTP response and close callback without logging secrets', () => {
  const observer = fileURLToPath(new URL('./stop-observer.cjs', import.meta.url));
  const program = `const http=require('node:http');
    const server=http.createServer((req,res)=>res.end('DIAGNOSTIC_TEST_SECRET'));
    server.listen(0,'127.0.0.1',()=>http.get({hostname:'127.0.0.1',port:server.address().port,
      path:'/a/auth/callback?code=DIAGNOSTIC_TEST_SECRET',agent:false,
      headers:{Cookie:'DIAGNOSTIC_TEST_SECRET','X-D3-Diagnostic-Client':'api'}},res=>{
        let body='';res.on('data',chunk=>body+=chunk);res.on('end',()=>{
          if(body!=='DIAGNOSTIC_TEST_SECRET'||res.statusCode!==200)process.exit(2);
          server.close(()=>process.exit(0));
        });
      }));`;
  const result = spawnSync(process.execPath, ['--require', observer, '-e', program], {encoding:'utf8', timeout:10000});
  assert.equal(result.status, 0);
  assert.match(result.stderr, /"phase":"http_close_called"/);
  assert.match(result.stderr, /"phase":"http_close_callback"/);
  assert.doesNotMatch(result.stdout + result.stderr, /DIAGNOSTIC_TEST_SECRET|callback\?code|Cookie/);
});
