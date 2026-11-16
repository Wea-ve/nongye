"""农溯（安溯·知源 VeriTrace）一键部署到阿里云 ECS。
用法：python deploy-nongye.py [--upload-only]
"""

import argparse
import os
import sys
import time

import paramiko

BASE = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
FILES = [
    (os.path.join(BASE, "target", "nongye-0.0.1-SNAPSHOT.jar"), "/opt/nongye/nongye.jar"),
    (os.path.join(BASE, "nongyeplus.sql"), "/tmp/nongyeplus.sql"),
    (os.path.join(BASE, "deploy", "missing_tables.sql"), "/tmp/missing_tables.sql"),
    # 升级脚本：补信任者体系缺失列（幂等，按 information_schema 判断后再 ALTER）
    (os.path.join(BASE, "sql", "upgrade_trust_consumer.sql"), "/tmp/upgrade_trust_consumer.sql"),
    (os.path.join(BASE, "sql", "upgrade_ai_evidence.sql"), "/tmp/upgrade_ai_evidence.sql"),
    (os.path.join(BASE, "sql", "ai_knowledge.sql"), "/tmp/ai_knowledge.sql"),
    (os.path.join(BASE, "sql", "trust_promise_seed.sql"), "/tmp/trust_promise_seed.sql"),
    (os.path.join(BASE, "sql", "consumer_knowledge_seed.sql"), "/tmp/consumer_knowledge_seed.sql"),
    (os.path.join(BASE, "sql", "demo_trust_scenarios.sql"), "/tmp/demo_trust_scenarios.sql"),
    (os.path.join(BASE, "deploy", "nongye.service"), "/etc/systemd/system/nongye.service"),
    (os.path.join(BASE, "deploy", "nginx-nongye.conf"), "/etc/nginx/sites-available/nongye.conf"),
    (os.path.join(BASE, "deploy", "deploy-nongye.sh"), "/tmp/deploy-nongye.sh"),
]


def run(c, cmd, timeout=900):
    _i, o, e = c.exec_command(cmd, timeout=timeout)
    return o.read().decode(errors="ignore"), e.read().decode(errors="ignore")


def main():
    ap = argparse.ArgumentParser()
    ap.add_argument("--host", default="47.243.26.118")
    ap.add_argument("--user", default="root")
    ap.add_argument("--pwd", default=os.environ.get("SERVER_PWD", ""),
                    help="服务器 root 密码；留空则读取环境变量 SERVER_PWD")
    ap.add_argument("--upload-only", action="store_true")
    args = ap.parse_args()

    for src, _d in FILES:
        if not os.path.exists(src):
            print("MISSING:", src)
            return 2

    c = paramiko.SSHClient()
    c.set_missing_host_key_policy(paramiko.AutoAddPolicy())
    c.connect(args.host, username=args.user, password=args.pwd, timeout=30)
    run(c, "mkdir -p /opt/nongye/uploads")

    sftp = c.open_sftp()
    for src, dst in FILES:
        t0 = time.time()
        sftp.put(src, dst)
        print("uploaded %-38s %6.1f MB %.1fs" % (os.path.basename(src), os.path.getsize(src) / 1048576.0, time.time() - t0))
    sftp.close()

    run(c, "sed -i 's/\\r$//' /tmp/deploy-nongye.sh /etc/systemd/system/nongye.service /etc/nginx/sites-available/nongye.conf")

    if not args.upload_only:
        print("running remote deploy ...")
        out, err = run(c, "bash /tmp/deploy-nongye.sh 2>&1")
        print(out[-5000:])
        if err.strip():
            print("[stderr]", err[-1000:])

    c.close()
    print("CLIENT_DONE")
    return 0


if __name__ == "__main__":
    sys.exit(main())
