FROM eclipse-temurin:21-jre-jammy

# обновляем систему + ставим инструменты диагностики
RUN apt-get update && apt-get install -y --no-install-recommends \
    iputils-ping \
    traceroute \
    iproute2 \
    dnsutils \
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

# ENV для хранения файлов
ENV FILE_STORAGE=/data/documents

WORKDIR /app

# Копируем JAR
COPY target/*.jar app.jar

# создаём пользователя (лучше, чем root)
RUN useradd -m appuser
USER appuser

ENTRYPOINT ["java", "-jar", "/app/app.jar"]