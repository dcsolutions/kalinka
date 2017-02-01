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
package org.diehl.dcs.kalinka.sub.context;

import static org.diehl.dcs.kalinka.util.LangUtil.createClass;
import static org.diehl.dcs.kalinka.util.LangUtil.createObject;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import javax.jms.ConnectionFactory;

import org.I0Itec.zkclient.ZkClient;
import org.apache.activemq.ActiveMQConnectionFactory;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.common.serialization.Deserializer;
import org.diehl.dcs.kalinka.sub.cache.BrokerCache;
import org.diehl.dcs.kalinka.sub.cache.IBrokerCache;
import org.diehl.dcs.kalinka.sub.publisher.IJmsMessageFromKafkaPublisher;
import org.diehl.dcs.kalinka.sub.publisher.IMessageFromKafkaPublisher;
import org.diehl.dcs.kalinka.sub.publisher.JmsTemplateProvider;
import org.diehl.dcs.kalinka.sub.util.KafkaPartitionResolver;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Scope;
import org.springframework.jms.connection.CachingConnectionFactory;

import com.google.common.base.Splitter;
import com.google.common.collect.Maps;


/**
 * @author michas <michas@jarmoni.org>
 *
 */
@Configuration
public class ContextConfiguration {

	public static final String KAFKA_POLL_TIMEOUT = "kafkaPollTimeout";
	public static final String KAFKA_SUBSCRIBED_PARTITIONS = "kafkaSubscribedPartitions";
	public static final String KAFKA_SUBSCRIBED_TOPICS = "kafkaSubscribedTopics";

	private static final Logger LOG = LoggerFactory.getLogger(ContextConfiguration.class);

	@SuppressWarnings("rawtypes")
	private Class<? extends Deserializer> kafkaKeyDeserializerClass;

	@SuppressWarnings("rawtypes")
	private Class<? extends Deserializer> kafkaValueDeserializerClass;

	private List<String> jmsHosts;

	private List<String> kafkaSubscribedTopics;

	private List<Integer> kafkaSubscribedPartitions;

	@Value("${kafka.key.serializer.class.name:org.apache.kafka.common.serialization.StringDeserializer}")
	public void setKafkaKeyDeserializerClass(final String kafkaKeyDeserializerClassName) {

		this.kafkaKeyDeserializerClass = createClass(kafkaKeyDeserializerClassName, Deserializer.class);
	}

	@Value("${kafka.value.serializer.class.name:org.apache.kafka.common.serialization.ByteArrayDeserializer}")
	public void setKafkaValueDeserializerClass(final String kafkaValueDeserializerClassName) {

		this.kafkaValueDeserializerClass = createClass(kafkaValueDeserializerClassName, Deserializer.class);
	}

	@Value("${jms.hosts}")
	public void setJmsHosts(final String rawJmsHosts) {

		this.jmsHosts = Splitter.on(',').omitEmptyStrings().trimResults().splitToList(rawJmsHosts);
	}

	@Value("${kafka.subscribed.topics}")
	public void setKafkaSuscribedTopics(final String rawKafkaSubscribedTopics) {

		this.kafkaSubscribedTopics = Splitter.on(',').omitEmptyStrings().trimResults().splitToList(rawKafkaSubscribedTopics);
	}

	@Value("${kafka.subscribed.partitions}")
	public void setKafkaSubscribedPartitions(final String rawKafkaSubscribedPartitions) {

		this.kafkaSubscribedPartitions = KafkaPartitionResolver.partitionsFromString(rawKafkaSubscribedPartitions);
	}

	@Value("${zk.hosts}")
	private String zkHosts;

	@Value("${kafka.hosts}")
	private String kafkaHosts;

	@Value("${kafka.replication.factor:1}")
	private int kafkaReplicationFactor;

	@Value("${kafka.poll.timeout:100}")
	private long kafkaPollTimeout;

	@Value("${kafka.auto.commit:false}")
	private boolean kafkaAutoCommit;

