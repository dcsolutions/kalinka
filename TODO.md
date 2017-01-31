# Topics

## mqtt-attached-device to mqtt-attached-device

```
mqtt/<SRC_MQTT_CLIENT_ID>/mqtt/<DEST_MQTT_CLIENT_ID> ->  mqtt.mqtt [key: <DEST_MQTT_CLIENT_ID>] (Header: <SRC_MQTT_CLIENT_ID>)
```

## mqtt-attached-device to kafka-attached-system

```
mqtt/<SRC_MQTT_CLIENT_ID>/<DEST_SYSTEM_NAME>         ->  mqtt.<DEST_SYSTEM_NAME> (Header: <SRC_MQTT_CLIENT_ID>)
```
e.g.:
```
mqtt/<SRC_MQTT_CLIENT_ID>/spark_cluster              ->  mqtt.spark_cluster (Header: <SRC_MQTT_CLIENT_ID>)
```

## kafka-attached-system to mqtt-attached-device

```
<DEST_SYSTEM_NAME>/mqtt/<DEST_MQTT_CLIENT_ID>        ->  <DEST_SYSTEM_NAME>.mqtt [key: <DEST_MQTT_CLIENT_ID>)
```
e.g.:
```
spark_cluster/mqtt/<DEST_MQTT_CLIENT_ID>             ->  spark_cluster.mqtt [key: <DEST_MQTT_CLIENT_ID>)

```

# Testszenario

* 3 Clients (Client-ID muss so gewählt werden, dass jede Client-ID auf eine andere Partition gehashed wird)
* Clients verbinden sich:
  * Client1 -> dev1
  * Client2 -> dev2
  * Client3 -> dev3
 
* Clients senden:
  * Client1 -> Client2, Client3 (mqtt.client1.mqtt.client2/3)
  * ...
  
* Clients subscriben sich:
  * Client1 -> mqtt.+.mqtt.client1
  * ...
  
```
{
  from: "client1",
  to: "client2"
}
