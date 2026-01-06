# 河道漂浮物检测系统 (River Floating Detection System)

> 🚀 **新手必读**：请查看 [**启动说明书.md (项目启动指南)**](./启动说明书.md) 获取从环境搭建到启动的详细步骤。


一个基于深度学习的河道漂浮物智能检测系统，支持图片和视频的实时识别与分析。

## 📋 项目简介

本项目是一个全栈的河道漂浮物检测系统，采用前后端分离架构，结合 YOLOv8 深度学习模型实现高精度的漂浮物检测。系统支持图片和视频的批量处理，提供可视化监控地图和数据分析功能。

## ✨ 主要功能

- 🔐 **用户认证系统**：支持用户注册、登录、忘记密码等功能
- 🖼️ **图片识别**：上传图片进行漂浮物检测，返回标注结果
- 🎥 **视频识别**：支持视频文件上传，自动逐帧检测并生成结果视频
- 🗺️ **监控地图**：基于 Leaflet 的地图展示，支持监控点管理
- 📊 **数据可视化**：使用 ECharts 展示检测统计和趋势分析
- 👥 **用户管理**：管理员可管理用户信息和权限
- 📈 **识别结果管理**：查看历史识别记录和结果详情

## 🛠️ 技术栈

### 前端
- **框架**：Vue 3.3.4
- **UI 组件库**：Element Plus 2.4.4
- **状态管理**：Pinia 2.1.7
- **路由**：Vue Router 4.2.5
- **图表**：ECharts 5.4.3 + Vue-ECharts 6.6.1
- **地图**：Leaflet 1.9.4
- **构建工具**：Vite 5.0.8
- **HTTP 客户端**：Axios 1.6.2

### 后端
- **框架**：Spring Boot 2.7.14
- **ORM**：MyBatis Plus 3.5.3.1
- **数据库**：MySQL 8.0.33
- **安全**：Spring Security + JWT (jjwt 0.11.5)
- **工具**：Lombok

### AI 推理服务
- **框架**：FastAPI 0.115.0
- **深度学习**：Ultralytics YOLOv8 8.3.7
- **图像处理**：OpenCV 4.10.0.84
- **科学计算**：NumPy 1.26.4
- **服务器**：Uvicorn 0.30.6

## 📁 项目结构

```
pfw/
├── backend/                 # Spring Boot 后端服务
│   ├── src/
│   │   └── main/
│   │       ├── java/        # Java 源代码
│   │       └── resources/
│   │           └── application.yml  # 配置文件
│   ├── uploads/            # 文件上传目录
│   └── pom.xml             # Maven 依赖配置
│
├── frontend/               # Vue 3 前端应用
│   ├── src/
│   │   ├── api/           # API 接口封装
│   │   ├── components/    # 组件
│   │   ├── layouts/       # 布局组件
│   │   ├── router/        # 路由配置
│   │   ├── stores/        # Pinia 状态管理
│   │   └── views/         # 页面视图
│   ├── package.json       # NPM 依赖配置
│   └── vite.config.js     # Vite 配置
│
├── inference_service/      # FastAPI 推理服务
│   ├── main.py           # 主服务文件
│   └── requirements.txt  # Python 依赖
│
├── yolov8/                # YOLOv8 模型训练相关
│   ├── runs/             # 训练结果
│   ├── xun/              # 训练数据集
│   └── *.py              # 训练脚本
│
├── database/              # 数据库脚本
│   └── init.sql          # 数据库初始化脚本
│
└── scripts/               # 启动脚本
    └── start_all.ps1     # Windows 一键启动脚本
```

## 🚀 快速开始

### 环境要求

- **Java**：JDK 1.8+
- **Node.js**：16.0+
- **Python**：3.8+
- **MySQL**：8.0+
- **Maven**：3.6+
- **FFmpeg**（视频处理需要）

### 1. 克隆项目

```bash
git clone https://github.com/defIKUN/pfw.git
cd pfw
```

### 2. 数据库配置

```bash
# 创建数据库并导入初始化脚本
mysql -u root -p < database/init.sql
```

修改 `backend/src/main/resources/application.yml` 中的数据库连接信息：

