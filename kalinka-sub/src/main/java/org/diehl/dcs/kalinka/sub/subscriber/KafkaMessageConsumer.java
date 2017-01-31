package org.diehl.dcs.kalinka.sub.subscriber;

import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;

import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.common.PartitionInfo;
import org.apache.kafka.common.TopicPartition;
import org.apache.kafka.common.errors.WakeupException;
import org.diehl.dcs.kalinka.sub.publisher.IJmsMessageFromKafkaPublisher;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.jms.core.JmsTemplate;

public class KafkaMessageConsumer<K, V> implements Runnable {


	private static final Logger LOG = LoggerFactory.getLogger(KafkaMessageConsumer.class);

	private final KafkaConsumer<K, V> consumer;

	private final long pollTimeout;

	private JmsTemplate jmsTemplate;

	private IJmsMessageFromKafkaPublisher<K, V> jmsPublisher;

	public KafkaMessageConsumer(final KafkaConsumer<K, V> consumer, final List<Integer> assignedPartitions, final long pollTimeout, final String topic) {

		this.consumer = consumer;
		this.pollTimeout = pollTimeout;

		final List<PartitionInfo> partitionInfos = consumer.partitionsFor(topic);

		final Collection<TopicPartition> partitions = partitionInfos.stream().filter(p -> assignedPartitions.contains(Integer.valueOf(p.partition())))
				.map(p -> new TopicPartition(p.topic(), p.partition())).collect(Collectors.toList());
		LOG.info("Assigning to topic={}, partitions={}", topic, partitions);
		this.consumer.assign(partitions);
	}

	@Override
	public void run() {
		this.consume();
	}

	public void stop() {
		this.consumer.wakeup();
	}

	private void consume() {
		try {
			while (true) {
				final ConsumerRecords<K, V> records = this.consumer.poll(this.pollTimeout);
				LOG.trace("Polling..... Assignments={}", this.consumer.assignment());

				records.forEach(record -> {
					LOG.info("Received record={}", record);
					try {
						final String hostIdentifier = this.jmsPublisher.getHostIdentifier(record);
						if (hostIdentifier != null) {
							this.jmsPublisher.publish(record, this.jmsTemplate);
						}
						consumer.commitAsync((offsets, exception) -> {
							if (exception != null) {
								LOG.error("Commit failed for offsets=", exception);
							}

						});
					} catch (final WakeupException wex) {
						throw wex;
					} catch (final Throwable t) {
						LOG.error("Exception while sending to JMS", t);
					}

				});
			}
		} catch (final WakeupException wex) {
			LOG.error("Consumers have received wakeup-signal. Exit.", wex);
		} finally {
			try {
				this.consumer.commitSync();
			} finally {
				this.consumer.close();
			}
		}
	}

}
