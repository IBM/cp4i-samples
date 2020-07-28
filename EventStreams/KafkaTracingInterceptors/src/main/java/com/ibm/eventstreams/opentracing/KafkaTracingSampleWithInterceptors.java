/**
 * Copyright 2020 IBM Corporation
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */
package com.ibm.eventstreams.opentracing;

import io.jaegertracing.Configuration;
import io.opentracing.Tracer;
import io.opentracing.util.GlobalTracer;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.time.Duration;
import java.util.Arrays;
import java.util.Properties;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.atomic.AtomicBoolean;

import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.ProducerRecord;

public class KafkaTracingSampleWithInterceptors {
  private static final String TOPIC_NAME = "opentracing-topic";
  private static final String RECORD_VALUE = "{\"accounts\":{\"id\":\"314159\"}}";
  private static final int CONSUME_DURATION_MS = 10000;

  private static AtomicBoolean stopNow = new AtomicBoolean(); // Whether stop has been requested

  public static void main(final String[] args) throws InterruptedException, IOException, ExecutionException {
    System.out.println("Kafka OpenTracing sample started");

    final Thread closingHook = new Thread(() -> stopNow.set(true));
    Runtime.getRuntime().addShutdownHook(closingHook);

    // Load all of the properties shared between producers and consumers from the kafka.properties file
    try (InputStream is = new FileInputStream("kafka.properties")) {
      Properties producerProps = new Properties();
      producerProps.load(is);

      Properties consumerProps = new Properties();
      consumerProps.putAll(producerProps);

      // Add in the producer-specific and consumer-specific properties
      producerProps.setProperty("key.serializer", "org.apache.kafka.common.serialization.StringSerializer");
      producerProps.setProperty("value.serializer", "org.apache.kafka.common.serialization.StringSerializer");
      producerProps.setProperty("acks", "all");
      consumerProps.setProperty("key.deserializer", "org.apache.kafka.common.serialization.StringDeserializer");
      consumerProps.setProperty("value.deserializer", "org.apache.kafka.common.serialization.StringDeserializer");
      consumerProps.setProperty("group.id", "opentracing-sample-intercept");
      consumerProps.setProperty("enable.auto.commit", "true");
      consumerProps.setProperty("auto.commit.interval.ms", "1000");

      // If tracing is configured, enable it
      if (System.getenv("JAEGER_SERVICE_NAME") != null) {
        final Tracer tracer = Configuration.fromEnv().getTracer();
        GlobalTracer.registerIfAbsent(tracer);

        // And add the tracing interceptors to the configuration
        producerProps.setProperty("interceptor.classes", "io.opentracing.contrib.kafka.TracingProducerInterceptor");
        consumerProps.setProperty("interceptor.classes", "io.opentracing.contrib.kafka.TracingConsumerInterceptor");
      }

      System.out.println("Creating Kafka producer");
      try (KafkaProducer<String, String> producer = new KafkaProducer<>(producerProps)) {

        System.out.println("Creating Kafka consumer");
        try (KafkaConsumer<String, String> consumer = new KafkaConsumer<>(consumerProps)) {
          consumer.subscribe(Arrays.asList(TOPIC_NAME));

          while (!stopNow.get()) {
            // Send a record and wait for it to be acknowledged
            System.out.println("Sending a record...");
            producer.send(new ProducerRecord<String, String>(TOPIC_NAME, RECORD_VALUE)).get();

            // Consume the record (and any other records that happen to be waiting)
            System.out.println("Consuming records...");
            ConsumerRecords<String, String> records = consumer.poll(Duration.ofMillis(CONSUME_DURATION_MS));
            while (!records.isEmpty()) {
              for (ConsumerRecord<String, String> record : records) {
                System.out.println("Consumed a record: " + record.value());
              }

              records = consumer.poll(Duration.ofMillis(CONSUME_DURATION_MS));
            }

            // Waiting to go round again
            System.out.println("Waiting a while...");
            Thread.sleep(CONSUME_DURATION_MS);
          }
        }
      }
    }
  }
}