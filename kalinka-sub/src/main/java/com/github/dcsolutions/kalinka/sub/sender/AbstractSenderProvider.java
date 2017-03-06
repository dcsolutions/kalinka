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

import java.util.Optional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.github.dcsolutions.kalinka.cluster.IHostResolver;
import com.google.common.base.Preconditions;

/**
 * @author michas <michas@jarmoni.org>
 *
 */
public abstract class AbstractSenderProvider<T> implements ISenderProvider<T> {

	private static final Logger LOG = LoggerFactory.getLogger(AbstractSenderProvider.class);

	private final IHostResolver hostResolver;

	public AbstractSenderProvider(final IHostResolver hostResolver) {

		this.hostResolver = Preconditions.checkNotNull(hostResolver);
	}

	@Override
	public T getSender(final String hostIdentifier) {

		final Optional<String> hostOpt = this.hostResolver.getHost(hostIdentifier);
		if (!hostOpt.isPresent()) {
			LOG.warn("HostIdentifier={} is not registered in ZK. Cannot publish", hostIdentifier);
			return null;
		}
		return this.getSenderForHost(hostOpt.get());
	}

	public abstract T getSenderForHost(String host);

}
