import firebase_admin
from firebase_admin import credentials, storage
import numpy as np
import cv2
import ocrfn

cred = credentials.Certificate("./key.json")
app = firebase_admin.initialize_app(cred, {'storageBucket' : 'orbital-2022-ab549.appspot.com'})

#  Retrieving image from firebae storage and converting it into a image object for ocr
bucket = storage.bucket()
blob = bucket.get_blob("e3de7187e7808c21e83488b91d410a27-700.jpg")
arr = np.frombuffer(blob.download_as_string(), np.uint8)
img = cv2.imdecode(arr,cv2.COLOR_BGR2BGR555)

#  Transcribing
transcribedfilename = ocrfn.turn_img_into_text(img)

#  Upload result json file into firebase storage
blob1 = bucket.blob(transcribedfilename)
blob1.upload_from_filename(transcribedfilename)



