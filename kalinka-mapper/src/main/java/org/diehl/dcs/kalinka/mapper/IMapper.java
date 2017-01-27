package org.diehl.dcs.kalinka.mapper;

import java.util.List;

import org.diehl.dcs.kalinka.mapper.kafka.KafkaMessage;
import org.diehl.dcs.kalinka.mapper.mqtt.MqttMessage;

public interface IMapper {

	List<MqttMessage> fromKafkaMessage(KafkaMessage kafkaMessage);

	List<KafkaMessage> toKafkaMessage(MqttMessage mqttMessage);

}
