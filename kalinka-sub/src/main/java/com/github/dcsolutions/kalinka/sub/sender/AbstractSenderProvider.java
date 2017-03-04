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

package com.github.dcsolutions.kalinka.sub.sender;

import com.github.dcsolutions.kalinka.sub.cache.IBrokerCache;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Preconditions;

/**
 * @author michas <michas@jarmoni.org>
 *
 */
public abstract class AbstractSenderProvider<T> implements ISenderProvider<T> {

	private static final Logger LOG = LoggerFactory.getLogger(AbstractSenderProvider.class);

	private final IBrokerCache brokerCache;

	public AbstractSenderProvider(final IBrokerCache brokerCache) {

		this.brokerCache = Preconditions.checkNotNull(brokerCache);
	}

	@Override
	public T getSender(final String hostIdentifier) {

		final String host = this.brokerCache.get(hostIdentifier);
		if (host == null) {
			LOG.warn("HostIdentifier={} is not registered in ZK. Cannot publish", hostIdentifier);
			return null;
		}
		return this.getSenderForHost(host);
	}

	public abstract T getSenderForHost(String host);

}
