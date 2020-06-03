FROM ibmjava:jre
RUN mkdir /opt/app
COPY target/KafkaTracingInterceptors-1.0.0-jar-with-dependencies.jar /opt/app
ADD dockerfile.resources /opt/app
ARG PROJECT
ENV JAEGER_SERVICE_NAME myservicename
ENV JAEGER_AGENT_HOST localhost
ENV JAEGER_AGENT_PORT 6831
ENV JAEGER_SAMPLER_TYPE const
ENV JAEGER_SAMPLER_PARAM 1
ENV JAEGER_TAGS sourceNamespace=$PROJECT,externalAppType=myexternalapp,businessId=12345
WORKDIR /opt/app
CMD ["java", "-jar", "KafkaTracingInterceptors-1.0.0-jar-with-dependencies.jar"]