import firebase_admin
from firebase_admin import credentials, storage
import numpy as np
import cv2
import ocrfn
import os

def transcribe():
    cred = credentials.Certificate("./keySX.json")
    app = firebase_admin.initialize_app(cred, {'storageBucket' : 'scribex-1653106340524.appspot.com'})

    folderPath = "images/"

    #  make list of image names from manifest
    ManifestName = folderPath+"imgfiles.txt"
    Mbucket = storage.bucket()
    Mblob = Mbucket.blob(ManifestName)
    Mdata = Mblob.download_as_text()
    ImgNameList = Mdata.split("\r\n")


    #  apply ocr to each image
    for imageName in ImgNameList:
        #  Retrieving image from firebae storage and converting it into a image object for ocr
        bucket = storage.bucket()
        blob = bucket.get_blob(folderPath+imageName)
        arr = np.frombuffer(blob.download_as_string(), np.uint8)
        img = cv2.imdecode(arr,cv2.COLOR_BGR2BGR555)

        #  Transcribing
        transcribedfilename = ocrfn.turn_img_into_text(img)

        #  Upload result json file into firebase storage
        blob1 = bucket.blob(transcribedfilename)
        blob1.upload_from_filename(transcribedfilename)
        os.remove(transcribedfilename)

transcribe()