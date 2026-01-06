<#
.SYNOPSIS
    Automated Environment Setup Script for River Floating Detection System
    河道漂浮物检测系统 - 环境自动化配置脚本

.DESCRIPTION
    This script assists in checking and installing necessary dependencies for the project.
    It uses 'winget' (Windows Package Manager) to install missing software.
    Dependencies:
    1. Java JDK 17
    2. Apache Maven
    3. Node.js (LTS)
    4. Python 3.11
    5. MySQL Server 8.0

.NOTES
    Admin privileges might be required for installation.
#>

Write-Host "==========================================================" -ForegroundColor Cyan
Write-Host "   River Floating Detection System - Environment Setup    " -ForegroundColor Cyan
Write-Host "   河道漂浮物检测系统 - 环境配置脚本                      " -ForegroundColor Cyan
Write-Host "==========================================================" -ForegroundColor Cyan
Write-Host ""

# Function to check command availability
function Test-Command {
    param($command)
    if (Get-Command $command -ErrorAction SilentlyContinue) {
        return $true
    }
    return $false
}

# Function to check and install component
function Check-And-Install {
    param($name, $command, $wingetId)
    
    Write-Host "Checking [$name]..." -NoNewline
    if (Test-Command $command) {
        Write-Host " [INSTALLED]" -ForegroundColor Green
        return
    }
    
    Write-Host " [MISSING]" -ForegroundColor Yellow
    $choice = Read-Host "Do you want to install $name using Winget? (Y/N)"
    if ($choice -eq 'Y' -or $choice -eq 'y') {
        Write-Host "Installing $name..." -ForegroundColor Cyan
        winget install -e --id $wingetId
        
        # Refresh env vars for current session is hard, usually requires restart
        Write-Host "IMPORTANT: You might need to restart your terminal/computer after installation to use $name." -ForegroundColor Magenta
    } else {
        Write-Host "Skipping $name. Please install manually." -ForegroundColor Gray
    }
}

# Check for Winget
if (-not (Test-Command winget)) {
    Write-Host "Error: 'winget' (Windows Package Manager) not found." -ForegroundColor Red
    Write-Host "Please update your Windows version or install App Installer from Microsoft Store."
    exit
}

# 1. Java JDK
Check-And-Install "Java JDK 17" "java" "Eclipse.Temurin.17"

# 2. Maven
Check-And-Install "Apache Maven" "mvn" "Apache.Maven"

# 3. Node.js
Check-And-Install "Node.js (LTS)" "node" "OpenJS.NodeJS.LTS"

# 4. Python
Check-And-Install "Python 3.11" "python" "Python.Python.3.11"

# 5. MySQL
Check-And-Install "MySQL Server" "mysql" "Oracle.MySQL"

Write-Host ""
Write-Host "==========================================================" -ForegroundColor Cyan
Write-Host "Setup Check Complete!" -ForegroundColor Green
Write-Host "If you installed new components, please RESTART YOUR TERMINAL." -ForegroundColor Yellow
Write-Host "See STARTUP_GUIDE.md for next steps." -ForegroundColor Cyan
Write-Host "==========================================================" -ForegroundColor Cyan
Pause
