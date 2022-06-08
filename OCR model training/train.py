# set the matplotlib backend so figures can be saved in the background
import matplotlib
matplotlib.use("Agg")
# import the necessary packages
import callbacks
from models import ResNet
import parse_data
from tensorflow.keras.preprocessing.image import ImageDataGenerator
from tensorflow.keras.optimizers import SGD
from sklearn.preprocessing import LabelBinarizer
from sklearn.model_selection import train_test_split
from sklearn.metrics import classification_report
from imutils import build_montages
import matplotlib.pyplot as plt
import numpy as np
import argparse
import cv2

# initialize the number of epochs to train for, initial learning rate,
# and batch size
EPOCHS = 50
INIT_LR = 1e-1
BS = 64
TGT_ACC = 0.95
BAL_LABELS = "0123456789ABCDEFGHIJKLMNOPQRSTUVWXYZabdefghnqrt"
CLS_LABELS = "0123456789ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz"

#load datasets
print("[INFO] loading datasets...")
(traindata, trainlabels) = parse_data.load_data("EMNIST_dataset\emnist-balanced-train.csv")
(testdata, testlabels) = parse_data.load_data("EMNIST_dataset\emnist-balanced-test.csv")


#resize and preprocessing for datasets
traindata = np.array([cv2.resize(image, (32,32)) for image in traindata], dtype="float32")
traindata = np.expand_dims(traindata, axis=-1)
traindata /= 255.0

testdata = np.array([cv2.resize(image, (32,32)) for image in testdata], dtype="float32")
testdata = np.expand_dims(testdata, axis=-1)
testdata /= 255.0


# convert the labels from integers to vectors
le = LabelBinarizer()
trainlabels = le.fit_transform(trainlabels)
testlabels = le.fit_transform(testlabels)
counts = trainlabels.sum(axis=0)

# account for skew in the labeled data
classTotals = trainlabels.sum(axis=0)
classWeight = {}

# loop over all classes and calculate the class weight
for i in range(0, len(classTotals)):
	classWeight[i] = classTotals.max() / classTotals[i]

aug = ImageDataGenerator(rotation_range=10, zoom_range=0.05, width_shift_range=0.1, height_shift_range=0.1, shear_range=0.15, horizontal_flip=False, fill_mode="nearest")

# initialize and compile
print("[INFO] compiling model...")
opt = SGD(learning_rate=INIT_LR)
model = ResNet.build(32, 32, 1, len(le.classes_), (3, 3, 3), (64, 64, 128, 256), reg=0.0005)
model.compile(loss="categorical_crossentropy", optimizer=opt, metrics=["accuracy"])

accCallback = callbacks.accuracyCallback(TGT_ACC)
decay = callbacks.customLearningRateScheduler(callbacks.lr_schedule)

# train the network
print("[INFO] training network...")
H = model.fit(
	aug.flow(traindata, trainlabels, batch_size=BS),
	validation_data=(testdata, testlabels),
	steps_per_epoch=len(traindata) // BS,
	epochs=EPOCHS,
	class_weight=classWeight,
	verbose=1,
	callbacks=[accCallback, decay])
# define the list of label names
labelNames = BAL_LABELS
labelNames = [l for l in labelNames]
# evaluate the network
print("[INFO] evaluating network...")
predictions = model.predict(testdata, batch_size=BS)
print(classification_report(testlabels.argmax(axis=1),
	predictions.argmax(axis=1), target_names=labelNames))

model.save("models\model.h5",save_format="h5")

# construct a plot that plots and saves the training history
# N = np.arange(0, EPOCHS)
# plt.style.use("ggplot")
# plt.figure()
# plt.plot(N, H.history["loss"], label="train_loss")
# plt.plot(N, H.history["val_loss"], label="val_loss")
# plt.title("Training Loss and Accuracy")
# plt.xlabel("Epoch #")
# plt.ylabel("Loss/Accuracy")
# plt.legend(loc="lower left")
# initialize our list of output test images
images = []
# randomly select a few testing characters
for i in np.random.choice(np.arange(0, len(testlabels)), size=(49,)):
	# classify the character
	probs = model.predict(testdata[np.newaxis, i])
	prediction = probs.argmax(axis=1)
	label = labelNames[prediction[0]]
	# extract the image from the test data and initialize the text
	# label color as green (correct)
	image = (testdata[i] * 255).astype("uint8")
	color = (0, 255, 0)
	# otherwise, the class label prediction is incorrect
	if prediction[0] != np.argmax(testlabels[i]):
		color = (0, 0, 255)
	# merge the channels into one image, resize the image from 32x32
	# to 96x96 so we can better see it and then draw the predicted
	# label on the image
	image = cv2.merge([image] * 3)
	image = cv2.resize(image, (96, 96), interpolation=cv2.INTER_LINEAR)
	cv2.putText(image, label, (5, 20), cv2.FONT_HERSHEY_SIMPLEX, 0.75,
		color, 2)
	# add the image to our list of output images
	images.append(image)
# construct the montage for the images
montage = build_montages(images, (96, 96), (7, 7))[0]
# show the output montage
cv2.imshow("OCR Results", montage)
cv2.waitKey(0)