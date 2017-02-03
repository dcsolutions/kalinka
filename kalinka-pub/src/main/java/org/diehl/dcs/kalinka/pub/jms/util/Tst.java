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
package org.diehl.dcs.kalinka.pub.jms.util;

import java.util.Map;

import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.common.serialization.ByteArraySerializer;
import org.apache.kafka.common.serialization.StringSerializer;

import com.google.common.collect.Maps;

/**
 * @author michas <michas@jarmoni.org>
 *
 */
public class Tst {


	public static void main(final String[] args) {

		final Map<String, Object> producerConfig = Maps.newHashMap();
		producerConfig.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, "192.168.33.20:9092,192.168.33.21:9092,192.168.33.22:9092");
		producerConfig.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class);
		producerConfig.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, ByteArraySerializer.class);

		final KafkaProducer<String, byte[]> producer = new KafkaProducer<>(producerConfig);
		final ProducerRecord<String, byte[]> record = new ProducerRecord<>("sparkcluster.mqtt", "pyro", "bla".getBytes());
		producer.send(record);

	}

}
