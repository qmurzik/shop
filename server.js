/* Простой статический сервер без внешних зависимостей — для локального запуска игры. */
"use strict";

var http = require("http");
var fs = require("fs");
var path = require("path");

var ROOT = path.join(__dirname, "site");
var PORT = process.env.PORT || 3000;

var MIME = {
  ".html": "text/html; charset=utf-8",
  ".js": "text/javascript; charset=utf-8",
  ".css": "text/css; charset=utf-8",
  ".json": "application/json; charset=utf-8",
  ".svg": "image/svg+xml",
  ".png": "image/png",
  ".ico": "image/x-icon"
};

var server = http.createServer(function (req, res) {
  var reqPath = decodeURIComponent(req.url.split("?")[0]);
  if (reqPath === "/") reqPath = "/index.html";

  var filePath = path.normalize(path.join(ROOT, reqPath));
  if (filePath.indexOf(ROOT) !== 0) {
    res.writeHead(403);
    res.end("Forbidden");
    return;
  }

  fs.readFile(filePath, function (err, data) {
    if (err) {
      res.writeHead(404, { "Content-Type": "text/plain; charset=utf-8" });
      res.end("404 — файл не найден");
      return;
    }
    var ext = path.extname(filePath);
    res.writeHead(200, { "Content-Type": MIME[ext] || "application/octet-stream" });
    res.end(data);
  });
});

server.listen(PORT, function () {
  console.log("«Сердце Двух Лун» запущена: http://localhost:" + PORT);
});
