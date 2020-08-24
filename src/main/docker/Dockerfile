FROM adoptopenjdk:11-jre-hotspot

# Metadata
LABEL module.name="${project.artifactId}" \
      module.version="${project.version}"

COPY [ "${project.artifactId}-${project.version}-exec.jar", "/app.jar" ]

RUN sh -c 'touch /app.jar'

VOLUME [ "/tmp" ]

ENV JAVA_OPTS="$JAVA_OPTS -Xms256M -Xmx1G" \
    SERVER_PORT=8080

EXPOSE ${SERVER_PORT}

ENTRYPOINT [ "sh", "-c", "java $JAVA_OPTS -Djava.security.egd=file:/dev/./urandom -Dspring.profiles.active=mysql -jar /app.jar" ]
