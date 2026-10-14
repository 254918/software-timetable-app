# Timetable App dual-publish sync script (run locally on Windows)
# 1) download latest APKs from GitHub Release, 2) upload to Gitee Release,
# 3) sync local source (pull github -> push gitee).
# Usage: powershell -ExecutionPolicy Bypass -File scripts\sync-gitee.ps1
# Prereq: gh CLI logged in. Supports two layouts:
#   A) origin=Gitee, github=GitHub (workspace layout)
#   B) origin=GitHub, gitee=Gitee (clone-from-GitHub layout)

$ErrorActionPreference = "Stop"
$repoRoot = Split-Path -Parent (Split-Path -Parent $MyInvocation.MyCommand.Path)
Set-Location $repoRoot

Write-Host "== 1/5 get latest GitHub release =="
$tag = gh release list --repo 254918/software-timetable-app --limit 1 --json tagName --jq '.[0].tagName'
if (-not $tag) { Write-Host "no release found, abort"; exit 1 }
Write-Host "latest tag: $tag"

Write-Host "== 2/5 detect remotes and extract Gitee token =="
$remotes = @(git remote)
$githubRemote = if ($remotes -contains "github") { "github" } else { "origin" }
$giteeRemote = if ($remotes -contains "gitee") { "gitee" } else { "origin" }
$giteeUrl = git remote get-url $giteeRemote
if ($giteeUrl -notmatch 'gitee') { Write-Host "no Gitee remote found, abort"; exit 1 }
if ($giteeUrl -match '@') {
    $token = ($giteeUrl -split '@')[0] -split ':' | Select-Object -Last 1
} else {
    $token = $env:GITEE_TOKEN
}
if (-not $token) { Write-Host "cannot extract Gitee token, abort"; exit 1 }
Write-Host "github remote: $githubRemote, gitee remote: $giteeRemote, token length: $($token.Length)"

$m = [regex]::Match($giteeUrl, 'gitee\.com[:/]([^/]+/[^/\s]+?)(\.git)?$')
if (-not $m.Success) { Write-Host "cannot parse Gitee repo from $giteeUrl"; exit 1 }
$giteeRepo = $m.Groups[1].Value
Write-Host "gitee repo: $giteeRepo"

Write-Host "== 3/5 download APKs from GitHub =="
$dlDir = Join-Path $env:TEMP "gitee-sync"
New-Item -ItemType Directory -Force -Path $dlDir | Out-Null
Get-ChildItem (Join-Path $dlDir "*.apk") -ErrorAction SilentlyContinue | Remove-Item -Force
gh release download $tag --repo 254918/software-timetable-app --pattern "*.apk" --dir $dlDir
$apks = Get-ChildItem (Join-Path $dlDir "*.apk")
Write-Host "downloaded: $($apks.Name -join ', ')"

Write-Host "== 4/5 create/reuse Gitee release and upload =="
$q1 = "https://gitee.com/api/v5/repos/$giteeRepo/releases/tags/$tag`?access_token=$token"
$relJson = curl.exe -s $q1
$relId = ""
try { $relId = ($relJson | ConvertFrom-Json).id } catch {}
if (-not $relId) {
    Write-Host "Gitee tag missing, creating release..."
    $relJson = curl.exe -s -X POST "https://gitee.com/api/v5/repos/$giteeRepo/releases?access_token=$token" `
        --data-urlencode "tag_name=$tag" `
        --data-urlencode "name=timetable-app $tag" `
        --data-urlencode "target_commitish=master" `
        --data-urlencode "body=synced from GitHub Actions ($tag)"
    $relId = ($relJson | ConvertFrom-Json).id
}
Write-Host "Gitee release id: $relId"
$cur = (curl.exe -s "https://gitee.com/api/v5/repos/$giteeRepo/releases/$relId`?access_token=$token" | ConvertFrom-Json)
$haveNames = @($cur.assets | ForEach-Object { $_.name })

foreach ($apk in $apks) {
    if ($haveNames -contains $apk.Name) { Write-Host "skip $($apk.Name) (already uploaded)"; continue }
    Write-Host "uploading $($apk.Name) ..."
    $up = curl.exe -s -X POST "https://gitee.com/api/v5/repos/$giteeRepo/releases/$relId/attach_files?access_token=$token&name=$($apk.Name)" `
        -F "file=@$($apk.FullName)"
    Write-Host $up
}

Write-Host "== 5/5 sync source (pull $githubRemote, push $giteeRemote) =="
cmd /c "git pull $githubRemote master 2>&1" | Out-Host
cmd /c "git push $giteeRemote master 2>&1" | Out-Host

Write-Host ""
Write-Host "DONE. Gitee page: https://gitee.com/$giteeRepo/releases/tag/$tag"
