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
package com.github.dcsolutions.kalinka.it;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;

import java.util.ArrayList;
import java.util.List;

import org.I0Itec.zkclient.ZkClient;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.github.dcsolutions.kalinka.it.model.KafkaConsumerClient;
import com.github.dcsolutions.kalinka.it.model.MqttClient;
import com.github.dcsolutions.kalinka.it.testutil.TestUtils;
import com.github.dcsolutions.kalinka.it.testutil.ZookeeperCountdownLatch;

/**
 * @author Merkaban <ridethesnake7@yahoo.de>
 *
 */
public class KalinkaMqttToKafkaIT {
	private static final Logger LOG = LoggerFactory.getLogger(KalinkaMqttToKafkaIT.class);

	String zkConnection = "192.168.33.20:2181,192.168.33.21:2181,192.168.33.22:2181/activemq/connections";

	private ZkClient zkClient;
	private List<String> clients;
	private List<MqttClient> connectors;

	@Before
	public void setUp() throws Exception {

		this.zkClient = new ZkClient(zkConnection);
		clients = new ArrayList<>();
		clients.add("beast");
		clients.add("sabretooth");
		clients.add("wolverine");
		connectors = new ArrayList<>();
	}

	@After
	public void tearDown() throws Exception {
		this.zkClient.close();
		this.zkClient = null;
		clients.clear();
		clients = null;
		connectors.clear();
		connectors = null;
	}

	@Test
	public void multipleMqttClientsToKafka() throws InterruptedException {
		final long intervalInMillis = 1;

		connectors.add(new MqttClient("tcp://192.168.33.20:1883", "beast", clients, intervalInMillis));
		connectors.add(new MqttClient("tcp://192.168.33.20:1883", "sabretooth", clients, intervalInMillis));
		connectors.add(new MqttClient("tcp://192.168.33.20:1883", "wolverine", clients, intervalInMillis));

		ZookeeperCountdownLatch.waitForZookeeper(clients, zkClient);

		final KafkaConsumerClient consumer = new KafkaConsumerClient();
		consumer.start();

		Thread.sleep(1000);

		connectors.stream().forEach(con -> con.startWithFixExecutions(100));


		connectors.stream().forEach(con -> TestUtils.stopMqttClientWhenDone(con, LOG));
		consumer.stop();

		assertThat(connectors.get(0).getOut().size(), is(300));
		assertThat(connectors.get(1).getOut().size(), is(300));
		assertThat(connectors.get(2).getOut().size(), is(300));
		assertThat(consumer.getConsumeCounter(), is(300));
	}
}
