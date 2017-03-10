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

import com.github.dcsolutions.kalinka.it.model.KafkaProducerClient;
import com.github.dcsolutions.kalinka.it.model.MqttClient;
import com.github.dcsolutions.kalinka.it.testutil.TestUtils;
import com.github.dcsolutions.kalinka.it.testutil.ZookeeperCountdownLatch;

public class KalinkaKafkaToMqttIT {
	private static final Logger LOG = LoggerFactory.getLogger(KalinkaKafkaToMqttIT.class);

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
	public void kafka2MqttMultipleNodes() throws Exception {
		final long intervalInMillis = 500;

		connectors.add(new MqttClient("tcp://192.168.33.20:1883", "beast", clients));
		connectors.add(new MqttClient("tcp://192.168.33.21:1883", "sabretooth", clients));
		connectors.add(new MqttClient("tcp://192.168.33.22:1883", "wolverine", clients));

		ZookeeperCountdownLatch.waitForZookeeper(clients, zkClient);

		connectors.stream().forEach(con -> con.start());

		final KafkaProducerClient kafkaProducerClient = new KafkaProducerClient(clients, intervalInMillis);
		kafkaProducerClient.startWithFixExecutions(9);

		TestUtils.stopKafkaProducerWhenDone(kafkaProducerClient, LOG);

		connectors.stream().forEach(con -> con.stop());

		assertThat(kafkaProducerClient.getPublishCounter(), is(27));
		assertThat(connectors.get(0).getIn().size(), is(9));
		assertThat(connectors.get(1).getIn().size(), is(9));
		assertThat(connectors.get(2).getIn().size(), is(9));
	}

	@Test
	public void kafka2MqttMultipleNodesHighFrequency() throws Exception {
		final long intervalInMillis = 1;

		connectors.add(new MqttClient("tcp://192.168.33.20:1883", "beast", clients));
		connectors.add(new MqttClient("tcp://192.168.33.21:1883", "sabretooth", clients));
		connectors.add(new MqttClient("tcp://192.168.33.22:1883", "wolverine", clients));

		ZookeeperCountdownLatch.waitForZookeeper(clients, zkClient);

		connectors.stream().forEach(con -> con.start());

		final KafkaProducerClient kafkaProducerClient = new KafkaProducerClient(clients, intervalInMillis);
		kafkaProducerClient.startWithFixExecutions(1000);

		TestUtils.stopKafkaProducerWhenDone(kafkaProducerClient, LOG);

		//After KafkaProducer sent all messages, the MqttClients still need some time to be able to catch up
		Thread.sleep(10000);

		connectors.stream().forEach(con -> con.stop());

		assertThat(kafkaProducerClient.getPublishCounter(), is(3000));
		assertThat(connectors.get(0).getIn().size(), is(1000));
		assertThat(connectors.get(1).getIn().size(), is(1000));
		assertThat(connectors.get(2).getIn().size(), is(1000));
	}
}
