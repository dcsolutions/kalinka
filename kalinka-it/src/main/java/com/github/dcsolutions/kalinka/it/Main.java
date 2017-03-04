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

package com.github.dcsolutions.kalinka.it;

import java.util.ArrayList;
import java.util.List;

import com.github.dcsolutions.kalinka.it.model.MqttClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author Merkaban <ridethesnake7@yahoo.de>
 *
 */

public class Main {
	private static final Logger LOG = LoggerFactory.getLogger(Main.class);

	public static void main(final String[] args) {
		LOG.info("Starting application...");
		final List<String> clients = new ArrayList<>();
		clients.add("beast");
		clients.add("pyro");
		clients.add("wolverine");
		final List<MqttClient> connectors = new ArrayList<>();
		connectors.add(new MqttClient("tcp://192.168.33.22:1883", "beast", clients, 3000));
		connectors.add(new MqttClient("tcp://192.168.33.20:1883", "pyro", clients, 3000));
		connectors.add(new MqttClient("tcp://192.168.33.21:1883", "wolverine", clients, 3000));
		connectors.stream().forEach(con -> con.start());
	}
}
