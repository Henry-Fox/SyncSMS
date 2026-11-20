#!/bin/bash
# SyncSMS 鍙戝竷鑴氭湰锛氱敱浜戞晥娴佹按绾挎垨鎵嬪姩璋冪敤
# 鐢ㄦ硶: bash deploy.sh /path/to/package.tgz   锛堝寘鍐呴渶鍚?web/ 涓?app.jar锛?set -e

SITE=/www/wwwroot/syncsms
PKG="${1:?鐢ㄦ硶: bash deploy.sh <package.tgz>}"
KEEP=5
TS=$(date +%Y%m%d-%H%M%S)

[ -f "$PKG" ] || { echo "[ERR] 鎵句笉鍒板埗鍝? $PKG"; exit 1; }

PREV=$(readlink "$SITE/current" 2>/dev/null || true)
echo "[1/5] 鍒涘缓鍙戝竷鐩綍 $TS (涓婁竴鐗? ${PREV:-none})"
mkdir -p "$SITE/releases/$TS"
tar -xzf "$PKG" -C "$SITE/releases/$TS"

if [ ! -f "$SITE/releases/$TS/app.jar" ]; then
  echo "[ERR] 鍒跺搧缂哄皯 app.jar锛屼腑姝㈠彂甯?; exit 1
fi
if [ ! -f "$SITE/releases/$TS/web/index.html" ]; then
  echo "[ERR] 鍒跺搧缂哄皯 web/index.html锛屼腑姝㈠彂甯?; exit 1
fi

echo "[2/5] 鍒囨崲杞摼"
ln -sfn "releases/$TS" "$SITE/current"

echo "[3/5] 閲嶅惎鍚庣鏈嶅姟"
systemctl restart syncsms

echo "[4/5] 鍋ュ悍妫€鏌ワ紙鏈€澶氱瓑 60s锛?
ok=0
for i in $(seq 1 30); do
  code=$(curl -s -o /dev/null -w '%{http_code}' --max-time 3 http://127.0.0.1:8081/api/user/captcha || true)
  if [ "$code" = "200" ]; then ok=1; break; fi
  sleep 2
done

if [ "$ok" != "1" ]; then
  echo "[ERR] 鍋ュ悍妫€鏌ュけ璐?code=$code)锛屽洖婊氬埌 ${PREV:-none}"
  if [ -n "$PREV" ]; then ln -sfn "$PREV" "$SITE/current"; systemctl restart syncsms; fi
  exit 1
fi

echo "[5/5] 娓呯悊鏃х増鏈紝淇濈暀鏈€杩?$KEEP 涓?
ls -1dt "$SITE"/releases/*/ 2>/dev/null | tail -n +$((KEEP + 1)) | xargs -r rm -rf

echo "鍙戝竷鎴愬姛: $TS"
