[![Build Status](https://travis-ci.org/dcsolutions/kalinka.svg?branch=master)](https://travis-ci.org/dcsolutions/kalinka)

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

There are 2 different possibilities to deploy *kalinka-pub*:

* as a plugin inside ActiveMQ-Broker (recommended)
* as a standalone service

The way, *kalinka-pub* works, is quite straightforward: If deployed as a standalone service it subscribes several MQTT-topics (more precisely, JMS-queues because we use ActiveMQ as JMS-broker which uses JMS internally). If it runs as an ActiveMQ-plugin it intercepts the broker's *send*-method. In both variants it publishes all received messages to a kafka-cluster. Since JMS-queues and kafka-topics behave in a different way, some kind of mapping is required. See chapter *Topic-Mapping* for details on how to deal with that.

In standalone-mode each *kalinka-pub*-instance may consume from several brokers and each broker should be consumed by multiple *kalinka-pub*-instances (no s.p.o.f.).

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

It is recommended to deploy the basic infrastructure first and create a snapshot afterwards. So you can reset the environment very quickly.
**Note:** All deployment-commands must be executed from inside folder *ansible*

* Set up Docker-daemon, a Docker-registry and pull required images. These are the most time-consuming steps.
```
ansible-playbook infrastructure.yml
```

* Create a snapshot
```
cd ../vagrant
vagrant snapshot save <NAME>
```

* You can restore the snapshot by entering:
```
vagrant snapshot restore --no-provision <NAME>
```
Since we don't want to waste time, the flag `--no-provision` is recommended.

* Now you'll have to decide if you want to run the latest snapshot or build the setup on your own
* For further deployment-options take a look into the playbooks in folder *ansible*

#### Run latest snapshot 

* Run the setup with *kalinka-pub* as seperate process/container (standalone)

```
cd ../ansible
ansible-playbook  -v -e "reset_all=True kalinka_pub_plugin_enabled=False" zookeeper.yml kafka.yml activemq.yml kalinka-pub.yml kalinka-sub.yml
```

* Run the setup with *kalinka-pub* embedded in activemq-broker

```
cd ../ansible
ansible-playbook  -v -e "reset_all=True" zookeeper.yml kafka.yml activemq.yml kalinka-sub.yml
```

#### Self-built

* You have not the permission to push to `dcsolutions` so you'll have to create an account on <https://hub.docker.com> first:
* After creation of account you can build the whole project, including docker images by entering:
```
cd ..
cd <PROJECT_ROOT_DIR>
mvn clean install -DdockerImageBuild=true -DdockerRegistryPush=true -Ddocker.registry.prefix=<NAME_OF_YOUR_DOCKERHUB_ACCOUNT>
```

The deployment is the nearly the same as described before but you'll have to refer to your own docker-account:

* *kalinka-pub*-standalone
```
ansible-playbook  -v -e "reset_all=True kalinka_pub_plugin_enabled=False organization=<NAME_OF_YOUR_DOCKER_ACCOUNT>" zookeeper.yml kafka.yml activemq.yml kalinka-pub.yml kalinka-sub.yml
```

* *kalinka-pub* as plugin
```
ansible-playbook  -v -e "reset_all=True organization=<NAME_OF_YOUR_DOCKER_ACCOUNT>" zookeeper.yml kafka.yml activemq.yml kalinka-sub.yml
```
