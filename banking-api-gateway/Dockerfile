FROM amazoncorretto:17
WORKDIR /app

RUN yum install -y curl && yum clean all

COPY target/banking-api-gateway-0.0.1-SNAPSHOT.jar app.jar

EXPOSE 8080

HEALTHCHECK --interval=30s --timeout=5s --start-period=30s --retries=5 CMD curl -fsS http://localhost:8080/actuator/health || exit 1

ENTRYPOINT ["java", "-Xms256m", "-Xmx512m", "-jar", "/app/app.jar"]
