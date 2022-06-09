from fastapi import FastAPI

import firebase_admin
from firebase_admin import credentials, storage
from firebase_admin import firestore
import numpy as np
import cv2
import app.ocrfn as ocrfn
import os


class FBStorage(object):
    def __init__(self, userid):
        self.obj_file = File(userid)

    def upload_result(self):
        #  Set up document reference in firestore database
        doc = self.obj_file.doc_ref.get()
        curr_doc = doc.to_dict()

        imglst = self.obj_file.transcribe_imglst()

        update_len = len(imglst)
        
        if doc.exists:
            curr_len = len(curr_doc)
            keys = []
            for i in range(curr_len+1,curr_len+update_len+1):
                keys.append(str(i))
            upload = dict(zip(keys, imglst))
            self.obj_file.doc_ref.update(upload)
        else:
            curr_len = 0
            keys = []
            for i in range(curr_len+1,curr_len+update_len+1):
                keys.append(str(i))
            upload = dict(zip(keys, imglst))
            self.obj_file.doc_ref.set(upload)


class File(object):
    def __init__(self, userid):
        self.userid = userid
        self.bucket = storage.bucket()
        self.database = firestore.client()
        self.doc_ref = self.database.collection(self.get_userid()+"/archived document/transcribed documents").document("transcribed")

    def get_imagelist(self):
        manifestblob = self.bucket.blob(self.manifest_name)
        imagestr = manifestblob.download_as_text()
        return imagestr.split("\r\n")
    
    def get_userid(self):
        #  Currently the userid is the first row of the manifest file
        return self.userid

    def transcribe_imglst(self):
        new_entries = []

        for image in self.get_imagelist()[1:]:
            #   Retrieve image from firebase storage and converting it into a image object for ocr
            blob = self.bucket.get_blob(image)
            arr = np.frombuffer(blob.download_as_string(), np.uint8)
            Timage = cv2.imdecode(arr,cv2.COLOR_BGR2BGR555)
            
            #  Transcribe
            transcribedfilename = ocrfn.turn_img_into_text(Timage)
            #  Upload result txt file into firebase storage
            outputblob = self.bucket.blob(transcribedfilename)
            outputblob.upload_from_string(transcribedfilename)
            #  Get url of uploaded file to be saved to firestore database
            url = outputblob._get_download_url(transcribedfilename)
            new_entries.append(url)
            os.remove(transcribedfilename)
        return new_entries
            

class App(object):
    __instance = None
    __inited = False

    def __new__(cls):
        if cls.__instance is None:
            cls.__instance = super().__new__(cls)
        return cls.__instance

    def __init__(self):
        if type(self).__inited:
            return
        self.cred = credentials.Certificate('./app/keySX.json')
        self.app = firebase_admin.initialize_app(self.cred, {'storageBucket' : 'scribex-1653106340524.appspot.com'})
        type(self).__inited = True


app = FastAPI()

@app.get("/app/{userid}")
def main():
    test_app = App()
    test_storage = FBStorage(userid)
    test_storage.upload_result()

if __name__ == "__main__":
    main()

