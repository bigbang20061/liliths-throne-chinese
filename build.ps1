# Lilith's Throne 一键构建脚本
# 用法：右键 build.bat 运行，或在 PowerShell 中执行 ./build.ps1
# 产物：target\lilithsthrone (win)\lilithsthrone-<版本>.jar（同目录附带 res 资源文件夹）

$ErrorActionPreference = 'Stop'
Set-Location $PSScriptRoot

# --- 1. 定位 Maven ---
$mvn = Get-Command mvn.cmd -ErrorAction SilentlyContinue
if (-not $mvn) {
    $candidates = @(
        'D:\Program Files\maven\apache-maven-3.9.9\bin\mvn.cmd',
        'D:\Program Files\maven\apache-maven-3.8.8\bin\mvn.cmd'
    ) + (Get-ChildItem 'D:\Program Files\maven\*\bin\mvn.cmd' -ErrorAction SilentlyContinue | ForEach-Object FullName)
    $mvn = $candidates | Where-Object { Test-Path $_ } | Select-Object -First 1
} else {
    $mvn = $mvn.Source
}
if (-not $mvn) {
    Write-Error '未找到 Maven，请先安装或将其加入 PATH。'
    exit 1
}
Write-Output "使用 Maven: $mvn"

# --- 2. 确保 JAVA_HOME 可用 ---
if (-not $env:JAVA_HOME -or -not (Test-Path "$env:JAVA_HOME\bin\java.exe")) {
    $jdk = Get-ChildItem 'D:\Program Files\Java\jdk-*', 'C:\Program Files\Java\jdk-*' -ErrorAction SilentlyContinue |
           Sort-Object Name -Descending | Select-Object -First 1
    if ($jdk) {
        $env:JAVA_HOME = $jdk.FullName
        Write-Output "自动设置 JAVA_HOME: $($env:JAVA_HOME)"
    } else {
        Write-Error '未找到 JDK，请安装 JDK 17+ 或设置 JAVA_HOME。'
        exit 1
    }
}
Write-Output "使用 JAVA_HOME: $($env:JAVA_HOME)"

# --- 3. 强制 UTF-8，避免 GBK 系统下构建期损坏中文源码 ---
$env:MAVEN_OPTS = "-Dfile.encoding=UTF-8 $env:MAVEN_OPTS"

# --- 4. 读取版本号与项目名（pom.xml） ---
[xml]$pom = Get-Content pom.xml -Encoding UTF8
$version = $pom.project.version
$projName = $pom.project.name
Write-Output "构建版本: $version"

# --- 5. 构建（clean 会清空 target，须先保住发行目录里的存档） ---
$distDir = Join-Path 'target' ($projName + ' (win)')
$dataDir = Join-Path $distDir 'data'
$backupDir = Join-Path $env:TEMP ('lt-dist-data-' + [guid]::NewGuid().ToString('N'))
if (Test-Path $dataDir) {
    Write-Output "备份存档目录: $dataDir"
    New-Item -ItemType Directory -Path $backupDir | Out-Null
    Copy-Item -Path $dataDir -Destination (Join-Path $backupDir 'data') -Recurse -Force
}

try {
    & $mvn clean package
    if ($LASTEXITCODE -ne 0) {
        Write-Error "构建失败，退出码 $LASTEXITCODE"
        exit $LASTEXITCODE
    }
} finally {
    if (Test-Path (Join-Path $backupDir 'data')) {
        Write-Output "还原存档目录到: $dataDir"
        New-Item -ItemType Directory -Path $distDir -Force | Out-Null
        if (Test-Path $dataDir) {
            Remove-Item -Path $dataDir -Recurse -Force
        }
        Copy-Item -Path (Join-Path $backupDir 'data') -Destination $dataDir -Recurse -Force
        Remove-Item -Path $backupDir -Recurse -Force -ErrorAction SilentlyContinue
    }
}

# --- 6. 校验产物 ---
$jar = "target\$projName (win)\$projName-$version.jar"
if (Test-Path $jar) {
    $size = (Get-Item $jar).Length / 1MB
    Write-Output ''
    Write-Output ('构建成功: {0} ({1:N1} MB)' -f $jar, $size)
    Write-Output '运行方式：进入该目录后执行 java -jar 上述 jar（需 JDK 17+），res 文件夹已随包复制。'
} else {
    Write-Error "未找到产物 $jar"
    exit 1
}
