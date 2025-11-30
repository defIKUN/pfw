# PowerShell 一键启动脚本（Windows）
# 用法：右键“使用 PowerShell 运行”，或在项目根目录执行：
#   powershell -ExecutionPolicy Bypass -File .\scripts\start_all.ps1

# ========== 可配置区域 ==========
$BackendPort   = 8080
$InferencePort = 8000
$FrontendPort  = 3000

# 如需指定中文字体，可设置环境变量 FONT_PATH 或在此处填写绝对路径（可留空）
$FontPath = $Env:FONT_PATH
# 注入 FFmpeg 路径（你提供的路径）
$FfmpegPath = 'C:\Users\ASUS\ffmpeg-6.0-essentials_build\bin\ffmpeg.exe'
# ================================

function Start-InferenceService {
  Write-Host "[Start] Inference Service (FastAPI) on port $InferencePort" -ForegroundColor Green
  $cmd = @()
  $cmd += "Set-Location -Path `"$PSScriptRoot\..\inference_service`""
  if ($FontPath) { $cmd += '$env:FONT_PATH="' + $FontPath + '"' }
  if ($FfmpegPath) { $cmd += '$env:FFMPEG_PATH="' + $FfmpegPath + '"' }
  $cmd += "python -m pip install -r requirements.txt"
  $cmd += "python main.py"
  $joined = ($cmd -join " ; ")
  Start-Process powershell -ArgumentList "-NoExit","-Command",$joined -WindowStyle Normal
}

function Start-Backend {
  Write-Host "[Start] Backend (Spring Boot) on port $BackendPort" -ForegroundColor Green
  $cmd = @()
  $cmd += "Set-Location -Path `"$PSScriptRoot\..\backend`""
  # 如需更快开发启动，可改为：$cmd += "mvn spring-boot:run"
  $cmd += "mvn -q -DskipTests clean package"
  $cmd += "mvn spring-boot:run"
  $joined = ($cmd -join " ; ")
  Start-Process powershell -ArgumentList "-NoExit","-Command",$joined -WindowStyle Normal
}

function Start-Frontend {
  Write-Host "[Start] Frontend (Vite) on port $FrontendPort" -ForegroundColor Green
  $cmd = @()
  $cmd += "Set-Location -Path `"$PSScriptRoot\..\frontend`""
  $cmd += "npm install"
  $cmd += "npm run dev -- --port $FrontendPort"
  $joined = ($cmd -join " ; ")
  Start-Process powershell -ArgumentList "-NoExit","-Command",$joined -WindowStyle Normal
}

# 预检查：目录存在
$root = (Resolve-Path "$PSScriptRoot\..\").Path
if (-not (Test-Path "$root\backend\pom.xml"))           { Write-Error "未找到 backend/pom.xml，请在项目根目录下运行。"; exit 1 }
if (-not (Test-Path "$root\frontend\package.json"))     { Write-Error "未找到 frontend/package.json，请在项目根目录下运行。"; exit 1 }
if (-not (Test-Path "$root\inference_service\main.py")) { Write-Error "未找到 inference_service/main.py，请在项目根目录下运行。"; exit 1 }

# 确保上传目录存在
$uploads = Join-Path $root "backend\uploads"
New-Item -ItemType Directory -Force -Path (Join-Path $uploads "images") | Out-Null
New-Item -ItemType Directory -Force -Path (Join-Path $uploads "videos") | Out-Null
New-Item -ItemType Directory -Force -Path (Join-Path $uploads "results") | Out-Null

# 启动顺序：推理服务 -> 后端 -> 前端
Start-InferenceService
Start-Backend
Start-Frontend

Write-Host "All services launching in separate PowerShell windows..." -ForegroundColor Cyan
