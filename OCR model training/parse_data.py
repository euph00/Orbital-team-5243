import numpy as np

def rotate(image):
    image = image.reshape((28, 28))
    image = np.fliplr(image)
    image = np.rot90(image)
    return image.reshape((28, 28))

def load_data(path):
    data = []
    labels = []

    for row in open(path):
        row = row.split(",")
        #class labels are first entry in each row
        label = int(row[0]) 
        #remaining entries in row are flattened rgb values
        image = np.array([int(x) for x in row[1:]], dtype="uint8")
        #reshape flattened array into 28x28
        image = rotate(image)

        data.append(image)
        labels.append(label)
    
    #convert to np arrays
    data = np.array(data, dtype="float32")
    labels = np.array(labels, dtype="int")
    return (data, labels)

if __name__ == "__main__":
    dat = load_data("EMNIST_dataset\emnist-balanced-train.csv")
    print(dat[1][0])