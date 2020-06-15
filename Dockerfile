FROM adoptopenjdk/maven-openjdk8:latest
ARG TEST_API_KEY
ENV TEST_API_KEY ${TEST_API_KEY}
WORKDIR /build
COPY pom.xml .
RUN mvn dependency:go-offline dependency:resolve-plugins
COPY src/ /build/src/
RUN mvn test
RUN mvn spotbugs:check
