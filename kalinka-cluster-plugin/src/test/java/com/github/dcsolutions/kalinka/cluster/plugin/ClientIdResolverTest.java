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

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.util.Arrays;
import java.util.List;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import org.junit.Test;

import com.github.dcsolutions.kalinka.cluster.plugin.ClientIdResolver;


/**
 * @author michas <michas@jarmoni.org>
 *
 */
public class ClientIdResolverTest {

	@Test
	public void testIsClientToIgnore() throws Exception {

		final List<Pattern> patterns = Arrays.asList(ClientIdResolver.JMS_CLIENT_ID_KALINKA_PUB_REGEX.split(",")).stream().filter(s -> !s.trim().isEmpty())
				.map(r -> Pattern.compile(r.trim())).collect(Collectors.toList());

		assertTrue(ClientIdResolver.isClientToIgnore("kalinka-pub-123", patterns));
		assertTrue(ClientIdResolver.isClientToIgnore("kalinka-sub-456", patterns));
		assertFalse(ClientIdResolver.isClientToIgnore("kalinka-bla-789", patterns));
	}

}
