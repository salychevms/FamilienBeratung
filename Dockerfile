FROM eclipse-temurin:21-jre-jammy

# обновляем систему + ставим инструменты диагностики
RUN apt-get update && apt-get install -y --no-install-recommends \
    iputils-ping \
    traceroute \
    iproute2 \
    net-tools \
    curl \
    wget \
    nano \
    htop \
    tcpdump \
    telnet \
    unzip \
    procps \
    vim \
    && apt-get clean && rm -rf /var/lib/apt/lists/*

WORKDIR /data

# Копируем JAR
COPY target/*.jar /app/app.jar

ENTRYPOINT ["java", "-jar", "/app/app.jar"]