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

package com.github.dcsolutions.kalinka.sub.publisher;

import java.util.LinkedHashMap;
import java.util.Map.Entry;
import java.util.Optional;
import java.util.regex.Pattern;

import com.google.common.base.Preconditions;


/**
 * @author michas <michas@jarmoni.org>
 *
 */
public class MessagePublisherProvider<T, K, V> {

	private final LinkedHashMap<Pattern, IMessagePublisher<T, K, V>> publishers;

	public MessagePublisherProvider(final LinkedHashMap<Pattern, IMessagePublisher<T, K, V>> publishers) {

		this.publishers = Preconditions.checkNotNull(publishers);
	}

	public IMessagePublisher<T, K, V> getPublisher(final String sourceTopicName) {

		final Optional<Entry<Pattern, IMessagePublisher<T, K, V>>> opt =
				this.publishers.entrySet().stream().filter(p -> p.getKey().matcher(sourceTopicName).matches()).findFirst();
		return opt.isPresent() ? opt.get().getValue() : null;
	}
}
