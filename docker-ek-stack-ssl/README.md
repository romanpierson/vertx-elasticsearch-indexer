# Local ElasticSearch / Kibana Playground (SSL) 

## Technical Usage

First time you need to create the self signed certificates

```xml
docker-compose -f create-certs.yml up
```

After that just you the standard commands for docker-compose

```xml
docker-compose up -d
```

```xml
docker-compose down
```

## Open Isses

* Kibana still cannot connect to the secured ES cluster
* Kibana not secured yet