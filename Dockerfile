FROM amazoncorretto:19

ARG JAR_FILE=build/libs/checkmarkscli-1.0-SNAPSHOT-all.jar
ARG CRON_FILE=cronfile
ARG SECRET_FILE=secret.json
ARG SCRIPT=entrypoint.sh

# Copy files accross
COPY ${JAR_FILE} app.jar
COPY ${CRON_FILE} cronfile
COPY ${SECRET_FILE} secret.json
COPY ${SCRIPT} entrypoint.sh

# Make script executable
RUN chmod +x ${SCRIPT}

# endless loop
ENTRYPOINT ["tail", "-f", "/dev/null"]
