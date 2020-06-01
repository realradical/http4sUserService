FROM openjdk:11-slim as build-stage

ENV sbt_version 1.3.7
ENV sbt_home /usr/local/sbt
ENV PATH ${PATH}:${sbt_home}/bin

# Install sbt
RUN apt-get update && \
    apt-get install -y wget && \
    mkdir -p $sbt_home && \
    wget -qO - --no-check-certificate https://github.com/sbt/sbt/releases/download/v$sbt_version/sbt-$sbt_version.tgz | tar xz -C $sbt_home --strip-components=1 && \
    sbt sbtVersion

ENV BUILDER_HOME=/home/builder
RUN useradd -ms /bin/bash -d ${BUILDER_HOME} builder

WORKDIR ${BUILDER_HOME}/app
COPY ./ ${BUILDER_HOME}/app/

RUN chown -R builder: ${BUILDER_HOME}

USER builder

RUN sbt -mem 2048 assembly exit

#
# Docker File for Scala Application microservice
#
FROM openjdk:11.0.7-jre-slim

# Make directory for the run scripts
RUN mkdir -p /root/scripts

# Define working directory.
WORKDIR /root

# Copy assembled Fat JAR to working directory from building stage
COPY --from=build-stage /home/builder/app/target/scala-2.13/http4sUserService.jar /root

# Copy the run scripts and target folder
COPY scripts/* /root/scripts/
COPY . /root/

# Add permission
RUN chmod +x /root/scripts/run.sh

# Trigger Docker Instance startup
ENTRYPOINT ["/root/scripts/run.sh"]