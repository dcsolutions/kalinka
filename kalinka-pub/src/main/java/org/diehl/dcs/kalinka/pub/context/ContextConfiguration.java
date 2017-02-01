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

package org.diehl.dcs.kalinka.pub.context;

import static org.diehl.dcs.kalinka.util.LangUtil.createClass;
import static org.diehl.dcs.kalinka.util.LangUtil.createObject;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

import javax.jms.MessageListener;

import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.common.serialization.Serializer;
import org.diehl.dcs.kalinka.pub.publisher.IMessagePublisher;
import org.diehl.dcs.kalinka.pub.publisher.JmsMessageListener;
import org.diehl.dcs.kalinka.pub.publisher.MessagePublisherProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Scope;
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
	@SuppressWarnings("rawtypes")
	private Class<? extends Serializer> kafkaKeySerializerClass;

	@SuppressWarnings("rawtypes")
	private Class<? extends Serializer> kafkaValueSerializerClass;

	private List<String> kafkaMessagePublisherClassNames;

	@Value("${kafka.key.serializer.class.name:org.apache.kafka.common.serialization.StringSerializer}")
	public void setKafkaKeySerializerClass(final String kafkaKeySerializerClassName) {

		this.kafkaKeySerializerClass = createClass(kafkaKeySerializerClassName, Serializer.class);
	}

	@Value("${kafka.value.serializer.class.name:org.apache.kafka.common.serialization.ByteArraySerializer}")
	public void setKafkaValueSerializerClass(final String kafkaValueSerializerClassName) {

		this.kafkaValueSerializerClass = createClass(kafkaValueSerializerClassName, Serializer.class);
	}

	@Value("${kafka.message.publisher.class.names}")
	public void setKafkaMessagePublisherClassNames(final String rawKafkaMessagePublisherClassNames) {

		this.kafkaMessagePublisherClassNames = Splitter.on(',').omitEmptyStrings().trimResults().splitToList(rawKafkaMessagePublisherClassNames);
	}

	@SuppressWarnings({ "rawtypes", "unchecked" })
	@Bean
	public MessagePublisherProvider messagePublisherProvider() {

		final LinkedHashMap<Pattern, IMessagePublisher> publishers = new LinkedHashMap<>();
		this.kafkaMessagePublisherClassNames.forEach(className -> {
			final IMessagePublisher publisher = this.messageToKafkaPublisher(className);
			publishers.put(Pattern.compile(publisher.getSourceTopicRegex()), publisher);
		});
		return new MessagePublisherProvider(publishers);
	}


	@SuppressWarnings("rawtypes")
	@Bean
	@Scope(BeanDefinition.SCOPE_PROTOTYPE)
	public IMessagePublisher messageToKafkaPublisher(final String className) {

		return createObject(className, IMessagePublisher.class);
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

	@SuppressWarnings({ "unchecked", "rawtypes" })
	@Bean
	public MessageListener messageListener() {

		return new JmsMessageListener(this.messagePublisherProvider(), this.kafkaTemplate());
	}
}
