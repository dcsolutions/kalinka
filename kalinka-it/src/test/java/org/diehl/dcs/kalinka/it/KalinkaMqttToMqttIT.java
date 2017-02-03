package org.diehl.dcs.kalinka.it;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import org.I0Itec.zkclient.ZkClient;
import org.diehl.dcs.kalinka.it.model.KafkaConnector;
import org.diehl.dcs.kalinka.it.model.MqttConnector;
import org.junit.Before;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class KalinkaMqttToMqttIT {

	private static final Logger LOG = LoggerFactory.getLogger(KalinkaMqttToMqttIT.class);

	String zkConnection = "192.168.33.20:2181,192.168.33.21:2181,192.168.33.22:2181/activemq/connections";

	private ZkClient zkClient;

	@Before
	public void setUp() throws Exception {

		this.zkClient = new ZkClient(zkConnection);
		// this must be done after Zookeeper install in "real-world-setup"
		//this.zkClient.createPersistent("/apachemq/connections", true);
	}

	@Test
	public void mqtt2MqttBasicSubscribingAndPublishingSingleNode() throws Exception {
		final long intervalInMillis = 5000;
		final List<String> clients = new ArrayList<>();
		clients.add("beast");
		clients.add("pyro");
		clients.add("wolverine");
		final List<MqttConnector> connectors = new ArrayList<>();
		connectors.add(new MqttConnector("tcp://192.168.33.20:1883", "beast", clients, intervalInMillis));
		connectors.add(new MqttConnector("tcp://192.168.33.20:1883", "pyro", clients, intervalInMillis));
		connectors.add(new MqttConnector("tcp://192.168.33.20:1883", "wolverine", clients, intervalInMillis));

		waitForZookeeper(clients);

		connectors.stream().forEach(con -> con.start());

		Thread.sleep(intervalInMillis + 1000);

		//		LOG.info("OUT:");
		//		connectors.get(0).getOut().stream().forEach(out -> LOG.info(out));
		//		LOG.info("IN:");
		//		connectors.get(0).getIn().stream().forEach(in -> LOG.info(in));
		assertThat(connectors.get(0).getOut().size(), is(4));
		assertThat(connectors.get(0).getIn().size(), is(4));
		assertThat(connectors.get(1).getOut().size(), is(4));
		assertThat(connectors.get(1).getIn().size(), is(4));
		assertThat(connectors.get(2).getOut().size(), is(4));
		assertThat(connectors.get(2).getIn().size(), is(4));

		connectors.stream().forEach(con -> con.stop());
	}

	@Test
	public void mqtt2MqttBasicSubscribingAndPublishingWithIntermediateDisconnect() throws Exception {
		final long intervalInMillis = 5000;
		final List<String> clients = new ArrayList<>();
		clients.add("beast");
		clients.add("pyro");
		clients.add("wolverine");
		final List<MqttConnector> connectors = new ArrayList<>();
		connectors.add(new MqttConnector("tcp://192.168.33.20:1883", "beast", clients, intervalInMillis));
		connectors.add(new MqttConnector("tcp://192.168.33.21:1883", "pyro", clients, intervalInMillis));
		connectors.add(new MqttConnector("tcp://192.168.33.22:1883", "wolverine", clients, intervalInMillis));

		waitForZookeeper(clients);

		connectors.stream().forEach(con -> con.start());

		Thread.sleep(intervalInMillis + 1000);

		connectors.get(0).stop();

		Thread.sleep(intervalInMillis + 1000);

		//		LOG.info("OUT:");
		//		connectors.get(0).getOut().stream().forEach(out -> LOG.info(out));
		LOG.info("IN:");
		connectors.get(1).getIn().stream().forEach(in -> LOG.info(in));
		assertThat(connectors.get(0).getOut().size(), is(4));
		assertThat(connectors.get(0).getIn().size(), is(4));
		assertThat(connectors.get(1).getOut().size(), is(6));
		assertThat(connectors.get(1).getIn().size(), is(5));
		assertThat(connectors.get(2).getOut().size(), is(6));
		assertThat(connectors.get(2).getIn().size(), is(5));

		connectors.get(1).stop();
		connectors.get(2).stop();
	}

	@Test
	public void mqtt2MqttBasicSubscribingAndPublishingWithIntermediateReconnect() throws Exception {
		final long intervalInMillis = 5000;
		final List<String> clients = new ArrayList<>();
		clients.add("beast");
		clients.add("pyro");
		clients.add("wolverine");
		final List<MqttConnector> connectors = new ArrayList<>();
		connectors.add(new MqttConnector("tcp://192.168.33.20:1883", "beast", clients, intervalInMillis));
		connectors.add(new MqttConnector("tcp://192.168.33.21:1883", "pyro", clients, intervalInMillis));
		connectors.add(new MqttConnector("tcp://192.168.33.22:1883", "wolverine", clients, intervalInMillis));

		waitForZookeeper(clients);

		connectors.stream().forEach(con -> con.start());

		Thread.sleep(intervalInMillis + 1000);

		connectors.get(0).stop();
		connectors.get(0).setUrl("tcp://192.168.33.22:1883");
		connectors.get(0).reconnect();
		waitForZookeeper(Arrays.asList("beast"));

		connectors.get(0).start();

		Thread.sleep(intervalInMillis + 1000);

		//		LOG.info("OUT:");
		//		connectors.get(0).getOut().stream().forEach(out -> LOG.info(out));
		LOG.info("IN:");
		connectors.get(1).getIn().stream().forEach(in -> LOG.info(in));
		assertThat(connectors.get(0).getOut().size(), is(8));
		assertThat(connectors.get(0).getIn().size(), is(6));
		assertThat(connectors.get(1).getOut().size(), is(6));
		assertThat(connectors.get(1).getIn().size(), is(7));
		assertThat(connectors.get(2).getOut().size(), is(6));
		assertThat(connectors.get(2).getIn().size(), is(7));

		connectors.get(0).stop();
		connectors.get(1).stop();
		connectors.get(2).stop();
	}

	@Test
	public void mqtt2MqttBasicSubscribingAndPublishingMultipleNodes() throws Exception {
		final long intervalInMillis = 5000;
		final List<String> clients = new ArrayList<>();
		clients.add("beast");
		clients.add("pyro");
		clients.add("wolverine");
		final List<MqttConnector> connectors = new ArrayList<>();
		connectors.add(new MqttConnector("tcp://192.168.33.20:1883", "beast", clients, intervalInMillis));
		connectors.add(new MqttConnector("tcp://192.168.33.21:1883", "pyro", clients, intervalInMillis));
		connectors.add(new MqttConnector("tcp://192.168.33.22:1883", "wolverine", clients, intervalInMillis));

		waitForZookeeper(clients);

		connectors.stream().forEach(con -> con.start());

		Thread.sleep(intervalInMillis + 1000);

		//		LOG.info("OUT:");
		//		connectors.get(0).getOut().stream().forEach(out -> LOG.info(out));
		//		LOG.info("IN:");
		//		connectors.get(0).getIn().stream().forEach(in -> LOG.info(in));
		assertThat(connectors.get(0).getOut().size(), is(4));
		assertThat(connectors.get(0).getIn().size(), is(4));
		assertThat(connectors.get(1).getOut().size(), is(4));
		assertThat(connectors.get(1).getIn().size(), is(4));
		assertThat(connectors.get(2).getOut().size(), is(4));
		assertThat(connectors.get(2).getIn().size(), is(4));

		connectors.stream().forEach(con -> con.stop());
	}

	@Test
	public void kafka2MqttMultipleNodes() throws Exception {
		final long intervalInMillis = 3000;
		final List<String> clients = new ArrayList<>();
		clients.add("beast");
		clients.add("pyro");
		clients.add("wolverine");
		final List<MqttConnector> connectors = new ArrayList<>();
		connectors.add(new MqttConnector("tcp://192.168.33.20:1883", "beast", clients));
		connectors.add(new MqttConnector("tcp://192.168.33.21:1883", "pyro", clients));
		connectors.add(new MqttConnector("tcp://192.168.33.22:1883", "wolverine", clients));

		waitForZookeeper(clients);

		connectors.stream().forEach(con -> con.start());

		final KafkaConnector kafkaConnector = new KafkaConnector(clients, intervalInMillis);
		kafkaConnector.start();

		Thread.sleep(intervalInMillis + 1000);

		//		LOG.info("OUT:");
		//		connectors.get(0).getOut().stream().forEach(out -> LOG.info(out));
		//		LOG.info("IN:");
		//		connectors.get(0).getIn().stream().forEach(in -> LOG.info(in));
		assertThat(connectors.get(0).getOut().size(), is(4));
		assertThat(connectors.get(0).getIn().size(), is(4));
		assertThat(connectors.get(1).getOut().size(), is(4));
		assertThat(connectors.get(1).getIn().size(), is(4));
		assertThat(connectors.get(2).getOut().size(), is(4));
		assertThat(connectors.get(2).getIn().size(), is(4));

		connectors.stream().forEach(con -> con.stop());
	}

	private void waitForZookeeper(final List<String> clients) throws InterruptedException {
		final CountDownLatch cdl = new CountDownLatch(1);

		new Thread() {
			@Override
			public void run() {
				boolean tryAgain = true;
				while (tryAgain) {
					try {
						LOG.info("Trying to read data from zookeeper");
						clients.stream().forEach(client -> zkClient.readData("/" + client));
						cdl.countDown();
						tryAgain = false;

					} catch (final Exception e) {
						LOG.error("exception thrown while checking zk data: ", e);
					}
					try {
						Thread.sleep(100L);
					} catch (final InterruptedException e) {
						//
					}
				}
			};
		}.run();

		cdl.await(10, TimeUnit.SECONDS);
	}

}
