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
    locales \
        && locale-gen de_DE.UTF-8 \
        && update-locale LANG=de_DE.UTF-8 \
        && apt-get clean && rm -rf /var/lib/apt/lists/*

WORKDIR /app

# Копируем JAR
COPY target/*.jar /app.jar

ENTRYPOINT ["java", "-jar", "/app.jar"]