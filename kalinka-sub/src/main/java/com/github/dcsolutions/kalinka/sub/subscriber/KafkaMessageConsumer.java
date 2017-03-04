package com.github.dcsolutions.kalinka.sub.subscriber;

import java.util.Map;

import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.common.errors.WakeupException;
import com.github.dcsolutions.kalinka.sub.context.Constants;
import com.github.dcsolutions.kalinka.sub.publisher.IMessagePublisher;
import com.github.dcsolutions.kalinka.sub.publisher.MessagePublisherProvider;
import com.github.dcsolutions.kalinka.sub.sender.ISenderProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.collect.Lists;

public class KafkaMessageConsumer<T, K, V> implements Runnable {


	private static final Logger LOG = LoggerFactory.getLogger(KafkaMessageConsumer.class);

	private final KafkaConsumer<K, V> consumer;

	private final long pollTimeout;

	private final ISenderProvider<T> senderProvider;

	private final MessagePublisherProvider<T, K, V> publisherProvider;

	public KafkaMessageConsumer(final Map<String, Object> consumerConfig, final String topic, final ISenderProvider<T> senderProvider,
			final MessagePublisherProvider<T, K, V> publisherProvider) {
		this.consumer = new KafkaConsumer<>(consumerConfig);
		this.pollTimeout = (Long) consumerConfig.get(Constants.KAFKA_POLL_TIMEOUT);
		this.senderProvider = senderProvider;
		this.publisherProvider = publisherProvider;

		LOG.info("Subscribing to topic={}", topic);
		this.consumer.subscribe(Lists.newArrayList(topic));
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
