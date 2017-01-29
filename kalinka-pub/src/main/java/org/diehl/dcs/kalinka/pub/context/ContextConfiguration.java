package org.diehl.dcs.kalinka.pub.context;

import java.util.List;
import java.util.Map;

import javax.jms.ConnectionFactory;
import javax.jms.MessageListener;

import org.apache.activemq.ActiveMQConnectionFactory;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.common.serialization.Serializer;
import org.diehl.dcs.kalinka.pub.publisher.IJmsMessageToKafkaPublisher;
import org.diehl.dcs.kalinka.pub.publisher.impl.JmsMessageListener;
import org.diehl.dcs.kalinka.pub.publisher.impl.JmsMessageToKafkaPublisher;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jms.connection.CachingConnectionFactory;
import org.springframework.jms.listener.DefaultMessageListenerContainer;
import org.springframework.kafka.core.DefaultKafkaProducerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.core.ProducerFactory;

import com.google.common.base.Splitter;
import com.google.common.collect.Maps;

/**
 * @author michas <michas@jarmoni.org>
 *
 */
@Configuration
public class ContextConfiguration {

	private List<String> jmsHosts;

	@SuppressWarnings("rawtypes")
	private Class<? extends Serializer> kafkaKeySerializerClass;

	@SuppressWarnings("rawtypes")
	private Class<? extends Serializer> kafkaValueSerializerClass;

	@Value("${jms.hosts}")
	public void setJmsHosts(final String rawJmsHosts) {

		this.jmsHosts = Splitter.on(',').omitEmptyStrings().trimResults().splitToList(rawJmsHosts);
	}

	@Value("${kafka.key.serializer.class.name:org.apache.kafka.common.serialization.StringSerializer}")
	public void setKafkaKeySerializerClass(final String kafkaKeySerializerClassName) {

		this.kafkaKeySerializerClass = this.createSerializerClass(kafkaKeySerializerClassName);
	}

	@Value("${kafka.value.serializer.class.name:org.apache.kafka.common.serialization.ByteArraySerializer}")
	public void setKafkaValueSerializerClass(final String kafkaValueSerializerClassName) {

		this.kafkaValueSerializerClass = this.createSerializerClass(kafkaValueSerializerClassName);
	}

	@SuppressWarnings("rawtypes")
	@Bean
	public IJmsMessageToKafkaPublisher messageToKafkaPublisher() {

		return new JmsMessageToKafkaPublisher();
	}

	@Bean
	public Map<String, Object> kafkaProducerConfig() {

		final Map<String, Object> producerConfig = Maps.newHashMap();
		producerConfig.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, this.kafkaKeySerializerClass);
		producerConfig.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, this.kafkaValueSerializerClass);
		// fill config....
		return producerConfig;
	}

	@SuppressWarnings("rawtypes")
	@Bean
	public ProducerFactory kafkaProducerFactory() {

		return new DefaultKafkaProducerFactory<>(this.kafkaProducerConfig());
	}

	@SuppressWarnings({"rawtypes", "unchecked"})
	@Bean
	public KafkaTemplate kafkaTemplate() {

		return new KafkaTemplate<>(this.kafkaProducerFactory());
	}

	@SuppressWarnings({"unchecked", "rawtypes"})
	@Bean
	public MessageListener messageListener() {

		return new JmsMessageListener(this.messageToKafkaPublisher(), this.kafkaTemplate());
	}

	@Bean
	public Map<String, ConnectionFactory> connectionFactoryMap() {

		final Map<String, ConnectionFactory> connectionFactories = Maps.newHashMap();

		this.jmsHosts.forEach(host -> {
			final CachingConnectionFactory cachingConnectionFactory = new CachingConnectionFactory(new ActiveMQConnectionFactory(host));
			// fill properties....
			connectionFactories.put(host, cachingConnectionFactory);
		});
		return connectionFactories;
	}

	@Bean
	public Map<String, DefaultMessageListenerContainer> messageListenerContainers() {

		final Map<String, DefaultMessageListenerContainer> messageListenerContainers = Maps.newHashMap();

		this.connectionFactoryMap().forEach((host,factory) -> {
			final DefaultMessageListenerContainer messageListenerContainer = new DefaultMessageListenerContainer();
			messageListenerContainer.setMessageListener(this.messageListener());
			messageListenerContainer.setConnectionFactory(factory);
			// fill properties...
			messageListenerContainers.put(host, messageListenerContainer);
		});
		return messageListenerContainers;
	}

	@SuppressWarnings({"rawtypes"})
	private Class<? extends Serializer> createSerializerClass(final String className) {

		try {
			return Class.forName(className).asSubclass(Serializer.class);
		}
		catch (final ClassNotFoundException e) {
			throw new RuntimeException("Cannot instantiate=" + className, e);
		}
	}
}
