from fastapi import FastAPI

import firebase_admin
from firebase_admin import credentials, storage
from firebase_admin import firestore
import numpy as np
import os
import urllib.request
import pyrebase
from google.cloud import vision_v1


config = {
    "apiKey": "AIzaSyAAXXwocAH6k5TSn8XvDpEI-LQpXV4-95E",
    "authDomain": "scribex-1653106340524.firebaseapp.com",
    "databaseURL": "https://scribex-1653106340524-default-rtdb.asia-southeast1.firebasedatabase.app",
    "projectId": "scribex-1653106340524",
    "storageBucket": "scribex-1653106340524.appspot.com",
    "serviceAccount": "./app/keySX.json"
}




class FBStorage(object):
    def __init__(self, userid):
        self.obj_file = File(userid)
        self.obj_app = App()
        self.storage = self.obj_app.firebase_storage.storage()
    
    def upload__file(self, transcribedfile):
        #  Upload result txt file into firebase storage
        self.storage.child("transcribed/"+self.obj_file.get_userid()+"/"+transcribedfile).put(transcribedfile)
        os.remove(transcribedfile)
    

    def update_database(self, key):
        data = {"id": key}
        # Add  fields into specified document in firestore database
        return self.obj_file.col_ref.document(key).set(data)

    def transcribe_QUEUE(self):
        imgdict = self.obj_file.get_QUEUE()
        keylst = imgdict.keys()

        for key in keylst:
            imgurl = imgdict[key]
            bytes = self.obj_file.get_img(imgurl)

            #  Transcribe
            transcribedfile = self.obj_app.ocrfn(key, bytes)
            #  Upload file to Firebase storage
            self.upload__file(transcribedfile)
            #  Update firestore datatbase
            self.update_database(key)
            


class File(object):
    def __init__(self, userid):
        self.userid = userid
        self.bucket = storage.bucket()
        self.database = firestore.client()
        self.col_ref = self.database.collection("users/"+self.get_userid()+"/transcribed")
        self.queue = self.database.collection("users/"+self.get_userid()+"/uploads").document("QUEUE")
    
    def get_QUEUE(self):
        queuelst = self.queue.get()
        imagedict = queuelst.to_dict()
        return imagedict

    def get_userid(self):
        return self.userid


    def get_img(self, imgurl):
        #  Retrieve image from firebase storage via url
        with urllib.request.urlopen(imgurl) as resp:
            # read image as an numpy array
            im = np.asarray(bytearray(resp.read()), dtype="uint8")
            #  convert nparray to bytes for google cloud vision to construct image
            byte = im.tobytes()
        return byte

            
#Singleton object representing connection to database
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
        self.firebase_storage = pyrebase.initialize_app(config)
        self.env = os.environ['GOOGLE_APPLICATION_CREDENTIALS'] = r'./app/keySX.json'
        self.client = vision_v1.ImageAnnotatorClient()
        type(self).__inited = True

    def googlevision(self, bytes):
        image = vision_v1.types.Image(content=bytes)
        resp = self.client.document_text_detection(image)
        text = resp.full_text_annotation.text
        return text

    def newtxtfile(self, key, text):
        filename = key+".txt"
        # A text file is created
        file = open(filename, "w+")
        # Appending the text into file
        file.write(text)
        file.close()
        return filename

    def ocrfn(self, key, bytes):
        text = self.googlevision(bytes)
        filename = self.newtxtfile(key, text)
        return filename



app = FastAPI()

@app.get("/app/{userid}")
def main(userid):
    app_instance = App()
    storage_instance = FBStorage(userid)
    storage_instance.transcribe_QUEUE()


if __name__ == "__main__":
    main("lc6h9J5fkif0iDaGUxZe6IJ6Bq53")





