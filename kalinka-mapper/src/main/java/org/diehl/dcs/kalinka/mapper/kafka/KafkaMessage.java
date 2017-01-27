package org.diehl.dcs.kalinka.mapper.kafka;

import java.util.Map;

import com.google.common.base.MoreObjects;

public class KafkaMessage {

	public static final String HEADER_SOURCE_ID = "sourceId";
	public static final String HEADER_TIMESTAMP = "timestamp";

	private final String topic;
	private final byte[] payload;
	private final String key;
	private final Map<String, String> headers;


	public KafkaMessage(final String topic, final byte[] payload, final String key,
			final Map<String, String> headers) {

		this.topic = topic;
		this.payload = payload;
		this.key = key;
		this.headers = headers;
	}


	public String getTopic() {
		return topic;
	}

	public byte[] getPayload() {
		return payload;
	}

	public String getKey() {
		return key;
	}

	public Map<String, String> getHeaders() {
		return headers;
	}

	@Override
	public String toString() {
		return MoreObjects.toStringHelper(this.getClass()).add("key", this.key)
				.add("topic", this.topic)
				.add("payload", payload == null ? null : new String(this.payload))
				.add("headers", this.headers).toString();
	}

}
