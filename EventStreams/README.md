# Event Streams samples
There are two sample applications provided to demonstrate sending OpenTracing data into the Operations Dashboard as an external application. They demonstrate two different ways to integrate OpenTracing into your own Kafka applications.

* [KafkaTracingInterceptors](KafkaTracingInterceptors/README.md) which shows how to use Kafka client interceptors to send OpenTracing data
* [KafkaTracingWrappers](KafkaTracingWrappers/README.md) which shows to use wrappers for the Kafka client classes

![Master Arch Diagram](images/architecture.png)

The applications are provided as source code for your to build and deploy on your OpenShift Container Platform cluster where IBM Cloud Pak for Integration is installed.
