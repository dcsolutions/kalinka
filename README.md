# What is it for?

There are several solutions to access a Kafka-cluster via MQTT, e.g. with [Kafka-Bridge](https://github.com/jacklund/mqttKafkaBridge).
In general these solutions work in a "one-way"-manner so that MQTT-clients may publish to Kafka but cannot consume from it.
*Kalinka* tries to solve this issue. Since the reference-implementation uses ActiveMQ as MQTT-broker there is no restriction to MQTT
so it should work with any supported protocol.

# Status Quo

The project is in prototype-state right now and acts as a proof of concept.

# Goals

* No cluster of MQTT-Brokers. The system should scale well and as we found out in our tests "normal" broker-clusters are limited in this respect. Instead we want to use 1-n "standalone" broker-instances. As we will see, this decision implies some issues we had to deal with.
* Kafka/Zookeeper is the only "real" clustered component.
* No single-point-of-failure (of course)

# Design

*Kalinka* consists of 2 components:

## kalinka-pub

Consumes from MQTT and publishes the messages to Kafka.

The way, *kalinka-pub* works, is quite straightforward: It subscribes several MQTT-topics (more precisely, JMS-queues because we use ActiveMQ as JMS-broker which uses JMS internally) and publishes all received messages to a kafka-cluster. Since JMS-queues and kafka-topics behave in a different way, some kind of mapping is required. See chapter *Topic-Mapping* for details on how to deal with that.
Each *kalinka-pub*-instance may consume from several brokers and each broker should be consumed by multiple *kalinka-pub*-instances (no s.p.o.f.)

## kalinka-sub

Subscribes to Kafka-topics and forwards messages to ActiveMQ-broker 

In contrast to *kalinka-pub* the situation is slightly different: As mentioned before the ActiveMQ-brokers do not act as one logical broker, they consist of several independent broker-instances. So it is crucial for the service to know, which of the available broker-instances the MQTT-client is connected to. This information is currently stored in Zookeeper. If it turns out that this does scale well, we'll have to find a better solution.

Another challenge is to limit the number of connections between hosts. If *n* instances of *kalinka-sub* had to send to *m* broker-instances this would result in n\*m relations between *kalinka-sub*- and broker-instances. We are trying to solve this issue by leveraging Kafka's partitioning-mechanism in combination with an additional grouping-strategy. More details in chapter *2nd-level-partitioning*.


# Topic-mapping

T.B.D.

# 2nd-level partitioning

T.B.D.

# Test-environment

## General

This setup allows to test the different components in combination.
**Note:** This is just a simple setup for testing and development. Security aspects are not considered so do not use this in a production-like environment without major changes!

The setup has been tested with Ubuntu 16.04 and Ubuntu 16.10 but it should work on any modern Linux-distribution. Feel free to test it on a Windows-OS but I cannot guarantee success.

The setup contains:

* Zookeeper
* Kafka
* ActiveMQ
* The MQTT-Kafka-Bridge-components

## VM-setup

### Installation of VirtualBox

<https://www.virtualbox.org/wiki/Linux_Downloads>

### Installation of Vagrant

<http://www.vagrantup.com/downloads>

### Start VM's

```
cd vagrant
vagrant up
```

You will end up with 3 virtual machines:

* `192.168.33.20 (dev1)`
* `192.168.33.21 (dev2)`
* `192.168.33.22 (dev3)`

### SSH-access

The test-setup uses an insecure SSH-keypair (`vagrant/ssh/insecure_rsa` and `vagrant/ssh/insecure_rsa.pub`). Never use this in a non-testing environment.
SSH-user ist `root`.

After you have started the VM's you can access them by entering:

```
ssh root@192.168.33.20 -i ssh/insecure_rsa
ssh root@192.168.33.21 -i ssh/insecure_rsa
ssh root@192.168.33.22 -i ssh/insecure_rsa
```

## Deployment of components

All components are deployed as docker-containers. Docker will be installed on the VM's so you don't have to install Docker on your host machine.

### Installation of Python and Virtualenv

```
sudo apt-get install python python-dev virtualenv build-essential libffi-dev libxml2-dev libxslt1-dev libssl-dev python-pip
```

### Creation of Virtualenv

Create your virtualenv in an arbitrary directory.

```
virtualenv <VENV_INSTALLATION_PATH>
```

### Activate Virtualenv

Activates Virtualenv for current shell.

```
source <VENV_INSTALLATION_PATH>/bin/activate
```

### Install dependencies

```
cd ansible
pip install -r requirements.txt
```

### Deployment

All deployment-commands must be executed from inside folder *ansible*

To deploy the whole stack, execute:

```
ansible-playbook site.yml
```

There are some configuration-options for each component. See the variable-declarations in folder *group_vars*. You can override each variable on the command-line. To get a clean kafka-installation with purged data-directory you may enter:

```
ansible-playbook -e "kafka_reset=True" site.yml
```

## Ensure everything is working

You may want to do some simple tests to verify that deployment was successful. When you have used the default configuration there should run an instance of each component on every of the three nodes. So you should perform each of the following checks on every node.

**Note:** These tests only ensure that proccesses are running and connectivity is possible (aka "smoke tests"). 

### ActiveMQ

* Subscribe topic
```
mosquitto_sub -h 192.168.33.20 -p 1883  -i 12345 -t just/a/test -q 1 -d
```

* Publish to topic
```
mosquitto_pub -h 192.168.33.20 -p 1883 -i 1234 -m "xyz" -t just/a/test -q 1 -d
```

* Connect via JMX
```
jconsole 192.168.33.20:9447
```
### Zookeeper

* Check if running

```
echo mntr | nc 192.168.33.20 2181
# Should return something like this:
# zk_version    3.4.9-1757313, built on 08/23/2016 06:50 GMT
# ...
# ...
```

* Connect via JMX
```
jconsole 192.168.33.20:9449
```

### Kafka

* Create topic
```
kafka-topics.sh --create --zookeeper 192.168.33.20:2181 --replication-factor 1 --partitions 1 --topic test
```

* Verify topic
```
kafka-topics.sh --zookeeper 192.168.33.11:2181 --describe --topic test
```

* Produce message
```
kafka-console-producer.sh --broker-list 192.168.33.20:9092 --topic test
# Enter text-message now
```

* Consume message
```
kafka-console-consumer.sh --zookeeper 192.168.33.12:2181 --topic test --from-beginning
# new syntax
kafka-console-consumer.sh --bootstrap-server 192.168.33.12:9092 --topic test --from-beginning
```   

* Connect via JMX
```
jconsole 192.168.33.20:9448
```

* List the topics created during deployment
```
kafka-topics.sh --zookeeper 192.168.33.20:2181/kafka --list
```
