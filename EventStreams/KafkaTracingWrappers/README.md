# Event Streams OpenTracing sample using Kafka client wrapper classes
The sample demonstrates sending OpenTracing data into the Operations Dashboard as an external application. It uses OpenTracing wrapper classes with the Kafka client.

![Master Arch Diagram](../images/architecture.png)

The application is provided as source code for your to build and deploy on your OpenShift Container Platform cluster where IBM Cloud Pak for Integration is installed.

To build and deploy the applications, you must have the following installed:
* [git](https://git-scm.com/)
* [Maven 3.0 or later](https://maven.apache.org)
* Java 8 or later
* Docker
* [OpenShift Container Platform command-line interface](https://docs.openshift.com/container-platform/4.4/cli_reference/openshift_cli/getting-started-cli.html) (oc)

You will need to be proficient with Docker and the OpenShift CLI. The instructions are written assuming you are using Linux or MacOS and that the OpenShift workers are using the x86_64 processor architecture.

## Building the sample application
First, clone the repository with the following command:
``` shell
$ git clone https://github.com/IBM/cp4i-samples.git
```

Change directory into the `cp4i-samples/EventStreams/KafkaTracingWrappers` directory:
``` console
$ cd cp4i-samples/EventStreams/KafkaTracingWrappers
```

Build the connector using Maven:
``` shell
$ mvn clean package
```

Once built, the output is a single JAR `target/KafkaTracingWrappers-1.0.0-jar-with-dependencies.jar` which contains all of the required dependencies.

## Configuring the application to connect to Event Streams
The application obtains its configuration from a file in its current directory called `kafka.properties`. When you build the Docker image for the application, all of the files in a directory called `dockerfile.resources` are copied into the image so the configuration is available to the application when it runs on OpenShift.

Create a directory called `dockerfile.resources` and copy the `kafka.properties` file into it:

``` shell
$ mkdir dockerfile.resources
$ cp template/kafka.properties dockerfile.resources
```

In Event Streams, create a topic called `opentracing-topic` and credentials for your application to produce and consume messages on this topic.

The way you have configured Event Streams will affect exactly what configuration information is needed, but the example `kafka.properties` provided has placeholders for:

* Bootstrap server
* Truststore location
* Truststore password
* User name
* Password

All of these can be obtained from the Event Streams UI in the "Connect to cluster" tool, or from the command-line interface.

Edit the `dockerfile.resources/kafka.properties` file to supply the required data. Also, download the truststore file for Event Streams and place it in the `dockerfile.resources` directory, making sure to change the truststore location to the correct file name.

## Building and uploading the Docker image to OpenShift
Log in to the OpenShift CLI in the usual way:
``` shell
$ oc login --token=<bearer token> --server=https://<host:port of Kubernetes API server>
```

Find the hostname for your OpenShift cluster's image registry:
``` shell
$ oc get routes -n openshift-image-registry
```

Log in to the image registry, replacing `<image registry hostname>` with your registry's hostname:
``` shell
$ docker login -u username -p $(oc whoami -t) https://<image registry hostname>
```

You'll need a project (or namespace) to work in. Either switch to the project or create a new one like this, replacing `<project>` with the name of your project:
``` shell
$ oc new-project <project>
```

Build the Docker image for the application, providing the name of your project as a build argument:
``` shell
$ docker build -t kafkatracingsamplewrap:latest --build-arg PROJECT=<project> .
```

Tag the image ready for pushing to the image registry:
``` shell
$ docker tag kafkatracingsamplewrap:latest <image registry hostname>/<project>/kafkatracingsamplewrap:latest
```

And push it:
``` shell
$ docker push <image registry hostname>/<project>/kafkatracingsamplewrap:latest
```

List the image streams in your namespace:
``` shell
$ oc get imagestream
NAME                     IMAGE REPOSITORY                                                               TAGS     UPDATED
kafkatracingsamplewrap   default-route-openshift-image-registry...../<project>/kafkatracingsamplewrap   latest   10 seconds ago
```

## Upload the Operations Dashboard images to OpenShift
Looking at the diagram earlier, you can see that it's necessary to include two sidecar containers alongside your application so it can send data to the Operations Dashboard. You can obtain the images from the IBM Entitled Registry using an entitlement key, just the same as the images for Cloud Pak for Integration.

It's easiest to add the images to your project in the OpenShift image registry to keep all of the pieces of the application together.

First, log in to the IBM Entitled Registry using your entitlement key:
``` shell
$ docker login cp.icr.io --username cp --password <IBM Entitled Registry key>
```

Then, pull the images for the OD agent and collector into your local Docker registry:

``` shell
$ docker pull cp.icr.io/cp/icp4i/ace/icp4i-od-agent:latest
$ docker pull cp.icr.io/cp/icp4i/ace/icp4i-od-collector:latest
```

Tag them ready for pushing to the OpenShift image registry:
``` shell
$ docker tag cp.icr.io/cp/icp4i/ace/icp4i-od-agent:latest <image registry hostname>/<project>/icp4i-od-agent:latest
$ docker tag cp.icr.io/cp/icp4i/ace/icp4i-od-collector:latest <image registry hostname>/<project>/icp4i-od-collector:latest
```

And push them:
``` shell
$ docker push <image registry hostname>/<project>/icp4i-od-agent:latest
$ docker push <image registry hostname>/<project>/icp4i-od-collector:latest
```

## Deploy the application with the agent and collector sidecar containers
The `templates/deployment.yaml` file contains the definition of the Kubernetes deployment that runs the application in a pod with the sidecar containers.

To customise the deployment for your environment, you can edit the file or use the `deploy.sh` script provided.

You will need to know the namespace in which the Operations Dashboard is running. If you're not sure, you can use the following command to list the pods:

``` shell
$ oc get services --all-namespaces | grep od-store-od
```

The output will be something like this, which shows the namespace is called `tracing`.
```
tracing           od-store-od                  ClusterIP      172.30.223.204   <none>             9200/TCP,9300/TCP               63d
tracing           od-store-od-headless         ClusterIP      None             <none>             9200/TCP                   63d
```

Now you can use the following command to deploy the application:

``` shell
$ ./deploy.sh <project name> <OD namespace>
```

The Kafka application will start producing and consuming records on the topic `opentracing-topic`. The pod created has 3 containers, but the tracing agent and collector will not start until the next step has been completed.

``` shell
$ oc get pods
NAME                                                    READY     STATUS                       RESTARTS   AGE
icp4i-od-external-app-kafka-wrappers-59b78ff7b9-vwltd   1/3       CreateContainerConfigError   0          94s
```

## Complete the registration process for the application
The final stage is to register applications running in your project (or namespace) to send data to the Operations Dashboard.

*This process only needs to be carried out once per namespace. If you run a second application in the namespace, it is not necessary to register a second time.*

You can use the following command to begin the registration process:

``` shell
./registration.sh <project name> <OD namespace>
```

This starts a job which calls a REST API that begins the registration. Now you must go into the Operations Dashboard Web Console to approve the registration request. Once you have approved the request, you will see data appear in the Operations Dashboard Web Console.

## Cleaning up
The application will continue to run producing and consuming one event every 20 seconds. To stop the application, delete the deployment.

``` shell
oc delete deployment icp4i-od-external-app-kafka-wrappers
```

You might also want to clean up the registration job once it has completed.

``` shell
oc delete job icp4i-od-external-app-registration
```