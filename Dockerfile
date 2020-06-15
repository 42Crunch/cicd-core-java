FROM adoptopenjdk/maven-openjdk8:latest
WORKDIR /build
COPY pom.xml .
RUN mvn dependency:go-offline dependency:resolve-plugins
COPY src/ /build/src/
RUN mvn test
RUN mvn spotbugs:check
