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
package com.github.dcsolutions.kalinka.it.model;

import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.stream.Collectors;

import org.eclipse.paho.client.mqttv3.IMqttDeliveryToken;
import org.eclipse.paho.client.mqttv3.MqttAsyncClient;
import org.eclipse.paho.client.mqttv3.MqttCallback;
import org.eclipse.paho.client.mqttv3.MqttConnectOptions;
import org.eclipse.paho.client.mqttv3.MqttException;
import org.eclipse.paho.client.mqttv3.MqttPersistenceException;
import org.eclipse.paho.client.mqttv3.persist.MqttDefaultFilePersistence;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author Merkaban <ridethesnake7@yahoo.de>
 *
 */
public class MqttClient implements MqttCallback {
	@Override
	public void connectionLost(final Throwable throwable) {
		try {
			LOG.info("{} > {} > connection lost ({}).", url, clientId, throwable.getMessage());

		} catch (final Throwable t) {
			LOG.error(t.getMessage(), t);
		}
	}

	@Override
	public void messageArrived(final String topic, final org.eclipse.paho.client.mqttv3.MqttMessage mqttMessage) throws Exception {
		LOG.info("{} > receive: {} - {}", url, topic, new String(mqttMessage.getPayload(), StandardCharsets.UTF_8));
		in.add(LocalDateTime.now() + " " + topic);
	}

	@Override
	public void deliveryComplete(final IMqttDeliveryToken token) {
		LOG.info("message delivered to {} on {} by {}", token.getTopics(), token.getClient().getServerURI(), token.getClient().getClientId());
		out.add(LocalDateTime.now() + " " + token.getTopics()[0]);
	}


	private static final Logger LOG = LoggerFactory.getLogger(MqttClient.class);

	private String url;
	private final String clientId;
	private final List<String> subTopics = new ArrayList<>();
	private final String pubTopic2Mqtt;
	private static final String PUBPOSTFIX = "/pub";
	private static final String SUBPOSTFIX = "/sub";
	private static final String KAFKABASETOPIC = "mqtt/{p}/sparkcluster/pub";
	private List<String> otherClients = new ArrayList<>();
	private final String message;
	private final long intervalInMillis;
	private MqttAsyncClient mqttAsyncClient;
	private final List<String> in = new ArrayList<>();
	private final List<String> out = new ArrayList<>();
	private boolean publish = true;
	private volatile boolean stopped = false;
	private ExecutorService execService = Executors.newSingleThreadExecutor();
	private Future<?> future;

	public MqttClient(final String url, final String clientId, final List<String> clients, final long intervalInMillis) {
		this.url = url;
		this.clientId = clientId;
		this.pubTopic2Mqtt = "mqtt/" + clientId + "/mqtt/";
		this.subTopics.add("mqtt/+/mqtt/" + clientId + SUBPOSTFIX);
		this.subTopics.add("sparkcluster/mqtt/" + clientId + SUBPOSTFIX);
		this.otherClients = clients.stream().filter(client -> !client.equals(clientId)).collect(Collectors.toList());
		this.message = "Regards from " + clientId;
		this.intervalInMillis = intervalInMillis;
		LOG.info("constructed MqttConnector for {}", clientId);

		if (!doConnectBroker()) {
			LOG.error("no broker connection possible for {} and {}.", url, clientId);
		}
	}

	public MqttClient(final String url, final String clientId, final List<String> clients) {
		this.url = url;
		this.clientId = clientId;
		this.pubTopic2Mqtt = "mqtt/" + clientId + "/mqtt/";
		this.subTopics.add("mqtt/+/mqtt/" + clientId + SUBPOSTFIX);
		this.subTopics.add("sparkcluster/mqtt/" + clientId + SUBPOSTFIX);
		this.otherClients = clients.stream().filter(client -> !client.equals(clientId)).collect(Collectors.toList());
		this.message = "Regards from " + clientId;
		this.intervalInMillis = 1000L;
		this.publish = false;
		LOG.info("constructed MqttConnector for {}", clientId);

		if (!doConnectBroker()) {
			LOG.error("no broker connection possible for {} and {}.", url, clientId);
		}
	}

