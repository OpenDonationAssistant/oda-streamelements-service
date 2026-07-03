FROM fedora:44
WORKDIR /app
COPY target/oda-streamelements-service /app

CMD ["./oda-streamelements-service"]
