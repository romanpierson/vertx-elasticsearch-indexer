# Local Elasticsearch / Kibana Playground Version 7.x (Without SSL)

Dockerized setup for testing 7.x version of Elasticsearch / Kibana

Using latest version `7.17.21`.

Kibana user is `elastic`/`changeme`

ES and Kibana data is persisted in relative folder `data`. If you want to start fresh delete that folder.

## Create / Start

```xml
docker-compose up -d
```

## Connect Kibana to your ES indexes

Login to Kibana with user elastic by accessing [http://localhost:5601](http://localhost:5601)

In order to visualize your indexed data you have to create a so called data view. In order to do this via Kibana UI there has to be indexed data in ES. Chose Analytics -> Discover and press button `Create Data View`. In the dialog name your view and define `Index pattern` and the `timestamp` field.


## Stop /Destroy

```xml
docker-compose down
```
