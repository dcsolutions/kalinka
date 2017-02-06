package org.diehl.dcs.kalinka.sub.subscriber;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.common.PartitionInfo;
import org.apache.kafka.common.TopicPartition;
import org.apache.kafka.common.errors.WakeupException;
import org.diehl.dcs.kalinka.sub.context.ContextConfiguration;
import org.diehl.dcs.kalinka.sub.publisher.IMessagePublisher;
import org.diehl.dcs.kalinka.sub.publisher.MessagePublisherProvider;
import org.diehl.dcs.kalinka.sub.sender.ISenderProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class KafkaMessageConsumer2<T, K, V> implements Runnable {


	private static final Logger LOG = LoggerFactory.getLogger(KafkaMessageConsumer2.class);

	private final KafkaConsumer<K, V> consumer;

	private final long pollTimeout;

	private final ISenderProvider<T> senderProvider;

	private final MessagePublisherProvider<T, K, V> publisherProvider;

	@SuppressWarnings("unchecked")
	public KafkaMessageConsumer2(final Map<String, Object> consumerConfig, final String topic, final ISenderProvider<T> senderProvider,
			final MessagePublisherProvider<T, K, V> publisherProvider) {
		this.consumer = new KafkaConsumer<>(consumerConfig);
		this.pollTimeout = (Long) consumerConfig.get(ContextConfiguration.KAFKA_POLL_TIMEOUT);
		this.senderProvider = senderProvider;
		this.publisherProvider = publisherProvider;

		final List<PartitionInfo> partitionInfos = consumer.partitionsFor(topic);

		final List<String> subscribedPartitions = (List<String>) consumerConfig.get(ContextConfiguration.KAFKA_SUBSCRIBED_PARTITIONS);
		final Collection<TopicPartition> partitions = partitionInfos.stream().filter(p -> subscribedPartitions.contains(Integer.valueOf(p.partition())))
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
					LOG.debug("Received record={}", record);
					try {
						final IMessagePublisher<T, K, V> publisher = this.publisherProvider.getPublisher(record.topic());
						if (publisher == null) {
							LOG.warn("No publisher found for srcTopic={}", record.topic());
						} else {
							publisher.publish(record, this.senderProvider);
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
