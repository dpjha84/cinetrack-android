const http = require('http');
const fs = require('fs');
const path = require('path');

const PORT = 3000;
const PUBLIC_DIR = path.join(__dirname, 'public');
const ROOT_ADS_TXT = path.join(__dirname, 'app-ads.txt');

function sendFileWithRange(req, res, filePath, stats, contentType, downloadName) {
  const baseHeaders = {
    'Content-Type': contentType,
    'Accept-Ranges': 'bytes',
    'Last-Modified': stats.mtime.toUTCString(),
    'Cache-Control': 'no-store, no-cache, must-revalidate',
    'X-Content-Type-Options': 'nosniff'
  };
  if (downloadName) {
    baseHeaders['Content-Disposition'] = `attachment; filename="${downloadName}"`;
  }

  const total = stats.size;
  const rangeHeader = req.headers.range;

  if (rangeHeader && rangeHeader.startsWith('bytes=')) {
    const [startStr, endStr] = rangeHeader.replace('bytes=', '').split('-');
    let start = startStr === '' ? 0 : parseInt(startStr, 10);
    let end = endStr === '' ? total - 1 : parseInt(endStr, 10);

    if (startStr === '' && endStr !== '') {
      const suffixLen = parseInt(endStr, 10);
      start = Math.max(0, total - suffixLen);
      end = total - 1;
    }

    if (Number.isNaN(start) || Number.isNaN(end) || start > end || start >= total) {
      res.writeHead(416, {
        ...baseHeaders,
        'Content-Range': `bytes */${total}`
      });
      res.end();
      return;
    }

    end = Math.min(end, total - 1);
    const chunkSize = end - start + 1;
    const rangeHeaders = {
      ...baseHeaders,
      'Content-Range': `bytes ${start}-${end}/${total}`,
      'Content-Length': chunkSize
    };

    if (req.method === 'HEAD') {
      res.writeHead(206, rangeHeaders);
      res.end();
      return;
    }

    res.writeHead(206, rangeHeaders);
    const stream = fs.createReadStream(filePath, { start, end });
    stream.on('error', (err) => {
      console.error('Range stream error:', err);
      if (!res.headersSent) res.writeHead(500);
      res.end();
    });
    stream.pipe(res);
    return;
  }

  const fullHeaders = {
    ...baseHeaders,
    'Content-Length': total
  };

  if (req.method === 'HEAD') {
    res.writeHead(200, fullHeaders);
    res.end();
    return;
  }

  res.writeHead(200, fullHeaders);
  const stream = fs.createReadStream(filePath);
  stream.on('error', (err) => {
    console.error('Stream error:', err);
    if (!res.headersSent) res.writeHead(500);
    res.end();
  });
  stream.pipe(res);
}

const server = http.createServer((req, res) => {
  res.setHeader('Access-Control-Allow-Origin', '*');
  res.setHeader('Access-Control-Allow-Methods', 'GET, HEAD, OPTIONS');
  res.setHeader('Access-Control-Allow-Headers', 'Range, Content-Type');

  if (req.method === 'OPTIONS') {
    res.writeHead(204);
    res.end();
    return;
  }

  let reqPath = decodeURI(req.url.split('?')[0]);
  if (reqPath === '/' || reqPath === '') {
    reqPath = '/index.html';
  }

  if (reqPath === '/app-ads.txt' && fs.existsSync(ROOT_ADS_TXT)) {
    const stat = fs.statSync(ROOT_ADS_TXT);
    sendFileWithRange(req, res, ROOT_ADS_TXT, stat, 'text/plain; charset=utf-8', null);
    return;
  }

  const lower = reqPath.toLowerCase();
  let downloadName = null;
  if (
    lower === '/watchpilot-v5.0.0.apk' ||
    lower === '/watchpilot-v1.0.0.apk' ||
    lower === '/watchpilot.apk' ||
    lower === '/movieverse.apk' ||
    lower === '/app-debug.apk'
  ) {
    reqPath = '/WatchPilot-v5.0.0.apk';
    downloadName = 'WatchPilot-v5.0.0.apk';
  } else if (
    lower === '/watchpilot-v5.0.0-debug.aab' ||
    lower === '/watchpilot-v5.0.0.aab' ||
    lower === '/watchpilot-v1.0.0-debug.aab' ||
    lower === '/watchpilot-v1.0.0.aab'
  ) {
    reqPath = '/WatchPilot-v5.0.0-debug.aab';
    downloadName = 'WatchPilot-v5.0.0-debug.aab';
  } else if (lower === '/watchpilot-v6.0.0.apk' || lower === '/watchpilot-v2.0.0.apk') {
    reqPath = '/WatchPilot-v6.0.0.apk';
    downloadName = 'WatchPilot-v6.0.0.apk';
  }

  let filePath = path.join(PUBLIC_DIR, reqPath);

  if (!filePath.startsWith(PUBLIC_DIR)) {
    res.writeHead(403);
    res.end('Forbidden');
    return;
  }

  if (reqPath === '/WatchPilot-v5.0.0.apk') {
    const gradleDebugApk = path.join(__dirname, 'app', 'build', 'outputs', 'apk', 'debug', 'app-debug.apk');
    if (fs.existsSync(gradleDebugApk)) {
      filePath = gradleDebugApk;
    }
  } else if (reqPath === '/WatchPilot-v5.0.0-debug.aab') {
    const gradleDebugAab = path.join(__dirname, 'app', 'build', 'outputs', 'bundle', 'debug', 'app-debug.aab');
    if (fs.existsSync(gradleDebugAab)) {
      filePath = gradleDebugAab;
    }
  }

  fs.stat(filePath, (err, stats) => {
    if (err || !stats.isFile()) {
      res.writeHead(404, { 'Content-Type': 'text/plain' });
      res.end('File Not Found');
      return;
    }

    const ext = path.extname(filePath).toLowerCase();
    let contentType = 'application/octet-stream';
    if (ext === '.html') contentType = 'text/html; charset=utf-8';
    else if (ext === '.apk') contentType = 'application/vnd.android.package-archive';
    else if (ext === '.css') contentType = 'text/css';
    else if (ext === '.js') contentType = 'application/javascript';
    else if (ext === '.jpg' || ext === '.jpeg') contentType = 'image/jpeg';
    else if (ext === '.png') contentType = 'image/png';
    else if (ext === '.json') contentType = 'application/json';
    else if (ext === '.txt') contentType = 'text/plain; charset=utf-8';

    sendFileWithRange(req, res, filePath, stats, contentType, downloadName);
  });
});

server.listen(PORT, '0.0.0.0', () => {
  console.log(`WatchPilot v2.0.0 server running on port ${PORT}`);
});
