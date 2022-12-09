FROM ubuntu:20.04

WORKDIR /app

ENV TZ=Asia/Ho_Chi_Minh
RUN ln -snf /usr/share/zoneinfo/$TZ /etc/localtime && echo $TZ > /etc/timezone
RUN apt clean 
RUN apt update -y

RUN apt install g++ -y
RUN apt install gcc -y
RUN apt install python3-dev -y
RUN apt install python3-pip -y

RUN apt update -y
RUN apt install libmysqlclient-dev -y

COPY requirements.txt .

RUN pip install -r requirements.txt

COPY . .

CMD ["python3", "manage.py", "runserver", "0.0.0.0:8000"]
