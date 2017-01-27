package org.diehl.dcs.kalinka.mapper;

import java.util.List;

import org.diehl.dcs.kalinka.mapper.model.KafkaMessage;
import org.diehl.dcs.kalinka.mapper.model.MqttMessage;

public interface IMapper {

	List<MqttMessage> fromKafkaMessage(KafkaMessage kafkaMessage);

	List<KafkaMessage> toKafkaMessage(MqttMessage mqttMessage);

}