	private boolean doConnectBroker() {
		try {
			LOG.debug("{} > connect..", url);

			final MqttConnectOptions mqttConnectOptions = new MqttConnectOptions();
			mqttConnectOptions.setUserName("admin");
			mqttConnectOptions.setPassword("admin".toCharArray());
			mqttConnectOptions.setCleanSession(true);
			mqttConnectOptions.setWill(pubTopic2Mqtt, "Bye, bye Baby!".getBytes(), 0, false);

			// client
			final String tmpDir = System.getProperty("java.io.tmpdir");
			final MqttDefaultFilePersistence dataStore = new MqttDefaultFilePersistence(tmpDir);
			LOG.info("creating MqttAsyncClient for {} and {}", url, clientId);
			mqttAsyncClient = new MqttAsyncClient(url, clientId, dataStore);

			// callback
			mqttAsyncClient.setCallback(this);

			// connect
			mqttAsyncClient.connect(mqttConnectOptions).waitForCompletion();

			// subscriptions
			for (final String subTopic : subTopics) {
				LOG.info("client {} subscribing to {}", clientId, subTopic);
				mqttAsyncClient.subscribe(subTopic, 0);
			}

			LOG.info("{} > mqtt connection established for {}.", url, clientId);
			return true;
		} catch (final Throwable throwable) {
			LOG.error("{} > connection failed. ({})", url, throwable.getMessage());
			close();
			return false;
		}
	}

	private void close() {
		if (mqttAsyncClient != null) {
			try {
				mqttAsyncClient.disconnect();
				mqttAsyncClient.close();
				mqttAsyncClient = null;
			} catch (final Throwable throwable) {
				LOG.error("{} > close: close failed. ({})", url, throwable.getMessage());
			}
		}
	}

	private void doPublish() {
		while (!stopped) {
			try {
				if (publish) {
					publish();
				}
				Thread.sleep(intervalInMillis);
			} catch (final Throwable t) {
				LOG.error("exception while publishing", t);
			}
		}
	}

	private void doPublishWithFixExecutions(final int numberOfExecutions) {
		for (int i = 0; i < numberOfExecutions; i++) {
			try {
				if (publish) {
					publish();
				}
				Thread.sleep(intervalInMillis);
			} catch (final Throwable t) {
				LOG.error("exception while publishing", t);
			}
		}
	}

	private void publish() throws MqttException, MqttPersistenceException {
		for (final String otherClient : otherClients) {
			final String topic2Mqtt = pubTopic2Mqtt + otherClient + PUBPOSTFIX;
			LOG.info("{} publishing \"{}\" to topic {}", clientId, message, topic2Mqtt);
			mqttAsyncClient.publish(topic2Mqtt, (LocalDateTime.now() + message).getBytes(), 1, false);
		}
		final String topic2Kafka = KAFKABASETOPIC.replace("{p}", clientId);
		LOG.info("{} publishing \"{}\" to topic {}", clientId, message, topic2Kafka);
		mqttAsyncClient.publish(topic2Kafka, (LocalDateTime.now() + message).getBytes(), 1, false);
	}

	public void start() {
		stopped = false;
		future = execService.submit(() -> doPublish());
	}

	public void startWithFixExecutions(final int numberOfExecutions) {
		stopped = false;
		future = execService.submit(() -> doPublishWithFixExecutions(numberOfExecutions));
	}


	public void stop() {
		execService.shutdown();
		future = null;
		stopped = true;
		close();
	}

	public void restartWithFixExecutions(final int numberOfExecutions) {
		stopped = false;
		execService = Executors.newSingleThreadExecutor();
	}

	public void reconnect() {
		doConnectBroker();
	}

	public List<String> getOut() {
		return out;
	}

	public List<String> getIn() {
		return in;
	}

	public void setUrl(final String url) {
		this.url = url;
	}

	public boolean isDone() {
		return future == null ? true : this.future.isDone();
	}
}
