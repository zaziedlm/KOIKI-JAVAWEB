// Opt-in, diagnostic-only HTTP observer. Never logs URLs, headers or payloads.
const http = require('node:http');
const fs = require('node:fs');
const sockets = new Map();
let sequence = 0;
function category(req) {
  if (req.url?.startsWith('/health/')) return 'health';
  if (/^\/[ab]\/auth\//.test(req.url || '')) return 'auth';
  if (/^\/[ab]\/api\//.test(req.url || '')) return 'business-api';
  return req.headers.rsc === '1' ? 'rsc' : 'page-or-asset';
}
function observe(socket) {
  if (!sockets.has(socket)) {
    const entry = {id: ++sequence, requests: 0, active: 0, finished: 0, response_closed: 0};
    sockets.set(socket, entry);
    socket.once('close', () => sockets.delete(socket));
  }
  return sockets.get(socket);
}
function report(phase) {
  const values = [...sockets].map(([socket, entry]) => {
    const fd = socket._handle?.fd;
    let owned = false;
    try { owned = Number.isInteger(fd) && fs.readlinkSync('/proc/self/fd/' + fd).startsWith('socket:['); } catch {}
    return {...entry, owner_pid: process.pid, fd_owned_by_process: owned,
      destroyed: socket.destroyed, readable: socket.readable, writable: socket.writable};
  });
  console.error('D3_STOP_DIAG ' + JSON.stringify({pid: process.pid, phase,
    socket_count: values.length, active_requests: values.reduce((sum, value) => sum + value.active, 0),
    sockets: values.slice(0, 64), truncated: values.length > 64}));
}
const originalEmit = http.Server.prototype.emit;
http.Server.prototype.emit = function(event, ...args) {
  if (event === 'connection') observe(args[0]);
  if (event === 'request') {
    const [req, res] = args, entry = observe(req.socket);
    const marker = req.headers['x-d3-diagnostic-client'];
    entry.client = ['api', 'api-rsc', 'page'].includes(marker) ? marker : 'unmarked';
    entry.category = category(req); entry.requests++; entry.active++;
    let completed = false;
    const complete = () => { if (!completed) { completed = true; entry.active--; } };
    res.once('finish', () => { entry.finished++; complete(); });
    res.once('close', () => { entry.response_closed++; complete(); });
  }
  if (event === 'close') report('http_server_close_event');
  return Reflect.apply(originalEmit, this, [event, ...args]);
};
const originalClose = http.Server.prototype.close;
http.Server.prototype.close = function(callback) {
  report('http_close_called');
  return Reflect.apply(originalClose, this, [function(...args) {
    report('http_close_callback');
    if (typeof callback === 'function') return Reflect.apply(callback, this, args);
  }]);
};
process.on('SIGTERM', () => {
  report('SIGTERM_observed');
  for (const seconds of [2, 10, 25, 35]) setTimeout(() => report('SIGTERM_after_' + seconds + 's'), seconds * 1000).unref();
});
