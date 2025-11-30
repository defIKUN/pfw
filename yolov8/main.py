from ultralytics import YOLO
from PIL import Image

model = YOLO("D:\\yolov8\\.venv\\Lib\\site-packages\\ultralytics\\assets\\yolov8n.pt")

source = "D:\\yolov8\\.venv\\Lib\\site-packages\\ultralytics\\assets\\zidane.jpg"

results = model(source)

for r in results:
    im_array = r.plot()
    im = Image.fromarray(im_array[..., ::-1])
    im.show()
    im.save("results.jpg")
