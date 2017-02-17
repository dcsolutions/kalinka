/*
Copyright [2017] [DCS <Info-dcs@diehl.com>]

Licensed under the Apache License, Version 2.0 (the "License");
you may not use this file except in compliance with the License.
You may obtain a copy of the License at

    http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License.
*/
package org.diehl.dcs.kalinka.it.model;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


public class KafkaProducerClient {

	private static final Logger LOG = LoggerFactory.getLogger(KafkaProducerClient.class);

	private final KafkaProducer<String, byte[]> producer;
	private final String kafkaHosts = "192.168.33.20:9092,192.168.33.21:9092,192.168.33.22:9092";
	private List<String> clients = new ArrayList<>();
	private final long intervalInMillis;
	private int publishCounter = 0;

	private volatile boolean stopped = false;

	private final ExecutorService execService = Executors.newSingleThreadExecutor();
	private Future<?> future;

	public KafkaProducerClient(final List<String> clients, final long intervalInMillis) {
		this.clients = clients;
		this.intervalInMillis = intervalInMillis;
		final Map<String, Object> props = new HashMap<>();
		props.put("bootstrap.servers", kafkaHosts);
		props.put("key.serializer", "org.apache.kafka.common.serialization.StringSerializer");
		props.put("value.serializer", "org.apache.kafka.common.serialization.ByteArraySerializer");
		producer = new KafkaProducer<>(props);
	}

	private void doPublish() {
		while (!stopped) {
			try {
				for (final String client : clients) {
					publish(client);
				}
				Thread.sleep(intervalInMillis);
			} catch (final Throwable t) {
				LOG.error("exception while publishing", t);
			}
		}
	}

	private void doPublishWithFixExecutions(final int numberOfExecutions) {
		for (int i = 0; i < numberOfExecutions; i++) {
			try {
				for (final String client : clients) {
					publish(client);
				}
				Thread.sleep(intervalInMillis);
			} catch (final Throwable t) {
				LOG.error("exception while publishing", t);
			}
		}
	}

	private void publish(final String client) {
		LOG.info("publishing message from kafka to {}", client);
		final ProducerRecord<String, byte[]> producerRecord =
				new ProducerRecord<>("0.sparkcluster.mqtt", client, new String("Regards from Kafka to " + client).getBytes());
		producer.send(producerRecord);
		publishCounter++;
	}

	public void start() {
		future = execService.submit(() -> doPublish());
	}

	public void stop() {
		execService.shutdown();
		future = null;
		this.stopped = false;
	}

	public int getPublishCounter() {
		return this.publishCounter;
	}

	public void startWithFixExecutions(final int numberOfExecutions) {
		future = execService.submit(() -> doPublishWithFixExecutions(numberOfExecutions));
	}

	public boolean isDone() {
		return future == null ? true : this.future.isDone();
	}
}
