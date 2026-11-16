#!/bin/bash
# 农溯（安溯·知源 VeriTrace）服务器端部署脚本
set -e

APP_DIR=/opt/nongye
DB_NAME=nongyeplus
DB_USER=nongye
DB_PASS="${DB_PASSWORD:?请先导出 DB_PASSWORD 环境变量（数据库 nongye 账号密码）}"

mkdir -p "$APP_DIR/uploads" "$APP_DIR/logs"

echo "[1/5] 创建数据库并导入数据"
if mysql -e "SHOW DATABASES LIKE '$DB_NAME'" | grep -q "$DB_NAME"; then
    echo "  库 $DB_NAME 已存在，跳过导入"
    IMPORTED=0
else
    mysql -e "CREATE DATABASE \`$DB_NAME\` DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci;"
    mysql "$DB_NAME" < /tmp/nongyeplus.sql
    echo "  已导入 nongyeplus.sql"

    # 基础库只有 8 张表，信任者体系另有 6 张表 + 若干新增列，必须按顺序补导
    if [ -f /tmp/missing_tables.sql ]; then
        mysql "$DB_NAME" < /tmp/missing_tables.sql && echo "  已导入 missing_tables.sql（6 张信任者体系表）"
    fi

    # 升级脚本：按 information_schema 判断后 ALTER，重复执行安全
    # 漏跑 upgrade_trust_consumer.sql 会导致 complaint.overdue 等 11 个列缺失，
    # 管理端工作台直接抛 Unknown column 'overdue' in 'where clause'
    for f in upgrade_trust_consumer.sql upgrade_ai_evidence.sql ai_knowledge.sql \
             trust_promise_seed.sql consumer_knowledge_seed.sql demo_trust_scenarios.sql; do
        if [ -f "/tmp/$f" ]; then
            mysql "$DB_NAME" < "/tmp/$f" && echo "  已导入 $f"
        else
            echo "  跳过 $f（未上传）"
        fi
    done
    IMPORTED=1
fi

echo "[2/5] 创建应用数据库账号"
mysql -e "CREATE USER IF NOT EXISTS '$DB_USER'@'localhost' IDENTIFIED WITH mysql_native_password BY '$DB_PASS';"
mysql -e "ALTER USER '$DB_USER'@'localhost' IDENTIFIED WITH mysql_native_password BY '$DB_PASS';"
mysql -e "GRANT ALL PRIVILEGES ON \`$DB_NAME\`.* TO '$DB_USER'@'localhost'; FLUSH PRIVILEGES;"
mysql -e "SELECT user,host,plugin FROM mysql.user WHERE user='$DB_USER';"

if [ "$IMPORTED" = "1" ]; then
    echo "  数据概览："
    mysql -e "SELECT 'crop_batch' t, COUNT(*) c FROM $DB_NAME.crop_batch UNION ALL SELECT 'trace_record', COUNT(*) FROM $DB_NAME.trace_record UNION ALL SELECT 'sys_user', COUNT(*) FROM $DB_NAME.sys_user;"
fi

echo "[3/5] 配置 Nginx（80 -> 8081 农溯）"
rm -f /etc/nginx/sites-enabled/default
ln -sf /etc/nginx/sites-available/nongye.conf /etc/nginx/sites-enabled/nongye.conf
rm -f /etc/nginx/sites-enabled/gaoxiao.conf
nginx -t
systemctl reload nginx
systemctl is-active nginx

echo "[4/5] 停止高校平台（保留文件，避免抢 8080/内存）"
systemctl stop gaoxiao 2>/dev/null || true
systemctl disable gaoxiao 2>/dev/null || true

echo "[5/5] 启动农溯服务"
systemctl daemon-reload
systemctl enable nongye >/dev/null 2>&1 || true
systemctl restart nongye
sleep 12
systemctl is-active nongye || true
ss -lntp | grep 8081 || echo "NO_8081"

echo "=== 自检 ==="
printf "首页 -> "; curl -s -o /dev/null -w "%{http_code}\n" http://127.0.0.1/
printf "直接后端 -> "; curl -s -o /dev/null -w "%{http_code}\n" http://127.0.0.1:8081/
echo "=== app.log 末尾 ==="
tail -25 "$APP_DIR/app.log" 2>/dev/null || echo "(暂无日志)"
echo "DEPLOY_NONGYE_DONE"
