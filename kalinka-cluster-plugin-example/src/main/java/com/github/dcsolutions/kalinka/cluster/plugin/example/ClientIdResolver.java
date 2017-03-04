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

package com.github.dcsolutions.kalinka.cluster.plugin.example;


import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import org.apache.activemq.broker.ConnectionContext;
import org.apache.activemq.command.ConnectionInfo;

import com.github.dcsolutions.kalinka.cluster.IIdResolver;

/**
 * @author michas <michas@jarmoni.org>
 *
 */
public class ClientIdResolver implements IIdResolver {

	static final String JMS_CLIENT_ID_KALINKA_PUB_REGEX = "kalinka-pub-.*, kalinka-sub-.*";

	private final List<Pattern> clientIdRegexPatternsToIgnore;

	public ClientIdResolver() {

		this(JMS_CLIENT_ID_KALINKA_PUB_REGEX);
	}

	public ClientIdResolver(final String clientIdRegexesToIgnore) {

		this.clientIdRegexPatternsToIgnore = Arrays.asList(clientIdRegexesToIgnore.split(",")).stream().filter(s -> !s.trim().isEmpty())
				.map(r -> Pattern.compile(r.trim())).collect(Collectors.toList());
	}

	@Override
	public Optional<String> resolveId(final ConnectionContext context, final ConnectionInfo info) {

		if (!isClientToIgnore(context.getClientId(), clientIdRegexPatternsToIgnore)) {
			return Optional.of(context.getClientId());
		}
		return Optional.empty();
	}

	static boolean isClientToIgnore(final String clientId, final List<Pattern> clientIdRegexPatternsToIgnore) {

		return clientIdRegexPatternsToIgnore.stream().filter(r -> {
			return r.matcher(clientId).matches();
		}).findFirst().isPresent();
	}
}
