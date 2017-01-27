package org.diehl.dcs.kalinka.mapper.model;

import java.util.Map;

import com.google.common.base.MoreObjects;

public class MqttMessage {

	public static final String HEADER_TIMESTAMP = "timestamp";

	private final String clientId;

	private final String topic;

	private final byte[] payload;

	private final Map<String, String> headers;

	public MqttMessage(final String clientId, final String topic, final byte[] payload,
			final Map<String, String> headers) {

		this.clientId = clientId;
		this.topic = topic;
		this.payload = payload;
		this.headers = headers;
	}

	public String getClientId() {
		return clientId;
	}

	public String getTopic() {
		return topic;
	}

	public byte[] getPayload() {
		return payload;
	}

	public Map<String, String> getHeaders() {
		return headers;
	}

	@Override
	public String toString() {
		return MoreObjects.toStringHelper(this.getClass()).add("clientId", clientId)
				.add("topic", topic)
				.add("payload", payload == null ? null : new String(this.payload))
				.add("headers", this.headers).toString();
	}
}
