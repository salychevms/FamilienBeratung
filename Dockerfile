# =========================
# STAGE 1: BUILD
# =========================
FROM maven:3.9.9-eclipse-temurin-21 AS builder   # ADDED

WORKDIR /build                                   # ADDED

COPY pom.xml .                                   # ADDED
RUN mvn -B dependency:go-offline                 # ADDED

COPY src ./src                                   # ADDED

RUN mvn clean package -Pproduction -DskipTests   # ADDED


# =========================
# STAGE 2: RUNTIME
# =========================
FROM eclipse-temurin:21-jre-jammy                # CHANGED (kept base)

# оставляем твои диагностические инструменты
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

# CHANGED: копируем jar из builder stage
COPY --from=builder /build/target/*.jar /app.jar   # CHANGED

ENTRYPOINT ["java", "-jar", "/app.jar"]