```yaml
spring:
  datasource:
    url: jdbc:mysql://localhost:3306/river_detection?useUnicode=true&characterEncoding=utf8&useSSL=false&serverTimezone=Asia/Shanghai
    username: root
    password: your_password
```

### 3. 后端服务

```bash
cd backend
mvn clean install
mvn spring-boot:run
```

后端服务将在 `http://localhost:8080` 启动。

### 4. 推理服务

```bash
cd inference_service
pip install -r requirements.txt

# 安装 PyTorch（根据你的环境选择）
# CPU 版本：
pip install torch==2.3.1 torchvision==0.18.1 --index-url https://download.pytorch.org/whl/cpu

# CUDA 11.8 版本：
pip install torch==2.3.1 torchvision==0.18.1 --index-url https://download.pytorch.org/whl/cu118

# 启动服务
python main.py
```

推理服务将在 `http://localhost:8000` 启动。

### 5. 前端服务

```bash
cd frontend
npm install
npm run dev
```

前端服务将在 `http://localhost:3000` 启动（默认端口）。

### 6. 一键启动（Windows）

```powershell
# 在项目根目录执行
powershell -ExecutionPolicy Bypass -File .\scripts\start_all.ps1
```

脚本会自动启动所有三个服务。

## ⚙️ 配置说明

### 后端配置 (`backend/src/main/resources/application.yml`)

```yaml
server:
  port: 8080

spring:
  datasource:
    url: jdbc:mysql://localhost:3306/river_detection
    username: root
    password: your_password

# JWT 配置
jwt:
  secret: your_secret_key
  expiration: 86400000  # 24小时

# 文件上传配置
file:
  upload-path: ./uploads
  image-path: ./uploads/images
  video-path: ./uploads/videos
  result-path: ./uploads/results

# 推理服务配置
inference:
  enabled: true
  base-url: http://localhost:8000
```

### 推理服务配置

在 `inference_service/main.py` 中可以配置：
- YOLO 模型权重路径（优先使用训练后的 `best.pt`）
- 中文字体路径（用于结果标注）
- FFmpeg 路径（视频处理）

## 📖 使用说明

1. **注册/登录**：首次使用需要注册账号，管理员账号可管理用户
2. **图片识别**：在"识别"页面上传图片，系统会自动检测并返回标注结果
3. **视频识别**：上传视频文件，系统会逐帧检测并生成结果视频
4. **监控地图**：在"监控"页面查看监控点位置和状态
5. **数据统计**：在"仪表盘"查看检测统计和趋势分析

## 🎯 YOLOv8 模型训练

项目使用 YOLOv8 进行目标检测，支持自定义训练：

```bash
cd yolov8
python train_gpu.py  # GPU 训练
# 或
python 训练.py       # 训练脚本
```

训练后的模型权重会自动被推理服务加载使用。

## 📝 API 文档

推理服务启动后，可以访问：
- Swagger UI：`http://localhost:8000/docs`
- ReDoc：`http://localhost:8000/redoc`

## 🔒 安全说明

- 使用 JWT 进行身份认证
- 密码采用加密存储
- 支持角色权限管理（admin/user）
- 文件上传大小限制：500MB

## 📦 部署建议

### 生产环境部署

1. **前端**：使用 `npm run build` 构建，部署到 Nginx
2. **后端**：使用 `mvn package` 打包，使用 `java -jar` 运行
3. **推理服务**：使用 Gunicorn 或 Uvicorn 作为生产服务器
4. **数据库**：建议使用 MySQL 8.0+ 并配置主从复制

### Docker 部署（可选）

可以编写 Dockerfile 和 docker-compose.yml 进行容器化部署。

## 🤝 贡献指南

欢迎提交 Issue 和 Pull Request！

## 📄 许可证

本项目采用 MIT 许可证。

## 👥 作者

- GitHub: [@defIKUN](https://github.com/defIKUN)

## 🙏 致谢

- [Ultralytics YOLOv8](https://github.com/ultralytics/ultralytics)
- [Spring Boot](https://spring.io/projects/spring-boot)
- [Vue.js](https://vuejs.org/)
- [Element Plus](https://element-plus.org/)

---

如有问题或建议，欢迎提交 Issue！

