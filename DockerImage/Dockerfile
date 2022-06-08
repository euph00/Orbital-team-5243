FROM python:3.7-slim

COPY requirements.txt .

RUN pip install -r requirements.txt

RUN apt-get update && apt-get install tesseract-ocr -y

RUN mkdir -p app

COPY ./app app
 
EXPOSE 80

CMD ["uvicorn", "app.app:app", "--host", "0.0.0.0", "--port", "80"]