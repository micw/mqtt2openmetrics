# MQTT to OpenMetrics

This tool listens on MQTT topics, extracts metrics and tags from it and pushes it to an OpenMetrics endpoint (e.g. to VictoriaMetrics or Prometheus, see https://docs.victoriametrics.com/url-examples.html#apiv1importprometheus).

It is an alternative to Telefrag vom InfluxDB if your only source is MQTT and your only target is Prometheus/VictoriaMetrics since it is way more easy to configure.

## Configuration

Copy application.example.yaml to application.yaml and fit it to your needs.

For reference, have a look at application.example.yaml.

Currently, I use it for my personal needs only. Create an issue if you require more features or need better documentation ;-)

## Running (docker)

```
docker run -it --rm \
  -e TZ=Europe/Berlin \
  -v  /path/to/application.yaml:/app/application.yaml \
  ghcr.io/micw/mqtt2openmetrics:master

```

The timezone should be set if timestamps without timezone are extracted from MQTT payloads. Default is Europe/Berlin.
