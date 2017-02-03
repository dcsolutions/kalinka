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
package org.diehl.dcs.kalinka.it.model;

import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import org.eclipse.paho.client.mqttv3.IMqttDeliveryToken;
import org.eclipse.paho.client.mqttv3.MqttAsyncClient;
import org.eclipse.paho.client.mqttv3.MqttCallback;
import org.eclipse.paho.client.mqttv3.MqttConnectOptions;
import org.eclipse.paho.client.mqttv3.persist.MqttDefaultFilePersistence;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author Merkaban <ridethesnake7@yahoo.de>
 *
 */
public class MqttConnector {

	public class MqttConnectorCallback implements MqttCallback {

		@Override
		public void connectionLost(final Throwable throwable) {
			try {
				LOG.info("{} > {} > connection lost ({}).", url, clientId, throwable.getMessage());

				t = null;

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
	}

	public class MqttConnectorRunnable implements Runnable {
		@Override
		public void run() {
			LOG.debug("{} > run..", url);

			doPublish();
		}
	}


	private volatile Thread t;
	private static final Logger LOG = LoggerFactory.getLogger(MqttConnector.class);

	//url = protocol + broker + ":" + port;
	private String url;
	private final String clientId;
	private final int qos = 2;
	private final int port = 1883;
	private final List<String> subTopics = new ArrayList<>();
	private final String pubTopic2Mqtt;
	private final String pubPostFix = "/pub";
	private final String subPostFix = "/sub";
	private List<String> otherClients = new ArrayList<>();
	private final String message;
	private final long intervalInMillis;
	private MqttAsyncClient mqttAsyncClient;
	private final List<String> in = new ArrayList<>();
	private final List<String> out = new ArrayList<>();
	private boolean publish = true;

	public MqttConnector(final String url, final String clientId, final List<String> clients, final long intervalInMillis) {
		this.url = url;
		this.clientId = clientId;
		this.pubTopic2Mqtt = "mqtt/" + clientId + "/mqtt/";
		this.subTopics.add("mqtt/+/mqtt/" + clientId + subPostFix);
		this.subTopics.add("spark_cluster/mqtt/" + clientId + subPostFix);
		this.otherClients = clients.stream().filter(client -> !client.equals(clientId)).collect(Collectors.toList());
		this.message = "Regards from " + clientId;
		this.intervalInMillis = intervalInMillis;
		LOG.info("constructed MqttConnector for {}", clientId);

		if (!doConnectBroker()) {
			LOG.error("no broker connection possible for {} and {}.", url, clientId);
		}
	}

	public MqttConnector(final String url, final String clientId, final List<String> clients) {
		this.url = url;
		this.clientId = clientId;
		this.pubTopic2Mqtt = "mqtt/" + clientId + "/mqtt/";
		this.subTopics.add("mqtt/+/mqtt/" + clientId + subPostFix);
		this.subTopics.add("spark_cluster/mqtt/" + clientId + subPostFix);
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

			// options
			final MqttConnectOptions mqttConnectOptions = new MqttConnectOptions();
			mqttConnectOptions.setCleanSession(true);

			// lastWill
			mqttConnectOptions.setWill(pubTopic2Mqtt, "Bye, bye Baby!".getBytes(), 0, false);

			// maximal queued messages
			//mqttConnectOptions.setMaxInflight(500);

			// client
			final String tmpDir = System.getProperty("java.io.tmpdir");
			final MqttDefaultFilePersistence dataStore = new MqttDefaultFilePersistence(tmpDir);
			LOG.info("creating MqttAsyncClient for {} and {}", url, clientId);
			mqttAsyncClient = new MqttAsyncClient(url, clientId, dataStore);

			// callback
			mqttAsyncClient.setCallback(new MqttConnectorCallback());

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
			} catch (final Throwable throwable) {
				LOG.error("{} > close: close failed. ({})", url, throwable.getMessage());
			}
		}
	}

	private void doPublish() {
		final Thread thisThread = Thread.currentThread();
		while (t == thisThread) {
			if (publish) {
				try {
					for (final String otherClient : otherClients) {
						final String topic = pubTopic2Mqtt + otherClient + pubPostFix;
						LOG.info("{} publishing \"{}\" to topic {}", clientId, message, topic);
						mqttAsyncClient.publish(topic, (LocalDateTime.now() + message).getBytes(), 1, false);
					}
					Thread.sleep(intervalInMillis);
				} catch (final Throwable t) {
					LOG.error("exception while publishing", t);
				}

			}
		}
	}

	public void start() {
		t = new Thread(new MqttConnectorRunnable(), clientId);
		t.start();
	}

	public void stop() {
		close();
		t = null;
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
}
