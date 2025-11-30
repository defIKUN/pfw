from fastapi import FastAPI, UploadFile, File, Form
from fastapi.responses import JSONResponse
from fastapi.middleware.cors import CORSMiddleware
import uvicorn
import os
import shutil
import time
import json
from typing import List, Dict, Any, Optional, Tuple, Union

# 集成 YOLOv8 推理
try:
    from ultralytics import YOLO  # type: ignore
    ULTRA_AVAILABLE = True
except Exception:
    YOLO = None
    ULTRA_AVAILABLE = False

# OpenCV / PIL 处理
import cv2
import numpy as np
from PIL import Image, ImageDraw, ImageFont
import subprocess
import requests

app = FastAPI(title="River Floating Detection Inference Service")

app.add_middleware(
    CORSMiddleware,
    allow_origins=["*"],
    allow_credentials=True,
    allow_methods=["*"],
    allow_headers=["*"],
)

# 输出目录：写到后端可访问的 uploads 目录
PROJECT_ROOT = os.path.dirname(os.path.abspath(__file__))
BACKEND_ROOT = os.path.abspath(os.path.join(PROJECT_ROOT, "..", "backend"))
BACKEND_UPLOADS = os.path.join(BACKEND_ROOT, "uploads")
RESULT_DIR = os.path.join(BACKEND_UPLOADS, "results")
IMAGE_DIR = os.path.join(BACKEND_UPLOADS, "images")
VIDEO_DIR = os.path.join(BACKEND_UPLOADS, "videos")

os.makedirs(RESULT_DIR, exist_ok=True)

# yolov8 目录与权重路径探测
YOLOV8_DIR = os.path.abspath(os.path.join(PROJECT_ROOT, "..", "yolov8"))
# 优先使用用户训练得到的 best.pt，若不存在再回退到 yolov8n 预训练模型
CANDIDATE_WEIGHTS = [
    os.path.join(YOLOV8_DIR, "runs", "detect", "train", "weights", "best.pt"),
    os.path.join(YOLOV8_DIR, "yolov8n.pt"),
]

MODEL: Optional[YOLO] = None
MODEL_NAMES: Dict[int, str] = {}


def try_load_model():
    global MODEL, MODEL_NAMES
    if not ULTRA_AVAILABLE:
        return
    for w in CANDIDATE_WEIGHTS:
        if os.path.exists(w):
            try:
                MODEL = YOLO(w)
                MODEL_NAMES = getattr(MODEL, 'names', {}) or {}
                print(f"[Inference] Loaded YOLO weights: {w}")
                return
            except Exception as e:
                print(f"[Inference] Failed to load {w}: {e}")
    print("[Inference] No valid YOLO weights found. Falling back to mock/empty detections.")


try_load_model()

# 中文字体：优先环境变量 FONT_PATH，其次常见系统字体路径，最后回退 OpenCV 英文（中文可能乱码）
FONT_PATHS_TRY = [
    os.environ.get("FONT_PATH"),
    # Windows 常见字体
    r"C:\\Windows\\Fonts\\msyh.ttc",
    r"C:\\Windows\\Fonts\\msyh.ttf",
    r"C:\\Windows\\Fonts\\simhei.ttf",
    r"C:\\Windows\\Fonts\\simsun.ttc",
    # Linux 常见字体
    "/usr/share/fonts/truetype/noto/NotoSansCJK-Regular.ttc",
    "/usr/share/fonts/truetype/noto/NotoSansSC-Regular.otf",
]

def load_zh_font(size=18) -> Optional[ImageFont.FreeTypeFont]:
    for p in FONT_PATHS_TRY:
        if not p:
            continue
        try:
            if os.path.exists(p):
                return ImageFont.truetype(p, size=size)
        except Exception:
            continue
    return None

# 类别名映射（英文/模型原名 → 中文）；可按你的模型 names 继续补充
LABEL_MAP = {
    # 漂浮物常见
    "plastic_bottle": "塑料瓶", "bottle": "塑料瓶",
    "plastic_bag": "塑料袋", "bag": "塑料袋",
    "foam": "泡沫", "foam_box": "泡沫箱",
    "branch": "树枝", "twig": "树枝", "leaf": "树叶", "leaves": "树叶",
    "garbage": "垃圾", "trash": "垃圾", "waste": "垃圾",
    "can": "易拉罐", "aluminum_can": "易拉罐",
    # 船只
    "boat": "船", "ship": "船", "vessel": "船",
}

