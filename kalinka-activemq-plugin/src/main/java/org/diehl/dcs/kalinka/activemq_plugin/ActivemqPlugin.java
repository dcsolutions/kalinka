package org.diehl.dcs.kalinka.activemq_plugin;

import org.I0Itec.zkclient.ZkClient;
import org.apache.activemq.broker.Broker;
import org.apache.activemq.broker.BrokerFilter;
import org.apache.activemq.broker.BrokerPlugin;
import org.apache.activemq.broker.ConnectionContext;
import org.apache.activemq.command.ConnectionInfo;

public class ActivemqPlugin implements BrokerPlugin {

	private ZkClient zkClient;

	@Override
	public Broker installPlugin(final Broker broker) throws Exception {

		return new BrokerFilter(broker) {

			@Override
			public void addConnection(final ConnectionContext context, final ConnectionInfo info)
					throws Exception {
				// TODO Auto-generated method stub
				super.addConnection(context, info);
			}

			@Override
			public void removeConnection(final ConnectionContext context, final ConnectionInfo info,
					final Throwable error) throws Exception {
				// TODO Auto-generated method stub
				super.removeConnection(context, info, error);
			}
		};
	};

}
