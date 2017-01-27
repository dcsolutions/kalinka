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
