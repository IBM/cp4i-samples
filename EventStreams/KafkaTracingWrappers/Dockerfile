FROM ibmjava:jre
RUN mkdir /opt/app
COPY target/KafkaTracingWrappers-1.0.0-jar-with-dependencies.jar /opt/app
ADD dockerfile.resources /opt/app
ARG PROJECT
ENV JAEGER_SERVICE_NAME myservicename
ENV JAEGER_AGENT_HOST localhost
ENV JAEGER_AGENT_PORT 6831
ENV JAEGER_SAMPLER_TYPE const
ENV JAEGER_SAMPLER_PARAM 1
ENV JAEGER_TAGS sourceNamespace=$PROJECT
WORKDIR /opt/app
CMD ["java", "-jar", "KafkaTracingWrappers-1.0.0-jar-with-dependencies.jar"]