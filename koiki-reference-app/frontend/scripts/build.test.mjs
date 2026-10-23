import { test } from 'node:test';
import assert from 'node:assert/strict';
import { mkdtempSync, mkdirSync, writeFileSync, readFileSync, rmSync, cpSync, symlinkSync } from 'node:fs';
import { tmpdir } from 'node:os';
import { resolve, dirname } from 'node:path';
import { fileURLToPath } from 'node:url';
import { spawnSync } from 'node:child_process';
import { checkToolchain, inputs, verifyAssets, verifyRecord } from './build-lib.mjs';
const actual = resolve(dirname(fileURLToPath(import.meta.url)), '..');
function fixture(run) {
  const dir = mkdtempSync(resolve(tmpdir(), 'koiki-spa-build-'));
  try {
    const root = resolve(dir, 'frontend'), out = resolve(dir, 'target/b1-frontend');
    mkdirSync(root); mkdirSync(resolve(out, 'assets'), { recursive: true });
    const pkg = { engines: { node: '24.19.0', npm: '11.9.0' }, packageManager: 'npm@11.9.0', dependencies: { react: '19.3.1' }, devDependencies: {} };
    writeFileSync(resolve(root, 'package.json'), JSON.stringify(pkg));
    writeFileSync(resolve(root, 'package-lock.json'), JSON.stringify({ lockfileVersion: 3, packages: { '': pkg } }));
    writeFileSync(resolve(out, 'index.html'), '<script type="module" src="/spa/assets/app.js"></script><link rel="stylesheet" href="/spa/assets/app.css">');
    writeFileSync(resolve(out, 'assets/app.js'), '/* fixture */'); writeFileSync(resolve(out, 'assets/app.css'), 'body{}');
    run(root, out, dir);
  } finally { rmSync(dir, { recursive: true, force: true }); }
}
test('a missing lock stops before build', () => fixture(root => {
  rmSync(resolve(root, 'package-lock.json'));
  assert.throws(() => checkToolchain(root, '24.19.0', '11.9.0'), /required/);
}));
test('a dependency mismatch stops before npm ci', () => fixture(root => {
  const path = resolve(root, 'package.json'); const pkg = JSON.parse(readFileSync(path)); pkg.dependencies.react = '0.0.0'; writeFileSync(path, JSON.stringify(pkg));
  assert.throws(() => checkToolchain(root, '24.19.0', '11.9.0'), /Lock/);
}));
test('a wrong Node or npm version is rejected', () => fixture(root => {
  assert.throws(() => checkToolchain(root, '22.0.0', '11.9.0'), /mismatch/);
  assert.throws(() => checkToolchain(root, '24.19.0', '10.0.0'), /mismatch/);
}));
test('missing assets, source maps and external references are rejected', () => fixture((root, out) => {
  rmSync(resolve(out, 'assets/app.js'));
  assert.throws(() => verifyAssets(out), /Missing referenced/);
  writeFileSync(resolve(out, 'assets/app.js'), '/* fixture */'); writeFileSync(resolve(out, 'assets/app.js.map'), '{}');
  assert.throws(() => verifyAssets(out), /Unexpected/);
  rmSync(resolve(out, 'assets/app.js.map')); writeFileSync(resolve(out, 'index.html'), '<script src="https://example.test/app.js"></script>');
  assert.throws(() => verifyAssets(out), /Invalid/);
}));
test('a changed input or asset invalidates an existing build record', () => fixture((root, out, dir) => {
  const record = resolve(dir, 'build.json');
  writeFileSync(record, JSON.stringify({ profile: 'b1-session-spa', inputs: inputs(root), assets: verifyAssets(out) }));
  verifyRecord(root, out, record);
  writeFileSync(resolve(root, 'changed.ts'), 'changed'); assert.throws(() => verifyRecord(root, out, record), /Stale/);
  rmSync(resolve(root, 'changed.ts')); writeFileSync(resolve(out, 'assets/app.js'), 'changed'); assert.throws(() => verifyRecord(root, out, record), /Stale/);
}));
test('the actual build stops at a TypeScript error before bundling', () => fixture((root) => {
  cpSync(resolve(actual, 'package.json'), resolve(root, 'package.json'));
  cpSync(resolve(actual, 'tsconfig.json'), resolve(root, 'tsconfig.json'));
  mkdirSync(resolve(root, 'src')); writeFileSync(resolve(root, 'src/bad.ts'), "const amount: number = 'invalid'; export { amount };\n");
  symlinkSync(resolve(actual, 'node_modules'), resolve(root, 'node_modules'), process.platform === 'win32' ? 'junction' : 'dir');
  const result = spawnSync(process.execPath, [resolve(actual, 'node_modules/typescript/bin/tsc'), '--noEmit', '-p', resolve(root, 'tsconfig.json')], { encoding: 'utf8' });
  assert.notEqual(result.status, 0); assert.match(result.stdout + result.stderr, /TS2322/);
}));
