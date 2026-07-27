# SmartAgents 本地模型一键部署脚本
# 用法：右键 → 使用 PowerShell 运行，或在终端执行: .\setup.ps1

$ErrorActionPreference = "Stop"
$LLM_DIR = "$env:LOCALAPPDATA\SmartAgents\llm"
$LLAMA_VER = "b9444"
$LLAMA_URL = "https://ghfast.top/https://github.com/ggml-org/llama.cpp/releases/download/$LLAMA_VER/llama-$LLAMA_VER-bin-win-x64.zip"
$MODEL_URL = "https://hf-mirror.com/Qwen/Qwen2.5-14B-Instruct-GGUF/resolve/main/qwen2.5-14b-instruct-q4_k_m.gguf"
$MODEL_FILE = "$LLM_DIR\qwen2.5-14b-instruct-q4_k_m.gguf"
$CONFIG_FILE = "$LLM_DIR\model-config.json"

Write-Host "========================================" -ForegroundColor Cyan
Write-Host "  SmartAgents 本地模型一键部署" -ForegroundColor Cyan
Write-Host "========================================" -ForegroundColor Cyan
Write-Host ""

# 1. 创建目录
if (-not (Test-Path $LLM_DIR)) {
    New-Item -ItemType Directory -Path $LLM_DIR -Force | Out-Null
}
Write-Host "[1/4] 目录已就绪: $LLM_DIR" -ForegroundColor Green

# 2. 下载 llama-server (~20MB)
$LLAMA_ZIP = "$env:TEMP\llama-server.zip"
$LLAMA_SERVER = "$LLM_DIR\llama-server.exe"
if (Test-Path $LLAMA_SERVER) {
    Write-Host "[2/4] llama-server 已存在，跳过下载" -ForegroundColor Yellow
} else {
    Write-Host "[2/4] 下载 llama-server (~20MB)..." -ForegroundColor Yellow
    try {
        Invoke-WebRequest -Uri $LLAMA_URL -OutFile $LLAMA_ZIP -UseBasicParsing
        Expand-Archive -Path $LLAMA_ZIP -DestinationPath $LLM_DIR -Force
        Remove-Item $LLAMA_ZIP -Force
        Write-Host "[2/4] llama-server 下载完成" -ForegroundColor Green
    } catch {
        Write-Host "[2/4] ghfast.top 不可用，尝试备用镜像..." -ForegroundColor Yellow
        $LLAMA_URL2 = "https://github.moeyy.xyz/https://github.com/ggml-org/llama.cpp/releases/download/$LLAMA_VER/llama-$LLAMA_VER-bin-win-x64.zip"
        try {
            Invoke-WebRequest -Uri $LLAMA_URL2 -OutFile $LLAMA_ZIP -UseBasicParsing
            Expand-Archive -Path $LLAMA_ZIP -DestinationPath $LLM_DIR -Force
            Remove-Item $LLAMA_ZIP -Force
        } catch {
            Write-Host "[2/4] 下载失败，请手动下载并解压到: $LLM_DIR" -ForegroundColor Red
            Write-Host "        手动下载: https://github.com/ggml-org/llama.cpp/releases" -ForegroundColor Red
            exit 1
        }
    }
}

# 3. 下载 GGUF 模型 (~8.4GB)
if (Test-Path $MODEL_FILE) {
    $size = (Get-Item $MODEL_FILE).Length / 1GB
    if ($size -gt 7) {
        Write-Host "[3/4] 模型已存在 ($([math]::Round($size,1))GB)，跳过下载" -ForegroundColor Yellow
    } else {
        Write-Host "[3/4] 模型文件不完整，重新下载..." -ForegroundColor Yellow
        Remove-Item $MODEL_FILE -Force
    }
}

if (-not (Test-Path $MODEL_FILE)) {
    Write-Host "[3/4] 下载 qwen2.5:14b 模型 (~8.4GB)..." -ForegroundColor Yellow
    Write-Host "       这可能需要 10-30 分钟，取决于网速..." -ForegroundColor DarkGray
    try {
        $ProgressPreference = 'SilentlyContinue'
        Invoke-WebRequest -Uri $MODEL_URL -OutFile $MODEL_FILE -UseBasicParsing
        $size = (Get-Item $MODEL_FILE).Length / 1GB
        Write-Host "[3/4] 模型下载完成 ($([math]::Round($size,1))GB)" -ForegroundColor Green
    } catch {
        Write-Host "[3/4] 下载失败: $_" -ForegroundColor Red
        Write-Host "       请检查网络或稍后重试" -ForegroundColor Red
        exit 1
    }
}

# 4. 创建配置和启动脚本
$config = @{
    model_path = $MODEL_FILE
    host = "127.0.0.1"
    port = 8080
    n_ctx = 32768
    n_gpu_layers = 0
} | ConvertTo-Json

Set-Content -Path $CONFIG_FILE -Value $config -Encoding UTF8

$startScript = @"
@echo off
title SmartAgents LLM Server
echo Starting SmartAgents local model...
echo Model: qwen2.5-14b-instruct Q4_K_M
echo API: http://127.0.0.1:8080/v1/chat/completions
echo.
"$LLM_DIR\llama-server.exe" -m "$MODEL_FILE" --host 127.0.0.1 --port 8080 -c 32768
pause
"@

Set-Content -Path "$LLM_DIR\start-model.bat" -Value $startScript -Encoding ASCII

# 创建桌面快捷方式
$WScriptShell = New-Object -ComObject WScript.Shell
$Shortcut = $WScriptShell.CreateShortcut("$env:USERPROFILE\Desktop\启动本地模型.lnk")
$Shortcut.TargetPath = "$LLM_DIR\start-model.bat"
$Shortcut.WorkingDirectory = $LLM_DIR
$Shortcut.Description = "启动 SmartAgents 本地大模型服务"
$Shortcut.Save()

Write-Host "[4/4] 配置和快捷方式已创建" -ForegroundColor Green
Write-Host ""
Write-Host "========================================" -ForegroundColor Cyan
Write-Host "  部署完成！" -ForegroundColor Green
Write-Host "========================================" -ForegroundColor Cyan
Write-Host ""
Write-Host "  启动方式（任选）：" -ForegroundColor White
Write-Host "  1. 桌面双击「启动本地模型」快捷方式" -ForegroundColor White
Write-Host "  2. 在 SmartAgents 设置页切到本地模型" -ForegroundColor White
Write-Host ""
Write-Host "  模型目录: $LLM_DIR" -ForegroundColor DarkGray
Read-Host "按回车键退出"
