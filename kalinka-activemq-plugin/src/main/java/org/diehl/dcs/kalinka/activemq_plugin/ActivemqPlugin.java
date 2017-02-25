/*
 * Copyright [2017] [DCS <Info-dcs@diehl.com>] Licensed under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance with the License. You may obtain a
 * copy of the License at http://www.apache.org/licenses/LICENSE-2.0 Unless required by applicable
 * law or agreed to in writing, software distributed under the License is distributed on an "AS IS"
 * BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License
 * for the specific language governing permissions and limitations under the License.
 */

package org.diehl.dcs.kalinka.activemq_plugin;


import java.util.Arrays;
import java.util.List;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import org.I0Itec.zkclient.ZkClient;
import org.I0Itec.zkclient.exception.ZkNodeExistsException;
import org.apache.activemq.broker.Broker;
import org.apache.activemq.broker.BrokerFilter;
import org.apache.activemq.broker.BrokerPlugin;
import org.apache.activemq.broker.ConnectionContext;
import org.apache.activemq.command.ConnectionInfo;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author michas <michas@jarmoni.org>
 *
 */
public class ActivemqPlugin implements BrokerPlugin {

	private static final Logger LOG = LoggerFactory.getLogger(ActivemqPlugin.class);

	static final String JMS_CLIENT_ID_KALINKA_PUB_REGEX = "kalinka-pub-.*, kalinka-sub-.*";

	private final ZkClient zkClient;
	private final String host;
	private final List<Pattern> clientIdRegexPatternsToIgnore;

	public ActivemqPlugin(final String zkServers, final String host) {

		this(zkServers, host, (JMS_CLIENT_ID_KALINKA_PUB_REGEX));
	}

	public ActivemqPlugin(final String zkServers, final String host, final String clientIdRegexesToIgnore) {

		LOG.debug("Trying to create ZkClient for zkServers={}, currentHost={}, clientIdRegexesToIgnore={}", zkServers, host, clientIdRegexesToIgnore);
		this.zkClient = new ZkClient(zkServers);
		this.host = host;
		this.clientIdRegexPatternsToIgnore = Arrays.asList(clientIdRegexesToIgnore.split(",")).stream().filter(s -> !s.trim().isEmpty())
				.map(r -> Pattern.compile(r.trim())).collect(Collectors.toList());
		LOG.info("Created ZkClient for zkServers={}, currentHost={}, clientIdRegexesToIgnore={}", zkServers, this.host, clientIdRegexesToIgnore);
	}


	@Override
	public Broker installPlugin(final Broker broker) throws Exception {

		return new BrokerFilter(broker) {

			@Override
			public void addConnection(final ConnectionContext context, final ConnectionInfo info) throws Exception {

				LOG.debug("Received connect from clientId={}", context.getClientId());

				super.addConnection(context, info);
				if (isClientToIgnore(context.getClientId(), clientIdRegexPatternsToIgnore)) {
					return;
				}
				upsertZkNode(context.getClientId());
				super.addConnection(context, info);
			}

			@Override
			public void removeConnection(final ConnectionContext context, final ConnectionInfo info, final Throwable error) throws Exception {

				LOG.debug("Received disconnect from clientId={}", context.getClientId());

				super.removeConnection(context, info, error);
				if (isClientToIgnore(context.getClientId(), clientIdRegexPatternsToIgnore)) {
					return;
				}
				deleteZkNode(context.getClientId());
				super.removeConnection(context, info, error);
			}
		};
	};

	static boolean isClientToIgnore(final String clientId, final List<Pattern> clientIdRegexPatternsToIgnore) {

		return clientIdRegexPatternsToIgnore.stream().filter(r -> {
			return r.matcher(clientId).matches();
		}).findFirst().isPresent();

	}

	void upsertZkNode(final String clientId) {

		final String node = "/" + clientId;
		LOG.debug("Trying to upsert node={}, to host={}", node, this.host);
		try {
			this.zkClient.createPersistent(node, this.host);
		} catch (final ZkNodeExistsException e) {
			this.zkClient.writeData(node, this.host);
		}
		LOG.info("Upserted node={}, to host={}", node, this.host);
	}

	void deleteZkNode(final String clientId) {

		final String node = "/" + clientId;
		LOG.debug("Trying to delete node={}", node);
		this.zkClient.delete(node);
		LOG.info("Deleted node={}", node);
	}
}