# 若模型 names 已是中文，将直接返回中文；否则尝试映射
FALLBACK_CLASSES = ["垃圾", "船"]

def map_label(label: Optional[str]) -> str:
    if label is None:
        return "未知"
    key = str(label).strip()
    mapped = LABEL_MAP.get(key)
    if mapped:
        return mapped
    lc = key.lower()
    return LABEL_MAP.get(lc, key)


def build_detections_from_result(result) -> List[Dict[str, Any]]:
    dets: List[Dict[str, Any]] = []
    try:
        boxes = result.boxes
        if boxes is None:
            return dets
        xyxy = boxes.xyxy.cpu().tolist()
        confs = boxes.conf.cpu().tolist() if boxes.conf is not None else [None] * len(xyxy)
        clss = boxes.cls.cpu().tolist() if boxes.cls is not None else [None] * len(xyxy)
        for i, (x1, y1, x2, y2) in enumerate(xyxy):
            w = max(0, x2 - x1)
            h = max(0, y2 - y1)
            cx = max(0, x1)
            cy = max(0, y1)
            c = float(confs[i]) if confs[i] is not None else None
            ci = int(clss[i]) if clss[i] is not None else -1
            label = MODEL_NAMES.get(ci) if MODEL_NAMES else None
            if not label:
                label = FALLBACK_CLASSES[ci % len(FALLBACK_CLASSES)] if ci >= 0 else FALLBACK_CLASSES[0]
            label_cn = map_label(label)
            dets.append({
                "objectClass": label_cn,
                "confidence": round(c, 4) if c is not None else None,
                "location": json.dumps({"x": int(cx), "y": int(cy), "width": int(w), "height": int(h)}, ensure_ascii=False)
            })
    except Exception:
        pass
    return dets


def draw_text_chinese(frame_bgr: np.ndarray, x: int, y: int, text: str, color=(0, 0, 255), size=18):
    font = load_zh_font(size)
    if font is None:
        cv2.putText(frame_bgr, text, (x, max(0, y - 6)), cv2.FONT_HERSHEY_SIMPLEX, 0.6, color, 2)
        return
    img = cv2.cvtColor(frame_bgr, cv2.COLOR_BGR2RGB)
    pil_img = Image.fromarray(img)
    draw = ImageDraw.Draw(pil_img)
    # PIL 需要 RGB 颜色，OpenCV 为 BGR，需要转换
    rgb_color = (color[2], color[1], color[0]) if isinstance(color, tuple) else (255, 0, 0)
    draw.text((x, max(0, y - 22)), text, font=font, fill=rgb_color)
    frame_bgr[:, :, :] = cv2.cvtColor(np.asarray(pil_img), cv2.COLOR_RGB2BGR)


# 颜色映射（BGR），不同物品不同颜色，文字与框颜色保持一致
COLOR_MAP = {
    "塑料袋": (255, 182, 193),   # 粉色
    "酒瓶": (186, 85, 211),     # 紫色
    "易拉罐": (0, 165, 255),    # 橙色
    "垃圾": (255, 0, 0),        # 蓝色（BGR）
    "塑料": (0, 215, 255),      # 金色
    "塑料盒": (0, 140, 255),    # 深橙
    "塑料瓶": (0, 255, 0),      # 绿色
    "船": (34, 139, 34),        # 深绿
    "泡沫箱": (0, 0, 255)       # 红色
}
DEFAULT_COLOR = (0, 255, 255)    # 黄色，其他类别默认

def draw_detections_color(frame: np.ndarray, dets: List[Dict[str, Any]]):
    for d in dets:
        try:
            loc = json.loads(d["location"]) if isinstance(d.get("location"), str) else d.get("location", {})
            x, y, w, h = int(loc.get("x", 0)), int(loc.get("y", 0)), int(loc.get("width", 0)), int(loc.get("height", 0))
            x2, y2 = x + w, y + h
            label = d.get("objectClass", "obj")
            conf = d.get("confidence")
            color = COLOR_MAP.get(label, DEFAULT_COLOR)
            cv2.rectangle(frame, (x, y), (x2, y2), color, 2)
            txt = f"{label} {conf:.2f}" if conf is not None else label
            draw_text_chinese(frame, x, y, txt, color=color, size=18)
        except Exception:
            continue


