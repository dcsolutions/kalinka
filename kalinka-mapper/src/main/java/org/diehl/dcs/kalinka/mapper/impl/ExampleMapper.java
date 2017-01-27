package org.diehl.dcs.kalinka.mapper.impl;

import java.util.List;
import java.util.Map;

import org.diehl.dcs.kalinka.mapper.IMapper;
import org.diehl.dcs.kalinka.mapper.model.KafkaMessage;
import org.diehl.dcs.kalinka.mapper.model.MqttMessage;

import com.google.common.base.Preconditions;
import com.google.common.base.Splitter;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;

public class ExampleMapper implements IMapper {

	@Override
	public List<MqttMessage> fromKafkaMessage(final KafkaMessage kafkaMessage) {

		Preconditions.checkNotNull(kafkaMessage);
		Preconditions.checkNotNull(kafkaMessage.getTopic());
		Preconditions.checkNotNull(kafkaMessage.getKey());
		Preconditions.checkNotNull(kafkaMessage.getPayload());
		Preconditions.checkNotNull(kafkaMessage.getHeaders());
		Preconditions.checkNotNull(kafkaMessage.getHeaders().get(KafkaMessage.HEADER_TIMESTAMP));

		final List<String> pathElems = Splitter.on('.').omitEmptyStrings().trimResults()
				.splitToList(kafkaMessage.getTopic());

		if(pathElems.size() != 2) {
			throw new IllegalStateException("Illegal kafka-topic=" + kafkaMessage.getTopic());
		}

		if(!pathElems.get(1).equals("mqtt")) {
			throw new IllegalStateException("Illegal kafka-topic=" + kafkaMessage.getTopic());
		}

		final Map<String, String> headers = Maps.newHashMap();
		headers.put(MqttMessage.HEADER_TIMESTAMP, kafkaMessage.getHeaders().get(KafkaMessage.HEADER_TIMESTAMP));

		return Lists.newArrayList(new MqttMessage(clientId, topic, payload, headers))
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public List<KafkaMessage> toKafkaMessage(final MqttMessage mqttMessage) {

		Preconditions.checkNotNull(mqttMessage);
		Preconditions.checkNotNull(mqttMessage.getClientId());
		Preconditions.checkNotNull(mqttMessage.getTopic());
		Preconditions.checkNotNull(mqttMessage.getHeaders());
		Preconditions.checkNotNull(mqttMessage.getHeaders().get(MqttMessage.HEADER_TIMESTAMP));

		final List<String> pathElems = Splitter.on('/').omitEmptyStrings().trimResults()
				.splitToList(mqttMessage.getTopic());

		if (pathElems.size() != 3 && pathElems.size() != 4) {
			throw new IllegalStateException("Invalid MQTT-topic=" + mqttMessage.getTopic());
		}

		final Map<String, String> headers = Maps.newHashMap();
		headers.put(KafkaMessage.HEADER_SOURCE_ID, mqttMessage.getClientId());
		headers.put(KafkaMessage.HEADER_TIMESTAMP,
				mqttMessage.getHeaders().get(MqttMessage.HEADER_TIMESTAMP));

		if (pathElems.size() == 4) {
			if (!pathElems.get(0).equals("mqtt") && !pathElems.get(2).equals("mqtt")) {
				throw new IllegalStateException("Invalid MQTT-topic=" + mqttMessage.getTopic());
			}
			if (!pathElems.get(2).equals(mqttMessage.getClientId())) {
				throw new IllegalStateException("Invalid MQTT-topic=" + mqttMessage.getTopic());
			}
			return Lists.newArrayList(new KafkaMessage("mqtt.mqtt", mqttMessage.getPayload(),
					pathElems.get(3), headers));
		}

		if (!pathElems.get(0).equals("mqtt")) {
			throw new IllegalStateException("Invalid MQTT-topic=" + mqttMessage.getTopic());
		}

		return Lists.newArrayList(new KafkaMessage("mqtt." + pathElems.get(2),
				mqttMessage.getPayload(), null, headers));
	}

}
