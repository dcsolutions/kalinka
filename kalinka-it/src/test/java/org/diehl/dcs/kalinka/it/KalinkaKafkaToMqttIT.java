package org.diehl.dcs.kalinka.it;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;

import java.util.ArrayList;
import java.util.List;

import org.I0Itec.zkclient.ZkClient;
import org.diehl.dcs.kalinka.it.model.KafkaProducerClient;
import org.diehl.dcs.kalinka.it.model.MqttClient;
import org.diehl.dcs.kalinka.it.testutil.ZookeeperCountdownLatch;
import org.junit.Before;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class KalinkaKafkaToMqttIT {

	private static final Logger LOG = LoggerFactory.getLogger(KalinkaKafkaToMqttIT.class);

	String zkConnection = "192.168.33.20:2181,192.168.33.21:2181,192.168.33.22:2181/activemq/connections";

	private ZkClient zkClient;

	@Before
	public void setUp() throws Exception {

		this.zkClient = new ZkClient(zkConnection);
	}

	@Test
	public void kafka2MqttMultipleNodes() throws Exception {
		final long intervalInMillis = 3000;
		final List<String> clients = new ArrayList<>();
		clients.add("beast");
		clients.add("pyro");
		clients.add("wolverine");
		final List<MqttClient> connectors = new ArrayList<>();
		connectors.add(new MqttClient("tcp://192.168.33.20:1883", "beast", clients));
		connectors.add(new MqttClient("tcp://192.168.33.21:1883", "pyro", clients));
		connectors.add(new MqttClient("tcp://192.168.33.22:1883", "wolverine", clients));

		ZookeeperCountdownLatch.waitForZookeeper(clients, zkClient);

		connectors.stream().forEach(con -> con.start());

		final KafkaProducerClient kafkaProducerClient = new KafkaProducerClient(clients, intervalInMillis);
		kafkaProducerClient.start();

		Thread.sleep(7000);

		//		LOG.info("OUT:");
		//		connectors.get(0).getOut().stream().forEach(out -> LOG.info(out));
		//		LOG.info("IN:");
		//		connectors.get(0).getIn().stream().forEach(in -> LOG.info(in));
		//assertThat(connectors.get(0).getOut().size(), is(4));
		//		assertThat(connectors.get(0).getIn().size(), is(4));
		//		assertThat(connectors.get(1).getOut().size(), is(4));
		//		assertThat(connectors.get(1).getIn().size(), is(4));
		//		assertThat(connectors.get(2).getOut().size(), is(4));
		//		assertThat(connectors.get(2).getIn().size(), is(4));

		kafkaProducerClient.stop();
		connectors.stream().forEach(con -> con.stop());
		assertThat(kafkaProducerClient.getPublishCounter(), is(9));

		assertTrue(true);
	}


}
