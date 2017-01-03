A maven plugin restructuring projects to remove non-desired files from artifacts


## Build

```
mvn clean install
```
 
## Run
 
```
mvn clean \
 me.escoffier.maven:vertx-artifact-sanitizer-maven-plugin:1.0-SNAPSHOT:generate-sanitized-pom \
 me.escoffier.maven:vertx-artifact-sanitizer-maven-plugin:1.0-SNAPSHOT:build-with-sanitized-pom
``` 

## Configuration
 
Use `-Dsanitizer.config` to point to a Yaml file with the following structure:
 
```
# Excluded dependencies groupId:artifactId
excluded-dependencies:
  - io.vertx:vertx-lang-js
  - io.vertx:vertx-lang-groovy
  - io.vertx:vertx-lang-ceylon
  - io.vertx:vertx-lang-kotlin
  - io.vertx:vertx-lang-ruby
  - io.vertx:vertx-lang-scala

# Excluded resources - path relative to the project root, ${module} is the module name.
excluded-resources:
 - src/main/resources/${module}/*.rb
 - src/main/resources/${module}-js/*.js

# Plugin to remove from the build
excluded-plugins:
 - kotlin-maven-plugin
 - jsdoc3-maven-plugin
 - gem-maven-plugin

# Profile to activate when generating the pom
profiles:
  - docs
  - release

# Project version, ${version} is the project version, so ${version}-redhat append the redhat
# suffix
version: ${version}
# artifactId: the new artifactId
# groupId: the new artifactId
# parent-version: the new parent version  
``` 