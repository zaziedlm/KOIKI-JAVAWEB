import { fileURLToPath } from 'node:url';
import { resolve, dirname } from 'node:path';
import { writeFileSync } from 'node:fs';
import { checkToolchain, inputs, verifyAssets } from './build-lib.mjs';
const root = resolve(dirname(fileURLToPath(import.meta.url)), '..');
const toolchain = checkToolchain(root);
const record = { schemaVersion: 1, profile: 'b1-session-spa', ...toolchain,
  inputs: inputs(root), assets: verifyAssets(resolve(root, '../target/b1-frontend')) };
writeFileSync(resolve(root, '../target/koiki-reference-spa-build.json'), JSON.stringify(record, null, 2) + '\n');
console.log('Verified SPA inputs and bundled assets.');
