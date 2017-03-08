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

package com.github.dcsolutions.kalinka.cluster.plugin;

import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;

import java.net.URI;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import org.apache.activemq.broker.BrokerPlugin;
import org.apache.activemq.broker.TransportConnector;
import org.apache.activemq.junit.EmbeddedActiveMQBroker;
import org.eclipse.paho.client.mqttv3.MqttClient;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.github.dcsolutions.kalinka.cluster.IConnectionStore;

/**
 * @author michas <michas@jarmoni.org>
 *
 */
public class KalinkaClusterPluginTest {

	private static final Logger LOG = LoggerFactory.getLogger(KalinkaClusterPluginTest.class);

	private KalinkaClusterPlugin plugin;

	@Mock
	private IConnectionStore connectionStore;

	private final ClientIdResolver clientIdResolver = new ClientIdResolver();

	private EmbeddedActiveMQBroker customizedBroker;

	@Before
	public void setUp() throws Exception {

		MockitoAnnotations.initMocks(this);

		this.plugin = new KalinkaClusterPlugin(this.connectionStore, this.clientIdResolver);

		this.customizedBroker = new EmbeddedActiveMQBroker() {
			@Override
			protected void configure() {
				try {
					final TransportConnector connector = new TransportConnector();
					connector.setUri(new URI("mqtt://localhost:1883"));
					connector.setName("mqtt");
					this.getBrokerService().addConnector(connector);
					this.getBrokerService().setPlugins(new BrokerPlugin[] { plugin });
				} catch (final Exception e) {
					e.printStackTrace();
				}
			}
		};
		this.customizedBroker.start();
	}

	@After
	public void tearDown() throws Exception {
		this.customizedBroker.stop();
	}

	@Test
	public void testWithClientIdResolver() throws Exception {

		final CountDownLatch cdl = new CountDownLatch(2);

		final MqttClient client = new MqttClient("tcp://localhost:1883", "pyro");

		Mockito.doAnswer(invocation -> {
			cdl.countDown();
			return null;
		}).when(this.connectionStore).upsertConnection("pyro");


		Mockito.doAnswer(invocation -> {
			cdl.countDown();
			return null;
		}).when(this.connectionStore).removeConnection("pyro");

		client.connect();
		client.disconnect();
		cdl.await(20L, TimeUnit.SECONDS);

		final InOrder inOrder = Mockito.inOrder(this.connectionStore);
		inOrder.verify(this.connectionStore).upsertConnection("pyro");
		inOrder.verify(this.connectionStore).removeConnection("pyro");
		inOrder.verifyNoMoreInteractions();
	}

	@Test
	public void testWithClientIdResolverIgnored() throws Exception {

		final MqttClient client = new MqttClient("tcp://localhost:1883", "kalinka-pub-xyz");

		client.connect();
		client.disconnect();

		Thread.sleep(100L);

		Mockito.verifyZeroInteractions(this.connectionStore);
	}

	@Test
	public void testWithClientIdResolverExThrownNotConnected() throws Exception {

		final MqttClient client = new MqttClient("tcp://localhost:1883", "pyro");

		Mockito.doThrow(RuntimeException.class).when(this.connectionStore).upsertConnection("pyro");

		try {
			client.connect();
		} catch (final Exception e) {
			LOG.error("Excpected exception", e);
		}

		Thread.sleep(100L);

		assertThat(client.isConnected(), is(false));

		Mockito.verify(this.connectionStore, Mockito.atLeastOnce()).upsertConnection("pyro");
	}
}
