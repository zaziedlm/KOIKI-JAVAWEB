import { spawnSync } from 'node:child_process';
import { createHash } from 'node:crypto';
import { readFileSync, readdirSync, existsSync } from 'node:fs';
import { resolve, relative, sep } from 'node:path';

export function npm(root, args) {
  // All command strings on Windows are fixed here; paths are passed as cwd only.
  const command = process.platform === 'win32' ? 'cmd.exe' : 'npm';
  const fixed = { '--version': 'npm --version', 'ci': 'npm ci --no-audit --no-fund', 'run': 'npm run build' };
  const invocation = process.platform === 'win32' ? ['/d', '/s', '/c', fixed[args[0]]] : args;
  const result = spawnSync(command, invocation, { cwd: root, encoding: 'utf8', stdio: args[0] === '--version' ? 'pipe' : 'inherit' });
  if (result.error || result.status !== 0) throw new Error('npm command failed: ' + args[0]);
  return result.stdout?.trim();
}
export const sha256 = path => createHash('sha256').update(readFileSync(path)).digest('hex');
export function inputs(root) {
  const result = {};
  function visit(directory) {
    for (const entry of readdirSync(directory, { withFileTypes: true }).sort((a,b) => a.name.localeCompare(b.name))) {
      if (entry.name === 'node_modules') continue;
      const path = resolve(directory, entry.name);
      if (entry.isDirectory()) visit(path);
      else if (entry.isFile()) result[relative(root, path).split(sep).join('/')] = sha256(path);
      else throw new Error('Unsupported frontend input: ' + entry.name);
    }
  }
  visit(root);
  return result;
}
export function checkToolchain(root, nodeVersion = process.versions.node, npmVersion = npm(root, ['--version'])) {
  const pkg = JSON.parse(readFileSync(resolve(root, 'package.json'), 'utf8'));
  if (nodeVersion !== pkg.engines.node || npmVersion !== pkg.engines.npm || pkg.packageManager !== 'npm@' + npmVersion) throw new Error('Node/npm version mismatch');
  if (!existsSync(resolve(root, 'package-lock.json'))) throw new Error('package-lock.json is required');
  const lock = JSON.parse(readFileSync(resolve(root, 'package-lock.json'), 'utf8'));
  const locked = lock.packages?.[''];
  if (lock.lockfileVersion !== 3 || !locked || JSON.stringify(locked.dependencies) !== JSON.stringify(pkg.dependencies)
      || JSON.stringify(locked.devDependencies) !== JSON.stringify(pkg.devDependencies)) throw new Error('Lock does not match package.json');
  return { node: nodeVersion, npm: npmVersion };
}
export function verifyAssets(out) {
  const index = resolve(out, 'index.html');
  if (!existsSync(index)) throw new Error('Missing SPA index');
  const html = readFileSync(index, 'utf8');
  const refs = [...html.matchAll(/(?:src|href)=["']([^"']+)["']/g)].map(match => match[1]);
  if (!refs.some(ref => ref.endsWith('.js')) || !refs.some(ref => ref.endsWith('.css')) || refs.some(ref => !/^\/spa\/assets\/[a-zA-Z0-9_.-]+\.(js|css)$/.test(ref))) throw new Error('Invalid SPA asset reference');
  const files = { 'index.html': sha256(index) };
  for (const entry of readdirSync(resolve(out, 'assets'), { withFileTypes: true })) {
    if (!entry.isFile() || !/^[a-zA-Z0-9_.-]+\.(js|css)$/.test(entry.name)) throw new Error('Unexpected SPA asset');
    const name = 'assets/' + entry.name;
    if (entry.name.endsWith('.css') && /url\(\s*["']?(?:https?:|\/\/)/i.test(readFileSync(resolve(out, name), 'utf8'))) throw new Error('External stylesheet resource');
    files[name] = sha256(resolve(out, name));
  }
  for (const ref of refs) if (!files[ref.slice('/spa/'.length)]) throw new Error('Missing referenced asset');
  return files;
}
export function verifyRecord(root, out, recordPath) {
  const record = JSON.parse(readFileSync(recordPath, 'utf8'));
  if (record.profile !== 'b1-session-spa' || JSON.stringify(record.inputs) !== JSON.stringify(inputs(root))
      || JSON.stringify(record.assets) !== JSON.stringify(verifyAssets(out))) throw new Error('Stale or inconsistent SPA build');
  return record;
}
