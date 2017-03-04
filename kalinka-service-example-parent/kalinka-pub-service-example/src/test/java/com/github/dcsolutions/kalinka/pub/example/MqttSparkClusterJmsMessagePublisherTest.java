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
package com.github.dcsolutions.kalinka.pub.example;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;

import org.junit.Test;

/**
 * @author michas <michas@jarmoni.org>
 *
 */
public class MqttSparkClusterJmsMessagePublisherTest {

	private static final String SRC_TOPIC = "tcp://mqtt.src.sparkcluster.pub";

	private final MqttSparkClusterJmsMessagePublisher publisher = new MqttSparkClusterJmsMessagePublisher();

	@Test
	public void testGetSourceId() {

		assertThat(this.publisher.getSourceId(SRC_TOPIC), is("src"));
	}

	@Test
	public void testGetDestTopic() {

		assertThat(this.publisher.getDestTopic(SRC_TOPIC), is("1.mqtt.sparkcluster"));
	}

}
