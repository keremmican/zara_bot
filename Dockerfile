FROM openjdk:17-jdk-slim

# Çalışma dizinini ayarla
WORKDIR /app

# JAR dosyasını kopyala
COPY target/zara-0.0.1-SNAPSHOT.jar app.jar

# Uygulamayı çalıştır
ENTRYPOINT ["java", "-jar", "app.jar"]
