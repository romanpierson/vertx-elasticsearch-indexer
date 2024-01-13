## Changelog


#### 1.0.0

(2019-02-14)

* Initial version

#### 1.1.0

(2020-12-17)

* Upgrade to Vertx 4


### 1.2.0

(2023-12-09)

* Moved package `com.mdac` to `com.romanpierson` (for maven central)
* Upgrade to latest versions
* Moved from Travis CI to Github Actions / Gradle Sonarqube Plugin
* Changed support from ES 6.x towards ES 7.x and higher only (doc type mapping not anymore supported)
* Added BEARER Authentication mode
* Added support for Axiom.co
* Added ES 7.x and 8.x playgrounds
* Removed requirement for jackson databind library

### 1.2.1

(2023-12-10)

* Fixed issues with multiple static indexes prefix caching

### 1.3.0

(2024-01-13)

* Its now possible to define the name for the timestamp using property `indexTimestampFieldName` - default is `@timestamp` - this in axiom internally translates into `_time`
