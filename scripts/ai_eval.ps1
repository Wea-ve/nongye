# AI 问答评测：检索命中率 / 引用可核验率 / 关键词覆盖率
# 用法：powershell -ExecutionPolicy Bypass -File scripts\ai_eval.ps1
#   可选参数：-BaseUrl http://localhost:8081
param(
    [string]$BaseUrl = "http://localhost:8081",
    [string]$Username = "admin",
    [string]$Password = "123456"
)

[Console]::OutputEncoding = [System.Text.Encoding]::UTF8
$utf8 = New-Object System.Text.UTF8Encoding($false)
$tmpDir = Join-Path $env:TEMP ("ai_eval_" + [Guid]::NewGuid().ToString("N"))
New-Item -ItemType Directory -Path $tmpDir | Out-Null
$cookie = Join-Path $tmpDir "cookies.txt"

# 登录（携带 role，否则会被当作账号密码错误）
$loginBody = @{ username = $Username; password = $Password; role = "admin" } | ConvertTo-Json -Compress
$loginFile = Join-Path $tmpDir "login.json"
[System.IO.File]::WriteAllText($loginFile, $loginBody, $utf8)
$loginResp = curl.exe -s -c $cookie -H "Content-Type: application/json" --data-binary "@$loginFile" "$BaseUrl/api/auth/login"
if ($loginResp -notmatch '"code":0') {
    Write-Host "登录失败：$loginResp" -ForegroundColor Red
    exit 1
}

# 真实问题集：expect 为回答中应出现的关键词（任一），types 为应当命中的依据类型
$cases = @(
    @{ batchId = 1; q = "这些西红柿安全吗？打药情况怎么样？"; expect = @("安全", "检测", "打药"); types = @("data", "record", "report") },
    @{ batchId = 1; q = "这个批次用过什么农药？"; expect = @("吡虫啉", "农药", "打药"); types = @("record") },
    @{ batchId = 1; q = "番茄灰霉病怎么防治？"; expect = @("防治", "霉"); types = @("knowledge") },
    @{ batchId = 1; q = "西红柿怎么清洗才能减少农残？"; expect = @("清洗", "浸泡", "流水"); types = @("knowledge") },
    @{ batchId = 1; q = "这个批次的检测报告结果是什么？"; expect = @("合格", "检测"); types = @("report") },
    @{ batchId = 1; q = "这个批次经过哪些流转环节？"; expect = @("流转", "合作社", "经销"); types = @("trace") },
    @{ batchId = 1; q = "农药的安全间隔期是多久？"; expect = @("间隔期", "天"); types = @("knowledge") },
    @{ batchId = 2; q = "这批黄瓜施过肥吗？"; expect = @("施肥", "农事", "记录"); types = @("record") },
    @{ batchId = 3; q = "南瓜的产地在哪里？"; expect = @("产地", "基地", "合作社", "农场"); types = @("data") },
    @{ batchId = 4; q = "北瓜的追溯码是什么？"; expect = @("追溯", "码"); types = @("data") }
)

$rows = @()
$i = 0
foreach ($c in $cases) {
    $i++
    $body = @{ batchId = $c.batchId; question = $c.q; sessionId = "eval-$i" } | ConvertTo-Json -Compress
    $bodyFile = Join-Path $tmpDir "q$i.json"
    $outFile = Join-Path $tmpDir "r$i.json"
    [System.IO.File]::WriteAllText($bodyFile, $body, $utf8)
    curl.exe -s -b $cookie -H "Content-Type: application/json" --data-binary "@$bodyFile" -o $outFile --max-time 180 "$BaseUrl/api/ai/chat"

    $r = $null
    try { $r = Get-Content $outFile -Raw -Encoding UTF8 | ConvertFrom-Json } catch { }
    if ($r -eq $null) {
        $rows += [pscustomobject]@{ n = $i; q = $c.q; retrieval = "FAIL"; citation = "FAIL"; answer = "FAIL"; note = "接口无响应" }
        continue
    }

    $sources = @($r.sources)
    $typeSet = @($sources | ForEach-Object { $_.type } | Select-Object -Unique)
    $missing = @($c.types | Where-Object { $typeSet -notcontains $_ })
    $retrieval = if ($sources.Count -gt 0 -and $missing.Count -eq 0) { "PASS" } else { "FAIL" }

    # 引用校验：回答中的《…》必须能在知识库依据的标题/详情里找到
    $cites = [regex]::Matches([string]$r.reply, "《([^》]{2,40})》")
    $citation = "PASS"
    if ($cites.Count -gt 0) {
        $allowed = (($sources | Where-Object { $_.type -eq "knowledge" } | ForEach-Object { "$($_.label)|$($_.detail)|" }) -join "").Replace(" ", "")
        foreach ($m in $cites) {
            $title = $m.Groups[1].Value.Replace(" ", "")
            if ($allowed.Length -eq 0 -or -not $allowed.Contains($title)) { $citation = "FAIL"; break }
        }
    }

    $kwHit = @($c.expect | Where-Object { ([string]$r.reply).Contains($_) })
    $answer = if ($kwHit.Count -gt 0) { "PASS" } else { "FAIL" }
    $rows += [pscustomobject]@{ n = $i; q = $c.q; retrieval = $retrieval; citation = $citation; answer = $answer; note = ("tools=$($r.tools.Count) sources=$($sources.Count)") }
}

$rows | Format-Table -AutoSize -Wrap
$total = $rows.Count
$r1 = @($rows | Where-Object { $_.retrieval -eq "PASS" }).Count
$r2 = @($rows | Where-Object { $_.citation -eq "PASS" }).Count
$r3 = @($rows | Where-Object { $_.answer -eq "PASS" }).Count
Write-Host ""
Write-Host ("检索命中率：" + $r1 + "/" + $total + " = " + [math]::Round(100.0 * $r1 / $total) + "%")
Write-Host ("引用可核验率：" + $r2 + "/" + $total + " = " + [math]::Round(100.0 * $r2 / $total) + "%")
Write-Host ("回答关键词覆盖率：" + $r3 + "/" + $total + " = " + [math]::Round(100.0 * $r3 / $total) + "%")
