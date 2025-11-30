"""
YOLOv8 GPU 训练脚本
使用方法：
1. 确保已安装 CUDA 和 PyTorch GPU 版本
2. 检查 GPU 是否可用：python -c "import torch; print(torch.cuda.is_available())"
3. 运行：python train_gpu.py
"""

from ultralytics import YOLO
import torch
import multiprocessing as mp
import os


def main():
    # 检查 GPU 是否可用
    if torch.cuda.is_available():
        print(f"✅ GPU 可用！设备：{torch.cuda.get_device_name(0)}")
        print(f"   CUDA 版本：{torch.version.cuda}")
        print(f"   PyTorch 版本：{torch.__version__}")
        device = 0  # 使用第一个 GPU
    else:
        print("⚠️  GPU 不可用，将使用 CPU 训练（速度较慢）")
        device = 'cpu'

    # 加载预训练模型（从 yolov8n.pt 开始训练）
    model = YOLO('yolov8n.pt')

    # Windows 下 DataLoader 多进程需要 spawn 防护；保险起见设为 0，避免多进程
    workers = 0 if os.name == 'nt' else max(1, os.cpu_count() // 2)

    # 开始训练
    print("\n🚀 开始训练...")
    print("=" * 60)

    results = model.train(
        data='data.yaml',           # 数据集配置文件（相对于本脚本目录）
        epochs=500,                 # 训练轮数（可根据需要调整）
        imgsz=640,                  # 输入图像尺寸
        batch=16,                   # 批次大小（GPU 显存不足时可减小，如 8 或 4）
        device=device,              # 使用 GPU (0) 或 CPU
        workers=workers,            # 数据加载线程数（Windows 建议 0）
        amp=True,                   # 自动混合精度训练
        cache=True,                 # 缓存图像到内存
        project='runs/detect',      # 项目目录
        name='train',               # 训练运行名称
        exist_ok=True,              # 允许覆盖已存在的训练结果
        patience=50,                # 早停耐心值
        save=True,                  # 保存检查点
        save_period=10,             # 每 10 轮保存一次
        val=True,                   # 训练时进行验证
        plots=True,                 # 生成训练图表
        verbose=True,               # 详细输出
        # 优化器设置
        optimizer='AdamW',          # 可选：SGD, Adam, AdamW
        lr0=0.01,                   # 初始学习率
        lrf=0.01,                   # 最终学习率（lr0 * lrf）
        momentum=0.937,             # SGD 动量
        weight_decay=0.0005,        # 权重衰减
        warmup_epochs=3,            # 预热轮数
        warmup_momentum=0.8,        # 预热动量
        warmup_bias_lr=0.1,         # 预热偏置学习率
        # 数据增强
        hsv_h=0.015,                # 色调增强
        hsv_s=0.7,                  # 饱和度增强
        hsv_v=0.4,                  # 明度增强
        degrees=0.0,                # 旋转角度
        translate=0.1,              # 平移
        scale=0.5,                  # 缩放
        shear=0.0,                  # 剪切
        perspective=0.0,            # 透视变换
        flipud=0.0,                 # 上下翻转概率
        fliplr=0.5,                 # 左右翻转概率
        mosaic=1.0,                 # Mosaic 增强概率
        mixup=0.0,                  # Mixup 增强概率
    )

    print("\n" + "=" * 60)
    print("✅ 训练完成！")
    print(f"📁 最佳模型保存在：{results.save_dir}/weights/best.pt")
    print(f"📁 最新模型保存在：{results.save_dir}/weights/last.pt")
    print("\n💡 提示：")
    print("   1. 将 best.pt 复制到推理服务可访问的位置")
    print("   2. 重启推理服务以使用新模型")
    print("   3. 查看训练结果图表：results.png, confusion_matrix.png 等")


if __name__ == '__main__':
    # Windows 多进程启动需要 spawn & freeze_support 防护
    try:
        mp.set_start_method('spawn', force=True)
    except RuntimeError:
        pass
    mp.freeze_support()
    main()


