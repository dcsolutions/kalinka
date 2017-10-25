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

import java.util.Set;

import com.github.dcsolutions.kalinka.cluster.IHostResolver;
import com.google.common.base.Preconditions;


/**
 * @author michas <michas@jarmoni.org>
 *
 */
public abstract class AbstractSenderProvider<T> implements ISenderProvider<T> {

	private final IHostResolver hostResolver;

	public AbstractSenderProvider(final IHostResolver hostResolver) {

		this.hostResolver = Preconditions.checkNotNull(hostResolver);
	}

	@Override
	public Set<T> getSenders(final String hostIdentifier) {

		final Set<String> hosts = this.hostResolver.getHosts(hostIdentifier);
		return this.getSendersForHosts(hosts);
	}

	public abstract Set<T> getSendersForHosts(Set<String> hosts);

}
