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

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

import javax.jms.MessageListener;

import org.apache.kafka.clients.producer.ProducerConfig;
import org.diehl.dcs.kalinka.pub.jms.JmsMessageListener;
import org.diehl.dcs.kalinka.pub.publisher.IMessagePublisher;
import org.diehl.dcs.kalinka.pub.publisher.MessagePublisherProvider;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.ImportResource;
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

	@SuppressWarnings({ "rawtypes", "unchecked" })
	@Bean
	public MessagePublisherProvider messagePublisherProvider() {

		final LinkedHashMap<Pattern, IMessagePublisher> publishers = new LinkedHashMap<>();
		this.messagePublishers.forEach(p -> publishers.put(p.getSourceTopicRegex(), p));
		return new MessagePublisherProvider(publishers);
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
