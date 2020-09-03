FROM adoptopenjdk:11-jre-hotspot

# Metadata
LABEL module.name="${project.artifactId}" \
      module.version="${project.version}"

COPY [ "${project.artifactId}-${project.version}-exec.jar", "/app.jar" ]

RUN sh -c 'touch /app.jar'

VOLUME [ "/tmp" ]

ENV JAVA_OPTS="$JAVA_OPTS -Xms256M -Xmx1G"

EXPOSE 8080

ENTRYPOINT [ "sh", "-c", "java $JAVA_OPTS -Djava.security.egd=file:/dev/./urandom -jar /app.jar" ]
