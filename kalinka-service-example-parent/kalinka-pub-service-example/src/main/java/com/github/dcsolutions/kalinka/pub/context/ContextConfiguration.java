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

package com.github.dcsolutions.kalinka.pub.context;

import static com.github.dcsolutions.kalinka.util.LangUtil.splitCsStrings;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

import javax.jms.ConnectionFactory;
import javax.jms.MessageListener;

import org.apache.activemq.ActiveMQConnectionFactory;
import org.apache.kafka.clients.producer.ProducerConfig;
import com.github.dcsolutions.kalinka.pub.jms.JmsMessageListener;
import com.github.dcsolutions.kalinka.pub.publisher.IMessagePublisher;
import com.github.dcsolutions.kalinka.pub.publisher.MessagePublisherProvider;
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
import org.springframework.jms.listener.DefaultMessageListenerContainer;
import org.springframework.kafka.core.DefaultKafkaProducerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.core.ProducerFactory;

import com.google.common.collect.Maps;

/**
 * @author michas <michas@jarmoni.org>
 *
 */
@Configuration
@ImportResource("${custom.config.file}")
public class ContextConfiguration {

	private static final Logger LOG = LoggerFactory.getLogger(ContextConfiguration.class);

	@SuppressWarnings("rawtypes")
	@Autowired
	private List<IMessagePublisher> messagePublishers;

	@Value("${kafka.hosts}")
	private String kafkaHosts;

	@Value("${kafka.retries:0}")
	private Integer kafkaRetries;

	@Value("${kafka.batch.size:16384}")
	private Integer kafkaBatchSize;

	@Value("${kafka.linger.ms:1}")
	private Integer kafkaLingerMs;

	@Value("${kafka.buffer.memory.config:33554432}")
	private Integer kafkaBufferMemory;

	@Value("${kafka.key.serializer.class:org.apache.kafka.common.serialization.StringSerializer}")
	private String kafkaKeySerializerClass;

	@Value("${kafka.value.serializer.class:org.apache.kafka.common.serialization.ByteArraySerializer}")
	private String kafkaValueSerializerClass;

	@Value("${jms.client.id.kalinka.pub:kalinka-pub-}")
	private String jmsClientIdKalinkaPub;

	@Value("${jms.user:#{null}}")
	private String jmsUser;

	@Value("${jms.passwd:#{null}}")
	private String jmsPasswd;

	private List<String> jmsHosts;

	private List<String> jmsDestinations;

	@Value("${jms.hosts}")
	public void setJmsHosts(final String rawJmsHosts) {

		this.jmsHosts = splitCsStrings(rawJmsHosts);
	}

	@Value("${jms.destinations}")
	public void setJmsDestinations(final String rawJmsDestinations) {

		this.jmsDestinations = splitCsStrings(rawJmsDestinations);
	}

	@SuppressWarnings({ "rawtypes", "unchecked" })
	@Bean
	public MessagePublisherProvider messagePublisherProvider() {

		return new MessagePublisherProvider(this.messagePublishers);
	}

	@Bean
	public Map<String, Object> kafkaProducerConfig() {

		final Map<String, Object> producerConfig = Maps.newHashMap();
		producerConfig.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, this.kafkaHosts);
		producerConfig.put(ProducerConfig.RETRIES_CONFIG, this.kafkaRetries);
		producerConfig.put(ProducerConfig.BATCH_SIZE_CONFIG, this.kafkaBatchSize);
		producerConfig.put(ProducerConfig.LINGER_MS_CONFIG, this.kafkaLingerMs);
		producerConfig.put(ProducerConfig.BUFFER_MEMORY_CONFIG, this.kafkaBufferMemory);
		producerConfig.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, this.kafkaKeySerializerClass);
		producerConfig.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, this.kafkaValueSerializerClass);
		return producerConfig;
	}

	@SuppressWarnings("rawtypes")
	@Bean
	public ProducerFactory kafkaProducerFactory() {

		return new DefaultKafkaProducerFactory<>(this.kafkaProducerConfig());
	}

	@SuppressWarnings({ "rawtypes", "unchecked" })
	@Bean
	public KafkaTemplate kafkaTemplate() {

		return new KafkaTemplate<>(this.kafkaProducerFactory());
	}

	@Bean
	public Map<ConnectionFactory, List<DefaultMessageListenerContainer>> connectionFactories() {

		final Map<ConnectionFactory, List<DefaultMessageListenerContainer>> connectionFactories = Maps.newHashMap();
		this.jmsHosts.forEach(h -> {
			final ConnectionFactory connectionFactory = this.connectionFactory(h);
			final List<DefaultMessageListenerContainer> messageListenerContainers =
					this.jmsDestinations.stream().map(d -> this.messageListenerContainer(connectionFactory, d)).collect(Collectors.toList());
			connectionFactories.put(connectionFactory, messageListenerContainers);
		});
		return connectionFactories;
	}

	@Bean
	@Scope(BeanDefinition.SCOPE_PROTOTYPE)
	public ConnectionFactory connectionFactory(final String host) {

		LOG.info("Connecting with user={}, passwd={}", this.jmsUser, this.jmsPasswd);
		CachingConnectionFactory connectionFactory = null;
		if (this.jmsUser != null && this.jmsPasswd != null) {
			connectionFactory = new CachingConnectionFactory(new ActiveMQConnectionFactory(this.jmsUser, this.jmsPasswd, host));
		} else {
			connectionFactory = new CachingConnectionFactory(new ActiveMQConnectionFactory(host));
		}
		connectionFactory.setCacheConsumers(true);
		connectionFactory.setReconnectOnException(true);
		connectionFactory.setClientId(this.jmsClientIdKalinkaPub + host + "-" + UUID.randomUUID().toString());
		try {
			connectionFactory.createConnection();
		} catch (final Throwable t) {
			LOG.warn("Could not create connection", t);
		}
		return connectionFactory;
	}

	@Bean
	@Scope(BeanDefinition.SCOPE_PROTOTYPE)
	public DefaultMessageListenerContainer messageListenerContainer(final ConnectionFactory connectionFactory, final String destination) {

		final DefaultMessageListenerContainer messageListenerContainer = new DefaultMessageListenerContainer();
		messageListenerContainer.setMessageListener(this.messageListener());
		messageListenerContainer.setConnectionFactory(connectionFactory);
		messageListenerContainer.setDestinationName(destination);
		messageListenerContainer.start();
		return messageListenerContainer;
	}

	@SuppressWarnings({ "unchecked", "rawtypes" })
	@Bean
	public MessageListener messageListener() {

		return new JmsMessageListener(this.messagePublisherProvider(), this.kafkaTemplate());
	}
}
