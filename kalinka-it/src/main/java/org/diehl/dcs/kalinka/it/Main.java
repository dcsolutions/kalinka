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

package org.diehl.dcs.kalinka.it;

import java.util.ArrayList;
import java.util.List;

import org.diehl.dcs.kalinka.it.model.MqttConnector;

/**
 * @author Merkaban <ridethesnake7@yahoo.de>
 *
 */

public class Main {

	public static void main(final String[] args) {
		final List<String> clients = new ArrayList<>();
		clients.add("beast");
		clients.add("pyro");
		clients.add("wolverine");
		final List<MqttConnector> connectors = new ArrayList<>();
		connectors.add(new MqttConnector("192.168.33.20", "beast", clients, 5000));
		connectors.add(new MqttConnector("192.168.33.21", "pyro", clients, 5000));
		connectors.add(new MqttConnector("192.168.33.22", "wolverine", clients, 5000));
		connectors.stream().forEach(con -> con.start());
	}
}
