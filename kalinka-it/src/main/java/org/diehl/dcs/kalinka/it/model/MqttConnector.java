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
import java.util.List;
import java.util.stream.Collectors;

import org.eclipse.paho.client.mqttv3.IMqttDeliveryToken;
import org.eclipse.paho.client.mqttv3.MqttAsyncClient;
import org.eclipse.paho.client.mqttv3.MqttCallback;
import org.eclipse.paho.client.mqttv3.MqttConnectOptions;
import org.eclipse.paho.client.mqttv3.MqttException;
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
				LOG.info("{} > connection lost ({}).", url, throwable.getMessage());

				t = null;

			} catch (final Throwable t) {
				LOG.error(t.getMessage(), t);
			}
		}

		@Override
		public void messageArrived(final String topic, final org.eclipse.paho.client.mqttv3.MqttMessage message) throws Exception {
			LOG.info("{} > receive: {} - {}", url, topic, new String(message.getPayload(), StandardCharsets.UTF_8));
		}

		@Override
		public void deliveryComplete(final IMqttDeliveryToken token) {
			try {
				LOG.info("{} > delivered: {} - {}", url, token.getTopics(), new String(token.getMessage().getPayload(), StandardCharsets.UTF_8));
			} catch (final MqttException e) {
				LOG.error("error while trying to log deliveryCompletion", e);
			}
		}
	}

	public class MqttConnectorRunnable implements Runnable {
		@Override
		public void run() {
			LOG.debug("{} > run..", url);

			if (!doConnectBroker()) {
				LOG.error("no broker connection possible.");
				//				delayRetry = Math.min(delayRetry * 2, CONNECTION_DELAY_RETRY_MAX);
				//				scheduleConnect(delayRetry);
			} else {
				doPublish();
			}
		}
	}


	volatile Thread t;
	private static final Logger LOG = LoggerFactory.getLogger(MqttConnector.class);

	//url = protocol + broker + ":" + port;
	String url;
	String clientId;
	int qos = 2;
	int port = 1883;
	List<String> subTopics;
	final String pubTopic2Mqtt;
	List<String> otherClients;
	String message;
	long intervalInMillis;
	MqttAsyncClient mqttAsyncClient;

	public MqttConnector(final String url, final String clientId, final List<String> clients, final long intervalInMillis) {
		this.url = url;
		this.clientId = clientId;
		this.pubTopic2Mqtt = "mqtt/" + clientId + "/mqtt/";
		this.subTopics.add("mqtt/+/mqtt/" + clientId);
		this.subTopics.add("spark_cluster/mqtt/" + clientId);
		this.otherClients = clients.stream().filter(client -> !client.equals(clientId)).collect(Collectors.toList());
		this.message = "Regards from " + clientId;
		this.intervalInMillis = intervalInMillis;


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
			mqttAsyncClient = new MqttAsyncClient(url, clientId, dataStore);

			// callback
			mqttAsyncClient.setCallback(new MqttConnectorCallback());

			// connect
			mqttAsyncClient.connect(mqttConnectOptions).waitForCompletion();

			// subscriptions
			for (final String subTopic : subTopics) {
				mqttAsyncClient.subscribe(subTopic, 0);
			}

			LOG.info("{} > mqtt connection established.", url);
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
				mqttAsyncClient.close();
			} catch (final Throwable throwable) {
				LOG.error("{} > close: close failed. ({})", url, throwable.getMessage());
			}
		}
	}

	private void doPublish() {
		final Thread thisThread = Thread.currentThread();
		while (t == thisThread) {
			try {
				for (final String otherClient : otherClients) {
					mqttAsyncClient.publish(pubTopic2Mqtt + otherClient, message.getBytes(), 1, false);
				}
				Thread.sleep(intervalInMillis);
			} catch (final Throwable t) {
				LOG.error("exception while publishing", t);
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
}