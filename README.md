[![Build Status (5.x)](https://github.com/romanpierson/vertx-web-accesslog/actions/workflows/ci-vert.x-5.x.yml/badge.svg)](https://github.com/romanpierson/vertx-elasticsearch-indexer/actions/workflows/ci-vert.x-5.x.yml)
[![Build Status (4.x)](https://github.com/romanpierson/vertx-web-accesslog/actions/workflows/ci-vert.x-4.x.yml/badge.svg)](https://github.com/romanpierson/vertx-elasticsearch-indexer/actions/workflows/ci-vert.x-4.x.yml)
[![Quality Gate Status](https://sonarcloud.io/api/project_badges/measure?project=romanpierson_vertx-elasticsearch-indexer&metric=alert_status)](https://sonarcloud.io/summary/new_code?id=romanpierson_vertx-elasticsearch-indexer)
[![Awesome](https://cdn.rawgit.com/sindresorhus/awesome/d7305f38d29fed78fa85652e3a63e154dd8e8829/media/badge.svg)](https://github.com/vert-x3/vertx-awesome)

# vertx-elasticsearch-indexer

A verticle that receives index data via event bus and indexes to the corresponding ElasticSearch instance(s). The whole configuration is maintained on the verticle itself.

## Technical Usage

The artefact is published on maven central.

Just add it as a dependency to your project (gradle example)

```xml
dependencies {
	compile 'com.romanpierson:vertx-elasticsearch-indexer:1.2.0'
}
```

## Usage

Just start an instance of ElasticSearchIndexerVerticle with the proper json configuration.

As an example in yaml

```yaml
indexScheduleInterval: 5000
instances:
 - identifier: accesslog
   host: localhost
   port: 9200
   indexMode: STATIC_NAME
   indexNameOrPattern: accesslog
 - identifier: applicationlog_ssl
   host: localhost
   port: 9200
   indexMode: DATE_PATTERN_EVENT_TIMESTAMP
   indexNameOrPattern: applicationlog-yyyyMMdd
   ssl: true
   sslTrustAll: true
   authentication:
     type : basic
     config:
       user : elastic
       password : PleaseChangeMe
```

### Raw Index Event

The verticle receives simple json messages via the event bus that need to have a structure like this

```json
meta
  timestamp			
  instance_identifier
message
  key1 : value 1
  key n : value n
```

The meta data part is required only to decide where to index and having a clean timestamp. This timestamp itself is also added to the actual message values (using field name timestamp).

### Index creation
 
The solution supports three ways of index creation. 

For `IndexMode.STATIC_NAME` you must specify a plain index name. 

For `IndexMode.DATE_PATTERN_EVENT_TIMESTAMP` and `DATE_PATTERN_INDEX_TIMESTAMP` you must specify a pattern that can contain placeholders for year, month and day. The indexer will ensure that each access entry - based on its meta timestamp or the timestamp at actual index time - is indexed to the correct index.

### Authentication

In order to simplify things for now its only possible to use Basic Authentication. ES also supports Authentication via OAuth tokens but this is not supported for now.

Also AWS authentication is planned on the roadmap.

## Setup ES Cluster

In order to simplify testing this project contains two docker-compose setups including each a simple ES cluster and Kibana instance (one version with SSL).

### Define global index template

In theory indexing ES is able to derive its own field mapping but its a better option to create an index template as with this you get the correct datatypes.

```json
PUT _template/template_accesslog
{
  "index_patterns": ["accesslog", "accesslog*"],
  "settings": {
    "number_of_shards": 1
  },
  "mappings": {
    "_doc": {
      "_source": {
        "enabled": true
      },
      "properties": {
        "method": {
          "type": "keyword"
        },
        "status": {
          "type": "integer"
        },
        "duration": {
          "type": "integer"
        },
        "timestamp": {
          "type": "date"
        }
      }
    }
  }
}
```

## Compatibility

Version | ES version
----|------ 
1.0.0 | 6.5.4 (To be checked with earlier 6x versions)
1.1.0 | 6.5.4 (To be checked with earlier 6x versions)

## Known Issues and to be fixed

* Failure handling is implemented BUT there is no mechanism built yet to retry / temporary store the failed entries 

## Changelog

Detailed changelog can be found [here](https://github.com/romanpierson/vertx-elasticsearch-indexer/blob/master/CHANGELOG.md).


