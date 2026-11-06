# Java servisleri için ortak imaj tanımı. Kullanım: --build-arg MODULE=shipment-service (bkz. deploy/prod/docker-compose.yml)
# Derleme bağlamı depo kökündür. Testler ve kod denetimleri (Checkstyle, PMD, Spotless, JaCoCo) CI'da çalışır; imaj derlemesinde atlanır.
FROM eclipse-temurin:25-jdk AS build
ARG MODULE
WORKDIR /src
COPY . .
RUN --mount=type=cache,target=/root/.m2 \
    ./mvnw -B -q -pl services/${MODULE} -am package -DskipTests \
      -Dspotless.check.skip=true -Dcheckstyle.skip=true -Dpmd.skip=true -Djacoco.skip=true \
 && cp services/${MODULE}/target/${MODULE}-*.jar /app.jar

FROM eclipse-temurin:25-jre
RUN useradd --system --uid 10001 --create-home app
USER app
WORKDIR /home/app
COPY --from=build /app.jar app.jar
ENV TZ=UTC
# Üretimde `local` profili AÇILMAZ (MFA'yı kapatır ve örnek veri yükler).
ENTRYPOINT ["java", "-XX:MaxRAMPercentage=75", "-jar", "app.jar"]
