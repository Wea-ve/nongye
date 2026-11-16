# 安溯·知源 VeriTrace —— 公网部署说明

> 部署时间：2026-11-16　｜　服务器：阿里云 ECS 免费试用（中国香港）
> 交付对象：传智杯参赛作品演示 / 老师评审访问

---

## 一、线上演示环境

| 项目 | 值 |
|---|---|
| **公网地址** | **http://47.243.26.118/** （自动跳转到登录页） |
| 消费者扫码页示例 | http://47.243.26.118/trace/TZ202603001001 |
| 管理端入口 | 登录页选「管理员」Tab |
| 测试账号（密码均为 `123456`） | 农户 `farmer01` / 经销商 `dealer01` / 管理员 `admin` |

已验证可用的链路：

- 首页 → 登录页（三端角色切换、背景图、品牌文案正常）
- 管理员登录 → 管理工作台（数据大屏 6 项指标 + 作物产量分布 + 投诉状态分布，图表正常渲染）
- 消费者溯源页（`/trace/{溯源码}`）：可信指数、决策建议（建议购买 + 理由 + 注意事项 + 保存建议）、农户承诺兑现、AI 助手入口全部正常
- 二维码接口 `/qrcode?code=xxx` 返回 200 PNG，**内容为公网地址**（手机扫码可直接打开）

---

## 二、服务器与成本

| 项 | 值 |
|---|---|
| 公网 IP | `47.243.26.118` |
| 私网 IP | `172.24.230.94` |
| 实例 ID | `i-j6cbdx1qz23yflc3v6pn`（地域 `cn-hongkong`，免备案） |
| 规格 | 2 核 / 4 GiB / 40 GiB ESSD Entry / 100 Mbps 峰值（按流量计费） |
| 系统 | Ubuntu 22.04.5 LTS，root 登录 |
| root 密码 | 见本地交付凭据（**不随仓库提交**） |
| 试用期 | 2026-11-16 11:00 ~ **2027-02-16 11:00** |
| 免费额度 | 300 元抵扣额度，每小时上限 0.833 元，本机型 **0.306 元/小时** |
| 实际可跑时长 | 约 **980 小时 ≈ 40.8 天**（约 2026-12-27 额度耗尽） |
| 流量额度 | 20 GiB 内地 + 200 GiB 非内地（演示用远用不完） |

**成本提醒**：额度耗尽或试用到期后，若实例未释放会转为按量付费（约 7.3 元/天）。交完作品建议在控制台把实例**释放**，或至少设置提醒。

---

## 三、运行架构

```
公网 80 (安全组已放行)
   └─ Nginx (1.18)
        └─ 反代 127.0.0.1:8081
             └─ Spring Boot 4.0.8（/opt/nongye/nongye.jar）
                  ├─ 静态前端（前端已构建内嵌 jar 的 static/app/）
                  └─ MySQL 8.0.46（127.0.0.1:3306，库 nongyeplus，14 张表）
```

- 应用配置文件：jar 内置 `application.yml` 不变，运行时通过 **`/etc/nongye.env`（权限 600）** 覆盖，systemd 用 `EnvironmentFile` 加载：
  - `DB_USERNAME=nongye` / `DB_PASSWORD=********`（真实密码仅保存在服务器 `/etc/nongye.env`，不入仓库）
  - `APP_BASE_URL=http://47.243.26.118`（**二维码溯源链接用的公网地址，改域名/换 IP 时只需改这里**）
  - `AI_API_KEY=sk-ws-...`（**已接入阿里云百炼通义千问**，2026-11-16 配置；留空则自动降级为规则文案）
- 修改 env 后必须 `systemctl restart nongye` 才生效；`/etc/nongye.env` 已是 600 权限，不要放进项目仓库
- 上传文件目录：`/opt/nongye/uploads/`
- 日志：`/opt/nongye/app.log`

---

## 四、部署文件清单（本目录）

| 文件 | 作用 |
|---|---|
| `deploy-nongye.py` | 一键部署脚本（上传 jar/SQL/配置 + 远程执行），`python deploy-nongye.py` |
| `deploy-nongye.sh` | 服务器端执行脚本（建库导入 → 建账号 → 配 Nginx → 启服务 → 自检） |
| `nongye.service` | systemd 单元（8081 端口、环境变量、内存 512m~1536m、失败自动重启） |
| `nginx-nongye.conf` | 80 端口全量反代到 8081（AI 问答超时放宽到 180s） |
| `missing_tables.sql` | 补丁：`nongyeplus.sql` 缺少的 6 张表（`trust_promise`/`consumer_*`/`fulfillment_record`） |

---

## 五、日常运维命令（服务器上执行）

```bash
# 服务状态 / 重启
systemctl status nongye
systemctl restart nongye

# 看日志
tail -f /opt/nongye/app.log

# 改配置（如填 AI Key），改完重启
vim /etc/systemd/system/nongye.service
systemctl daemon-reload && systemctl restart nongye

# 数据库备份
mysqldump -uroot nongyeplus > /opt/nongye/backup_$(date +%F).sql

# Nginx 配置校验与重载
nginx -t && systemctl reload nginx
```

## 六、重新发布新版本（本机 Windows 上执行）

```powershell
# 1) 前端改动后先构建，并把产物覆盖到后端 static
cd D:\若依\nongye\frontend
npm run build
# 将 dist/* 拷贝到 src\main\resources\static\app\ （保留 /app 前缀）

# 2) 重新打包（必须重新 package，否则 jar 里带的是旧资源）
$env:JAVA_HOME="C:\Program Files\Java\jdk-17.0.1"
& "C:\Users\study\.m2\wrapper\dists\apache-maven-3.10.0-bin\39q0qgmngdu329psje7bjgne79\apache-maven-3.10.0\bin\mvn.cmd" -B -f "D:\若依\nongye\pom.xml" package -DskipTests

# 3) 上传并重启
python D:\若依\nongye\deploy\deploy-nongye.py --upload-only
# 然后在服务器上 systemctl restart nongye
```

