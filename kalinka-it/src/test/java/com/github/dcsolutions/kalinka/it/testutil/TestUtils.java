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
package com.github.dcsolutions.kalinka.it.testutil;

import com.github.dcsolutions.kalinka.it.model.KafkaProducerClient;
import com.github.dcsolutions.kalinka.it.model.MqttClient;
import org.slf4j.Logger;

/**
 * @author Merkaban <ridethesnake7@yahoo.de>
 *
 */
public class TestUtils {
	public static void stopMqttClientWhenDone(final MqttClient con, final Logger LOG) {
		boolean finished = false;
		while (!finished) {
			if (con.isDone()) {
				con.stop();
				finished = true;
			} else {
				try {
					Thread.sleep(1000);
				} catch (final Exception e) {
					LOG.error("exception thrown", e);
				}
			}
		}
	}


	public static void stopKafkaProducerWhenDone(final KafkaProducerClient prod, final Logger LOG) {
		boolean finished = false;
		while (!finished) {
			if (prod.isDone()) {
				prod.stop();
				finished = true;
			} else {
				try {
					Thread.sleep(1000);
				} catch (final Exception e) {
					LOG.error("exception thrown", e);
				}
			}
		}
	}
}
