# Timetable App dual-publish sync script (run locally on Windows)
# 1) download latest APKs from GitHub Release, 2) upload to Gitee Release,
# 3) sync local source (pull github -> push gitee).
# Usage: powershell -ExecutionPolicy Bypass -File scripts\sync-gitee.ps1
# Prereq: gh CLI logged in; git remotes: origin=Gitee, github=GitHub.

$ErrorActionPreference = "Stop"
$repoRoot = Split-Path -Parent (Split-Path -Parent $MyInvocation.MyCommand.Path)
Set-Location $repoRoot

Write-Host "== 1/5 get latest GitHub release =="
$tag = gh release list --repo 254918/software-timetable-app --limit 1 --json tagName --jq '.[0].tagName'
if (-not $tag) { Write-Host "no release found, abort"; exit 1 }
Write-Host "latest tag: $tag"

Write-Host "== 2/5 extract Gitee token from remote =="
$origin = git remote get-url origin
if ($origin -notmatch 'gitee') { Write-Host "origin is not Gitee remote"; exit 1 }
$token = ($origin -split '@')[0] -split ':' | Select-Object -Last 1
Write-Host "token length: $($token.Length)"

$giteeRepo = "Carl_Long/software-timetable-app"

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

Write-Host "== 5/5 sync source (pull github, push gitee) =="
cmd /c "git pull github master 2>&1" | Out-Host
cmd /c "git push origin master 2>&1" | Out-Host

Write-Host ""
Write-Host "DONE. Gitee page: https://gitee.com/$giteeRepo/releases/tag/$tag"