	@Value("${kafka.auto.commit.interval:#{null}}")
	private Long kafkaAutoCommitInterval;

	@Value("${kafka.session.timeout:15000}")
	private Integer kafkaSessionTimeout;

	@Value("${message.from.kafka.publisher.class.name}")
	private String messageFromKafkaPublisherClassName;

	@Value("${jms.client.id.kalinka.sub:kalinka-sub-}")
	private String jmsClientIdKalinkaSub;

	@Value("${cache.initial.size}")
	private int cacheInitialSize;

	@Value("${cache.max.size}")
	private long cacheMaxSize;

	@Value("${cache.eviction.hours}")
	private int cacheEvictionHours;

	@SuppressWarnings("rawtypes")
	@Bean
	public IMessageFromKafkaPublisher messageFromKafkaPublisher() {

		return createObject(this.messageFromKafkaPublisherClassName, IJmsMessageFromKafkaPublisher.class);
	}

	@Bean
	public ZkClient zkClient() {

		return new ZkClient(this.zkHosts);
	}

	@Bean
	public Map<String, Object> kafkaConsumerConfig() {
		final Map<String, Object> props = new HashMap<>();
		props.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, this.kafkaHosts);
		props.put(ConsumerConfig.ENABLE_AUTO_COMMIT_CONFIG, this.kafkaAutoCommit);
		if (this.kafkaAutoCommit) {
			props.put(ConsumerConfig.AUTO_COMMIT_INTERVAL_MS_CONFIG, this.kafkaAutoCommitInterval);
		}
		props.put(ConsumerConfig.SESSION_TIMEOUT_MS_CONFIG, this.kafkaSessionTimeout);
		props.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, this.kafkaKeyDeserializerClass);
		props.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, this.kafkaValueDeserializerClass);
		props.put(KAFKA_POLL_TIMEOUT, this.kafkaPollTimeout);
		props.put(KAFKA_SUBSCRIBED_PARTITIONS, this.kafkaSubscribedPartitions);
		props.put(KAFKA_SUBSCRIBED_TOPICS, this.kafkaSubscribedTopics);
		return props;
	}

	//	@Bean
	//	public KafkaMessageConsumer kafkaMessageConsumer() {
	//
	//		return new KafkaMessageConsumer<>(consumer, assignedPartitions, pollTimeout, topic, jmsTemplateProvider)
	//	}
	//	@Bean
	//	public IJmsTemplateFactory jmsTemplateFactory() {
	//
	//		return new JmsTemplateFactory(this.connectionFactoryFactory(), this.jmsHosts);
	//
	//	}


	//	@Bean
	//	public IConnectionFactoryFactory connectionFactoryFactory() {
	//
	//		return host -> connectionFactory(host);
	//	}

	@Bean
	public IBrokerCache brokerCache() {

		return new BrokerCache(this.zkClient(), this.cacheInitialSize, this.cacheMaxSize, this.cacheEvictionHours);
	}

	@Bean
	public JmsTemplateProvider jmsTemplateProvider() {

		final Map<String, ConnectionFactory> connectionFactories = Maps.newHashMap();
		this.jmsHosts.forEach(h -> connectionFactories.put(h, this.connectionFactory(h)));
		return new JmsTemplateProvider(connectionFactories, this.brokerCache());
	}

	@Bean
	@Scope(BeanDefinition.SCOPE_PROTOTYPE)
	public ConnectionFactory connectionFactory(final String host) {

		final CachingConnectionFactory connectionFactory = new CachingConnectionFactory(new ActiveMQConnectionFactory(host));
		connectionFactory.setCacheProducers(true);
		connectionFactory.setReconnectOnException(true);
		connectionFactory.setClientId(this.jmsClientIdKalinkaSub + host + "-" + UUID.randomUUID().toString());
		try {
			connectionFactory.createConnection();
		} catch (final Throwable t) {
			LOG.warn("Could not create connection", t);
		}
		return connectionFactory;
	}


}