---

## 七、已踩过的坑（排查时先看这里）

1. **页面白屏 / 只出背景不出内容**
   jar 里漏了前端大文件（`vendor-vue-*.js`、`vendor-element-*.js`、`vendor-echarts-*.js`）。
   原因是旧 jar 是增量打包的，`static/app/assets` 没拷全。
   → 重新 `mvn package`，然后校验：
   `python -c "import zipfile;print(len([n for n in zipfile.ZipFile('target/nongye-0.0.1-SNAPSHOT.jar').namelist() if 'static/app/assets/' in n]))"`（应为 64）。

2. **溯源页报 `Table 'nongyeplus.trust_promise' doesn't exist`**
   仓库里的 `nongyeplus.sql`（2.2MB）只含 8 张表，实际业务用到 14 张。
   → 用本目录 `missing_tables.sql` 补齐（已执行，现为 14 张表）。

3. **MySQL 8 连接报 `Public Key Retrieval is not allowed`**
   `caching_sha2_password` + 非 SSL 连接的老问题。
   → 已把 `nongye@localhost` 改为 `mysql_native_password`（高校项目那边是在 JDBC URL 加 `allowPublicKeyRetrieval=true`）。

4. **默认监听 80 打不开**
   阿里云安全组默认没放行 80。
   → 已在安全组 `sg-j6cbdx1qz23yflc3pscv` 入方向加规则：`允许 / 优先级 1 / 自定义 TCP / 80/80 / 0.0.0.0/0`。

5. **阿里云控制台广告浮层挡住按钮**
   "ECS 专属安全防护权益限时领取""简捷版本控制台全新上线"是 fixed 浮层，坐标点击会被拦。
   → 用 DOM 移除 `.ecs-core-modal` 后再点，或直接 `display:none`。

6. **管理端工作台顶部红字 `Unknown column 'overdue' in 'where clause'`**
   只导了 `nongyeplus.sql` + `missing_tables.sql`，**漏跑 `sql/upgrade_trust_consumer.sql`**，
   结果 `complaint`、`crop_batch`、`scan_log` 三张表少了 11 个列：
   `complaint(consumer_id, sla_hours, first_reply_time, overdue)`、
   `crop_batch(on_sale, price, stock, subscribe_open, promise_summary)`、
   `scan_log(consumer_id, scene)`。
   → 该脚本本身是幂等的（先查 `information_schema` 再 `ALTER`），直接执行即可：
   `mysql nongyeplus < sql/upgrade_trust_consumer.sql`。
   部署脚本 `deploy-nongye.sh` 已加入这一步，重装不会再漏。

7. **页面打开要等十几秒、组件一个个慢慢冒出来**
   服务端本身很快（本机 1MB 静态文件 0.013s），慢在跨境链路：到哈尔滨实测下行只有约
   70 KB/s，首屏约 1.2 MB 未压缩 JS 要 14.6 秒。
   两个原因叠在一起：
   - `gzip_proxied` 默认 `off`，**反代响应根本不会被压缩**（`gzip_types` 配了也没用）；
   - 后端返回 `Cache-Control: no-store`，每次访问都全量重下。
   → `nginx-nongye.conf` 已加 `gzip_proxied any` + `proxy_set_header Accept-Encoding ""`，
   并对 `/app/assets/` 设 `max-age=2592000, immutable`。
   实测 `vendor-element` 从 1,089,555 字节降到 341,220 字节，**下载 14.6s → 2.26s**，首屏约 3 秒；
   二次访问直接命中浏览器缓存，基本秒开。

---

## 八、AI 已接入（2026-11-16）

- Key 来源：阿里云百炼控制台 → API 管理 → API Key，归属「默认业务空间」，权限「全部」
- 注入方式：`/etc/nongye.env` 里的 `AI_API_KEY`，文件权限 600（`-rw-------`）
- 模型：`qwen-plus`（Agent 工具调用）、`qwen3-vl-plus`（图片识别）、`text-embedding-v3`（RAG 向量化）
- **已实测通过**（消费者溯源页 →「问 AI 助手」）：

  > 问：这箱西红柿怎么存？放冰箱还是常温？
  > 答：结论：建议先常温存放，成熟后可冷藏延缓软化。依据知识库：西红柿适宜储存温度 10–14℃、相对湿度 85%–90%……（来源：《果蔬贮运保鲜技术指南》2024-03）；并给出下一步动作与「依据：search_knowledge(query=储存方法)」

  说明 Agent 模式（function calling + RAG 知识库检索）确实在工作，不是模板文案。

- 注意：qwen-plus 走按量计费，账号目前**余额 0 元**，靠模型免费试用额度支撑（额度耗尽后 AI 会失效，页面其余功能不受影响）。演示期间够用，别长时间压测。
- 换 key：改 `/etc/nongye.env` 里的 `AI_API_KEY` → `systemctl restart nongye`。

## 九、后续可选的加分项

- **绑域名**：香港地域免备案，买个域名解析到 `47.243.26.118` 即可，二维码地址同步改 `APP_BASE_URL`。
- **演示数据**：目前 4 个种植批次 / 8 条溯源记录，演示够用；若想让图表更饱满可再补几条批次。
- **答辩话术**：AI 回答里能明确看到"知识库引用 + 工具调用名"，这正好证明"不是随口编的"，演示时可以点名这一点。
