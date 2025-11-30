from ultralytics import YOLO

a1 = YOLO('D:\\yolov8\\runs\\detect\\train\\weights\\best.pt')

a1("C:\\Users\\ASUS\\Pictures\\水面素材.mp4", show=True, save=True)

