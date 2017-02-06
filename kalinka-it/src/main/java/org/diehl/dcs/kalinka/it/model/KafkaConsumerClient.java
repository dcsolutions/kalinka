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

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.Executors;

import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author Merkaban <ridethesnake7@yahoo.de>
 *
 */
public class KafkaConsumerClient {
	private static final Logger LOG = LoggerFactory.getLogger(KafkaConsumerClient.class);

	private final KafkaConsumer<String, byte[]> consumer;
	private final String kafkaHosts = "192.168.33.20:9092,192.168.33.21:9092,192.168.33.22:9092";

	private int publishCounter = 0;

	private volatile boolean stopped = false;

	public KafkaConsumerClient() {
		final Map<String, Object> props = new HashMap<>();
		props.put("bootstrap.servers", kafkaHosts);
		props.put("key.deserializer", "org.apache.kafka.common.serialization.StringDeserializer");
		props.put("value.deserializer", "org.apache.kafka.common.serialization.ByteArrayDeserializer");
		props.put("group.id", "testConsumers");
		consumer = new KafkaConsumer<>(props);
		consumer.subscribe(Collections.singletonList("mqtt.sparkcluster"));
		LOG.info("Assigning to topic=mqtt.sparkcluster");
	}

	private void doPublish() {
		while (!stopped) {
			try {
				for (final String client : clients) {
					LOG.info("publishing message from kafka to {}", client);
					final ProducerRecord<String, byte[]> producerRecord =
							new ProducerRecord<>("sparkcluster.mqtt", client, new String("Regards from Kafka to " + client).getBytes());
					producer.send(producerRecord);
					publishCounter++;
				}
				Thread.sleep(intervalInMillis);
			} catch (final Throwable t) {
				LOG.error("exception while publishing", t);
			}

		}
	}

	public void start() {
		this.stopped = false;
		try {
			Executors.newSingleThreadExecutor().submit(() -> doPublish());
		} catch (final Throwable t) {
			LOG.error("exception during execution", t);
		}
	}

	public void stop() {
		this.stopped = true;
	}

	public int getPublishCounter() {
		return this.publishCounter;
	}
}
