import pytesseract
from pytesseract import Output
import cv2

pytesseract.pytesseract.tesseract_cmd = r"C:\Program Files\Tesseract-OCR\tesseract.exe"



def turn_img_into_txt(img, key):
	output = []
	filename = key+".txt"

	# load the input image, convert it from BGR to RGB channel ordering,
	# and use Tesseract to localize each area of text in the input image
	# image = cv2.imread(imgpath)
	rgb = cv2.cvtColor(img, cv2.COLOR_BGR2RGB)
	results = pytesseract.image_to_data(rgb, output_type=Output.DICT)

	# loop over each of the individual text localizations
	for i in range(0, len(results["text"])):
		# extract the OCR text itself along with the confidence of the
		# text localization
		text = results["text"][i]

		conf = int(float(results["conf"][i]))
		# filter out weak confidence text localizations
		if conf > 0.5:
			text = "".join([c if ord(c) < 128 else "" for c in text]).strip()
			output.append(text+" ")
	output = ''.join(output)

	# A text file is created
	file = open(filename, "w+")
	# Appending the text into file
	file.write(output)
	file.close()
	return filename
