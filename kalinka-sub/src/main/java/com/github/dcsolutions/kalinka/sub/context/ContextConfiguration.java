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
package com.github.dcsolutions.kalinka.sub.context;

import static com.github.dcsolutions.kalinka.util.LangUtil.createObject;
import static com.github.dcsolutions.kalinka.util.LangUtil.splitCsStrings;

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import javax.jms.ConnectionFactory;

import org.apache.activemq.ActiveMQConnectionFactory;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.ImportResource;
import org.springframework.context.annotation.Scope;
import org.springframework.jms.connection.CachingConnectionFactory;

import com.github.dcsolutions.kalinka.cluster.IHostResolver;
import com.github.dcsolutions.kalinka.sub.publisher.IMessagePublisher;
import com.github.dcsolutions.kalinka.sub.publisher.MessagePublisherProvider;
import com.github.dcsolutions.kalinka.sub.sender.ISenderProvider;
import com.github.dcsolutions.kalinka.sub.sender.jms.JmsSenderProvider;
import com.github.dcsolutions.kalinka.sub.subscriber.KafkaMessageConsumer;
import com.google.common.base.Splitter;
import com.google.common.collect.Maps;


/**
 * @author michas <michas@jarmoni.org>
 *
 */
@Configuration
@ImportResource({ "${custom.config.file}", "${clustering.config.file}" })
public class ContextConfiguration {

	private static final Logger LOG = LoggerFactory.getLogger(ContextConfiguration.class);

	@SuppressWarnings("rawtypes")
	@Autowired
	private List<IMessagePublisher> messagePublishers;

	@Autowired
	private IHostResolver hostResolver;

	@Value("${kafka.group.id:kalinka}")
	private String kafkaGroupId;

	@Value("${kafka.hosts}")
	private String kafkaHosts;

	@Value("${kafka.poll.timeout:100}")
	private long kafkaPollTimeout;

	@Value("${kafka.auto.commit:false}")
	private boolean kafkaAutoCommit;

	@Value("${kafka.auto.commit.interval:#{null}}")
	private Long kafkaAutoCommitInterval;

	@Value("${kafka.session.timeout:15000}")
	private Integer kafkaSessionTimeout;

	@Value("${kafka.key.deserializer.class:org.apache.kafka.common.serialization.StringDeserializer}")
	private String kafkaKeyDeserializerClass;

	@Value("${kafka.value.deserializer.class:org.apache.kafka.common.serialization.ByteArrayDeserializer}")
	private String kafkaValueDeserializerClass;

	@Value("${jms.client.id.kalinka.sub:kalinka-sub-}")
	private String jmsClientIdKalinkaSub;

	@Value("${jms.user:#{null}}")
	private String jmsUser;

	@Value("${jms.passwd:#{null}}")
	private String jmsPasswd;

	private List<String> jmsHosts;

	private List<TopicInfo> kafkaSubscribedTopics;

	@Value("${jms.hosts}")
	public void setJmsHosts(final String rawJmsHosts) {

		this.jmsHosts = splitCsStrings(rawJmsHosts);
	}

	@Value("${kafka.subscribed.topics}")
	public void setKafkaSuscribedTopics(final String rawKafkaSubscribedTopics) {

		final List<String> topicNumThreadPairs = splitCsStrings(rawKafkaSubscribedTopics);
		this.kafkaSubscribedTopics = topicNumThreadPairs.stream().map(p -> {
			final List<String> topicNumThreadPair = Splitter.on(':').omitEmptyStrings().splitToList(p);
			return new TopicInfo(topicNumThreadPair.get(0), topicNumThreadPair.size() == 2 ? Integer.valueOf(topicNumThreadPair.get(1)) : 1);
		}).collect(Collectors.toList());
	}

	@SuppressWarnings({ "rawtypes", "unchecked" })
	@Bean
	public MessagePublisherProvider messagePublisherProvider() {

		final LinkedHashMap<Pattern, IMessagePublisher> publishers = new LinkedHashMap<>();
		this.messagePublishers.forEach(p -> publishers.put(p.getSourceTopicRegex(), p));
		return new MessagePublisherProvider(publishers);
	}