def summary_counts(dets: List[Dict[str, Any]]) -> Dict[str, int]:
    counts: Dict[str, int] = {}
    for d in dets:
        cls = d.get("objectClass") or "未知"
        counts[cls] = counts.get(cls, 0) + 1
    return counts


# 推理默认阈值/后处理参数（可按需调整）
DEFAULT_CONF = 0.5   # 置信度阈值（原 0.2）
DEFAULT_IOU = 0.6    # NMS IOU 阈值
DEFAULT_IMGSZ = 960  # 推理输入尺寸
DEFAULT_MAX_DET = 50 # 最大检测数量
DEFAULT_AGNOSTIC = True  # 类别无关 NMS


def parse_conf_value(conf_value: Optional[str]) -> float:
    try:
        if conf_value is None:
            return DEFAULT_CONF
        v = float(conf_value)
        if v < 0.01:
            v = 0.01
        if v > 0.99:
            v = 0.99
        return v
    except Exception:
        return DEFAULT_CONF


def parse_int(value: Optional[str], default: int, lo: int, hi: int) -> int:
    try:
        if value is None:
            return default
        v = int(value)
        if v < lo:
            v = lo
        if v > hi:
            v = hi
        return v
    except Exception:
        return default


def ffmpeg_path() -> str:
    # 优先使用本机已知路径（无需依赖外部脚本/环境变量）
    win_ffmpeg = r"C:\\Users\\ASUS\\ffmpeg-6.0-essentials_build\\bin\\ffmpeg.exe"
    if os.path.exists(win_ffmpeg):
        return win_ffmpeg
    # 其次使用环境变量
    env_ffmpeg = os.environ.get("FFMPEG_PATH")
    if env_ffmpeg and os.path.exists(env_ffmpeg):
        return env_ffmpeg
    # 最后回退到 PATH 中的 ffmpeg
    return "ffmpeg"


def transcode_to_h264(input_path: str) -> Optional[str]:
    try:
        base, _ = os.path.splitext(os.path.basename(input_path))
        out_name = f"{base}_h264.mp4"
        out_abs = os.path.join(RESULT_DIR, out_name)
        cmd = [
            ffmpeg_path(), "-y", "-i", input_path,
            "-c:v", "libx264", "-preset", "veryfast", "-crf", "23",
            "-pix_fmt", "yuv420p", "-movflags", "+faststart",
            out_abs
        ]
        subprocess.run(cmd, check=True, stdout=subprocess.DEVNULL, stderr=subprocess.DEVNULL)
        return out_abs
    except Exception:
        return None


def predict_on_image_to_file(img_bgr: np.ndarray, conf: float) -> Tuple[str, List[Dict[str, Any]]]:
    results = MODEL.predict(source=img_bgr, conf=conf, save=False, verbose=False)
    dets: List[Dict[str, Any]] = []
    if isinstance(results, list) and len(results) > 0:
        dets = build_detections_from_result(results[0])
    out = img_bgr.copy()
    draw_detections_color(out, dets)
    out_name = f"img_{int(time.time()*1000)}_result.jpg"
    out_abs = os.path.join(RESULT_DIR, out_name)
    cv2.imwrite(out_abs, out)
    public = f"/uploads/results/{out_name}"
    return public, dets


def post_progress(callback_url: Optional[str], task_id: Optional[str], task_type: Optional[str], processed: int, total: int):
    try:
        if not callback_url or not task_id or not task_type or total <= 0:
            return
        progress = int(max(0, min(100, processed * 100 / total)))
        payload = {
            "taskId": int(task_id),
            "taskType": int(task_type),
            "progress": progress,
            "processed": processed,
            "total": total
        }
        requests.post(callback_url, json=payload, timeout=2)
    except Exception:
        pass


