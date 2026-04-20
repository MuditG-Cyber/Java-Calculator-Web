FROM eclipse-temurin:17-jdk-alpine
WORKDIR /app
COPY . /app
RUN javac CalculatorServer.java
CMD ["java", "CalculatorServer"]
