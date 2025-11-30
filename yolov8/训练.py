"""
YOLOv8 训练脚本（自动检测 GPU/CPU）
使用方法：python 训练.py
"""

from ultralytics import YOLO
import torch

# 检查 GPU 是否可用
if torch.cuda.is_available():
    print(f"✅ GPU 可用！设备：{torch.cuda.get_device_name(0)}")
    device = 0  # 使用第一个 GPU
else:
    print("⚠️  GPU 不可用，将使用 CPU 训练（速度较慢）")
    device = 'cpu'

# 加载预训练模型
model = YOLO('yolov8n.pt')

# 开始训练
print("\n🚀 开始训练...")
print("=" * 60)

model.train(
    data='data.yaml',
    epochs=500,
    imgsz=640,
    batch=16 if device == 0 else 8,  # GPU 用 16，CPU 用 8
    device=device,
    amp=True if device == 0 else False,  # GPU 启用混合精度
    cache=True,  # 缓存图像到内存（加速训练）
    workers=4,
    patience=50,  # 早停
    save=True,
    save_period=10,
    val=True,
    plots=True,
    verbose=True
)

print("\n" + "=" * 60)
print('✅ 模型训练完毕！')
print(f"📁 最佳模型：runs/detect/train/weights/best.pt")
print(f"📁 最新模型：runs/detect/train/weights/last.pt")