	@SuppressWarnings("rawtypes")
	@Scope(BeanDefinition.SCOPE_PROTOTYPE)
	@Bean
	public IMessagePublisher messagePublisher(final String className) {

		return createObject(className, IMessagePublisher.class);
	}

	@Bean
	public Map<String, Object> kafkaConsumerConfig() {
		final Map<String, Object> props = new HashMap<>();
		props.put(ConsumerConfig.GROUP_ID_CONFIG, this.kafkaGroupId);
		props.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, this.kafkaHosts);
		props.put(ConsumerConfig.ENABLE_AUTO_COMMIT_CONFIG, this.kafkaAutoCommit);
		if (this.kafkaAutoCommit) {
			props.put(ConsumerConfig.AUTO_COMMIT_INTERVAL_MS_CONFIG, this.kafkaAutoCommitInterval);
		}
		props.put(ConsumerConfig.SESSION_TIMEOUT_MS_CONFIG, this.kafkaSessionTimeout);
		props.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, this.kafkaKeyDeserializerClass);
		props.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, this.kafkaValueDeserializerClass);
		props.put(Constants.KAFKA_POLL_TIMEOUT, this.kafkaPollTimeout);
		props.put(Constants.KAFKA_SUBSCRIBED_TOPICS, this.kafkaSubscribedTopics);
		return props;
	}

	@SuppressWarnings("rawtypes")
	@Bean
	Map<String, KafkaMessageConsumer> KafkaMessageConsumers() {

		final Map<String, KafkaMessageConsumer> consumers = Maps.newHashMap();
		this.kafkaSubscribedTopics.forEach(t -> {
			for (int i = 0; i < t.numThreads; i++) {
				final String threadName = t.topic + "-" + i;
				final KafkaMessageConsumer consumer = this.kafkaMessageConsumer(t.topic);
				new Thread(consumer, threadName).start();
				consumers.put(t.topic + "-" + i, consumer);
			}
		});
		return consumers;
	}


	@SuppressWarnings({ "unchecked", "rawtypes" })
	@Bean(destroyMethod = "stop")
	@Scope(BeanDefinition.SCOPE_PROTOTYPE)
	public KafkaMessageConsumer kafkaMessageConsumer(final String topic) {

		return new KafkaMessageConsumer<>(this.kafkaConsumerConfig(), topic, this.senderProvider(), this.messagePublisherProvider());
	}

	@SuppressWarnings("rawtypes")
	@Bean
	public ISenderProvider senderProvider() {

		final Map<String, ConnectionFactory> connectionFactories = Maps.newHashMap();
		this.jmsHosts.forEach(h -> {
			connectionFactories.put(h.split(":")[0], this.connectionFactory(h));
		});
		return new JmsSenderProvider(connectionFactories, this.hostResolver);
	}

	@Bean
	@Scope(BeanDefinition.SCOPE_PROTOTYPE)
	public ConnectionFactory connectionFactory(final String host) {

		CachingConnectionFactory connectionFactory = null;
		LOG.info("Connecting with user={}, passwd={}", this.jmsUser, this.jmsPasswd);
		if (this.jmsUser != null && this.jmsPasswd != null) {
			connectionFactory = new CachingConnectionFactory(new ActiveMQConnectionFactory(this.jmsUser, this.jmsPasswd, host));
		} else {
			connectionFactory = new CachingConnectionFactory(new ActiveMQConnectionFactory(host));
		}
		connectionFactory.setCacheProducers(true);
		connectionFactory.setReconnectOnException(true);
		connectionFactory.setClientId(this.jmsClientIdKalinkaSub + host + "-" + UUID.randomUUID().toString());
		return connectionFactory;
	}

	private static final class TopicInfo {

		private final String topic;
		private final int numThreads;

		public TopicInfo(final String topic, final int numThreads) {

			this.topic = topic;
			this.numThreads = numThreads;
		}
	}
}
