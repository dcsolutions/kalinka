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

import java.util.Optional;

import org.apache.activemq.broker.Broker;
import org.apache.activemq.broker.BrokerFilter;
import org.apache.activemq.broker.BrokerPlugin;
import org.apache.activemq.broker.ConnectionContext;
import org.apache.activemq.command.ConnectionInfo;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.github.dcsolutions.kalinka.cluster.IConnectionStore;
import com.github.dcsolutions.kalinka.cluster.IIdResolver;
import com.google.common.base.Preconditions;

/**
 * @author michas <michas@jarmoni.org>
 *
 */
public class KalinkaClusterPlugin implements BrokerPlugin {

	private static final Logger LOG = LoggerFactory.getLogger(KalinkaClusterPlugin.class);

	private final IConnectionStore connectionStore;
	private final IIdResolver idResolver;

	public KalinkaClusterPlugin(final IConnectionStore connectionStore, final IIdResolver idResolver) {

		this.connectionStore = Preconditions.checkNotNull(connectionStore);
		this.idResolver = Preconditions.checkNotNull(idResolver);
	}

	@Override
	public Broker installPlugin(final Broker next) throws Exception {

		LOG.info("Installing plugin...");

		return new BrokerFilter(next) {

			@Override
			public void addConnection(final ConnectionContext context, final ConnectionInfo info) throws Exception {

				final Optional<String> idOpt = idResolver.resolveId(context, info);
				if (idOpt.isPresent()) {
					connectionStore.upsertConnection(idOpt.get());
				}
				super.addConnection(context, info);
			}

			@Override
			public void removeConnection(final ConnectionContext context, final ConnectionInfo info, final Throwable error) throws Exception {

				final Optional<String> idOpt = idResolver.resolveId(context, info);
				if (idOpt.isPresent()) {
					connectionStore.removeConnection(idOpt.get());
				}
				super.removeConnection(context, info, error);
			}
		};
	}
}
