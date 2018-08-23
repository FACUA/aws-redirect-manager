FROM alpine:3.7 as build

RUN apk add --no-cache openjdk8=8.171.11-r0

WORKDIR /app

COPY build.gradle .
COPY settings.gradle .
COPY gradlew .
COPY gradle/ ./gradle

RUN ./gradlew --no-daemon dependencies

COPY src/ ./src

RUN ./gradlew --no-daemon productionBuild

RUN mkdir -p dist && \
	cp build/libs/*.jar dist/app.jar && \
	echo "#!/usr/bin/env bash" >> dist/aws-redirect-manager && \
	echo "exec java -jar \$0 \"\$@\"" >> dist/aws-redirect-manager && \
	echo "" >> dist/aws-redirect-manager && \
	echo "" >> dist/aws-redirect-manager && \
	cat dist/app.jar >> dist/aws-redirect-manager && \
	chmod +x dist/aws-redirect-manager

FROM alpine:3.7 as exportg

VOLUME /out

COPY --from=build /app/dist/aws-redirect-manager ./

RUN echo "#!/bin/sh" >> export && \
	echo "cp aws-redirect-manager /out" >> export && \
	chmod +x export

CMD ["/export"]
