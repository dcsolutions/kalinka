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
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.common.PartitionInfo;
import org.apache.kafka.common.TopicPartition;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author Merkaban <ridethesnake7@yahoo.de>
 *
 */
public class KafkaConsumerClient {
	private static final Logger LOG = LoggerFactory.getLogger(KafkaConsumerClient.class);

	private KafkaConsumer<String, byte[]> consumer;
	private final String kafkaHosts = "192.168.33.20:9092,192.168.33.21:9092,192.168.33.22:9092";

	private volatile boolean stopped = false;
	private final ExecutorService execService = Executors.newSingleThreadExecutor();
	private final Set<ConsumerRecord<String, byte[]>> recordSet = new HashSet<>();

	public KafkaConsumerClient() {
		final Map<String, Object> props = new HashMap<>();
		props.put("bootstrap.servers", kafkaHosts);
		props.put("key.deserializer", "org.apache.kafka.common.serialization.StringDeserializer");
		props.put("value.deserializer", "org.apache.kafka.common.serialization.ByteArrayDeserializer");
		props.put("group.id", "testConsumers");
		consumer = new KafkaConsumer<>(props);
		List<PartitionInfo> partitionInfos = consumer.partitionsFor("0.mqtt.sparkcluster");
		final List<TopicPartition> partitions = new ArrayList<>();
		if (partitionInfos != null) {
			for (final PartitionInfo partition : partitionInfos) {
				partitions.add(new TopicPartition(partition.topic(), partition.partition()));
			}
		}
		partitionInfos = consumer.partitionsFor("1.mqtt.sparkcluster");
		if (partitionInfos != null) {
			for (final PartitionInfo partition : partitionInfos) {
				partitions.add(new TopicPartition(partition.topic(), partition.partition()));
			}
		}
		LOG.info("assigning to partitions: {}", partitions);
		consumer.assign(partitions);
	}

	private void doConsume() {
		try {
			consumer.seekToEnd(new HashSet<TopicPartition>());
			while (!stopped) {
				final ConsumerRecords<String, byte[]> records = consumer.poll(10);
				for (final ConsumerRecord<String, byte[]> record : records) {
					recordSet.add(record);
					LOG.info("Reading record: topic = {}, partition = {}, offset = {}, key = {}, value = {}", record.topic(), record.partition(),
							record.offset(), record.key(), new String(record.value()));
				}
				consumer.commitSync();
				Thread.sleep(10);
			}
		} catch (final InterruptedException e) {
			LOG.error("interrupted", e);
		} finally {
			consumer.close();
			consumer.unsubscribe();
			consumer = null;
		}
	}

	public void start() {
		this.stopped = false;
		try {
			execService.submit(() -> doConsume());
		} catch (final Throwable t) {
			LOG.error("exception during execution", t);
		}
	}

	public void stop() {
		this.stopped = true;
		execService.shutdown();
	}

	public int getConsumeCounter() {
		return this.recordSet.size();
	}

}