def process_stream_to_file(stream_source: Union[str, int], out_path: str, conf: float, max_frames: int = 600, infer_stride: int = 2,
                            callback_url: Optional[str] = None, task_id: Optional[str] = None, task_type: Optional[str] = None, total: Optional[int] = None) -> List[Dict[str, Any]]:
    cap = cv2.VideoCapture(stream_source)
    if not cap.isOpened():
        raise RuntimeError("无法打开视频流/摄像头/文件")

    fps = cap.get(cv2.CAP_PROP_FPS)
    if fps is None or fps <= 0 or np.isnan(fps):
        fps = 25.0
    width = int(cap.get(cv2.CAP_PROP_FRAME_WIDTH) or 1280)
    height = int(cap.get(cv2.CAP_PROP_FRAME_HEIGHT) or 720)

    fourcc = cv2.VideoWriter_fourcc(*'mp4v')
    os.makedirs(os.path.dirname(out_path), exist_ok=True)
    writer = cv2.VideoWriter(out_path, fourcc, fps, (width, height))
    if not writer.isOpened():
        cap.release()
        raise RuntimeError("无法创建输出视频文件")

    last_dets: List[Dict[str, Any]] = []
    frame_idx = 0
    posted = 0
    total_frames = int(total) if total and total > 0 else max_frames
    try:
        while frame_idx < max_frames:
            ret, frame = cap.read()
            if not ret or frame is None:
                break

            if MODEL is not None and (frame_idx % infer_stride == 0):
                results = MODEL.predict(source=frame, conf=conf, save=False, verbose=False)
                if isinstance(results, list) and len(results) > 0:
                    dets = build_detections_from_result(results[0])
                    last_dets = dets
                    draw_detections_color(frame, dets)
            writer.write(frame)
            frame_idx += 1

            # 每处理一定数量帧上报一次，避免过于频繁
            if frame_idx - posted >= max(5, infer_stride * 2):
                post_progress(callback_url, task_id, task_type, frame_idx, total_frames)
                posted = frame_idx
    finally:
        cap.release()
        writer.release()

    # 结束时回调100
    post_progress(callback_url, task_id, task_type, total_frames, total_frames)

    return last_dets


@app.post("/infer/image")
async def infer_image(file: UploadFile = File(...), conf: Optional[str] = Form(None)):
    try:
        os.makedirs(IMAGE_DIR, exist_ok=True)
        base = os.path.splitext(file.filename or f"upload_{int(time.time()*1000)}")[0]
        ext = os.path.splitext(file.filename or "")[1] or ".jpg"
        saved_path = os.path.join(IMAGE_DIR, f"{base}_{int(time.time()*1000)}{ext}")
        with open(saved_path, 'wb') as f:
            shutil.copyfileobj(file.file, f)

        threshold = parse_conf_value(conf)
        if MODEL is not None:
            img = cv2.imread(saved_path)
            result_path_public, detections = predict_on_image_to_file(img, threshold)
        else:
            name, ext2 = os.path.splitext(os.path.basename(saved_path))
            out_name = f"{name}_result{ext2}"
            out_abs = os.path.join(RESULT_DIR, out_name)
            shutil.copyfile(saved_path, out_abs)
            result_path_public = f"/uploads/results/{out_name}"
            detections = []
        return JSONResponse({
            "resultPath": result_path_public,
            "detections": detections,
            "summary": summary_counts(detections)
        })
    except Exception as e:
        return JSONResponse(status_code=500, content={"message": str(e)})


@app.post("/infer/video")
async def infer_video(file: UploadFile = File(...), conf: Optional[str] = Form(None), frames: Optional[str] = Form(None), stride: Optional[str] = Form(None),
                      callbackUrl: Optional[str] = Form(None), taskId: Optional[str] = Form(None), taskType: Optional[str] = Form(None), total: Optional[str] = Form(None)):
    try:
        os.makedirs(VIDEO_DIR, exist_ok=True)
        base = os.path.splitext(file.filename or f"upload_{int(time.time()*1000)}")[0]
        ext = os.path.splitext(file.filename or "")[1] or ".mp4"
        saved_path = os.path.join(VIDEO_DIR, f"{base}_{int(time.time()*1000)}{ext}")
        with open(saved_path, 'wb') as f:
            shutil.copyfileobj(file.file, f)

        threshold = parse_conf_value(conf)
        max_frames = parse_int(frames, default=600, lo=60, hi=5000)
        infer_stride = parse_int(stride, default=2, lo=1, hi=30)
        total_frames = parse_int(total, default=max_frames, lo=1, hi=100000)

        result_filename = f"video_{int(time.time()*1000)}_result.mp4"
        result_path_abs = os.path.join(RESULT_DIR, result_filename)

        detections: List[Dict[str, Any]] = []
        if MODEL is not None:
            detections = process_stream_to_file(saved_path, result_path_abs, threshold, max_frames=max_frames, infer_stride=infer_stride,
                                                callback_url=callbackUrl, task_id=taskId, task_type=taskType, total=total_frames)
        else:
            shutil.copyfile(saved_path, result_path_abs)

        # FFmpeg 转码提升浏览器兼容性
        h264_abs = transcode_to_h264(result_path_abs)
        if h264_abs and os.path.exists(h264_abs):
            result_path_public = f"/uploads/results/{os.path.basename(h264_abs)}"
        else:
            result_path_public = f"/uploads/results/{os.path.basename(result_path_abs)}"

        return JSONResponse({
            "resultPath": result_path_public,
            "detections": detections,
            "summary": summary_counts(detections)
        })
    except Exception as e:
        return JSONResponse(status_code=500, content={"message": str(e)})


@app.post("/infer/stream")
async def infer_stream(videoUrl: str = Form(...), conf: Optional[str] = Form(None), frames: Optional[str] = Form(None), stride: Optional[str] = Form(None),
                       callbackUrl: Optional[str] = Form(None), taskId: Optional[str] = Form(None), taskType: Optional[str] = Form(None), total: Optional[str] = Form(None)):
    try:
        url_str = (videoUrl or '').strip()
        stream_source: Union[str, int] = int(url_str) if url_str.isdigit() else url_str

        threshold = parse_conf_value(conf)
        max_frames = parse_int(frames, default=600, lo=60, hi=5000)
        infer_stride = parse_int(stride, default=2, lo=1, hi=30)
        total_frames = parse_int(total, default=max_frames, lo=1, hi=100000)

        result_filename = f"stream_{int(time.time())}_result.mp4"
        result_path_abs = os.path.join(RESULT_DIR, result_filename)

        detections: List[Dict[str, Any]] = []
        if MODEL is not None:
            detections = process_stream_to_file(stream_source, result_path_abs, threshold, max_frames=max_frames, infer_stride=infer_stride,
                                                callback_url=callbackUrl, task_id=taskId, task_type=taskType, total=total_frames)
        else:
            cap = cv2.VideoCapture(stream_source)
            if not cap.isOpened():
                raise RuntimeError("无法打开视频流/摄像头")
            fps = cap.get(cv2.CAP_PROP_FPS) or 25.0
            width = int(cap.get(cv2.CAP_PROP_FRAME_WIDTH) or 1280)
            height = int(cap.get(cv2.CAP_PROP_FRAME_HEIGHT) or 720)
            fourcc = cv2.VideoWriter_fourcc(*'mp4v')
            writer = cv2.VideoWriter(result_path_abs, fourcc, fps, (width, height))
            frames_written = 0
            while frames_written < max_frames:
                ret, frame = cap.read()
                if not ret:
                    break
                writer.write(frame)
                frames_written += 1
            cap.release()
            writer.release()

        # FFmpeg 转码提升浏览器兼容性
        h264_abs = transcode_to_h264(result_path_abs)
        if h264_abs and os.path.exists(h264_abs):
            result_path_public = f"/uploads/results/{os.path.basename(h264_abs)}"
        else:
            result_path_public = f"/uploads/results/{os.path.basename(result_path_abs)}"

        return JSONResponse({
            "resultPath": result_path_public,
            "detections": detections,
            "summary": summary_counts(detections)
        })
    except Exception as e:
        return JSONResponse(status_code=500, content={"message": str(e)})


if __name__ == "__main__":
    uvicorn.run(app, host="0.0.0.0", port=8000)
