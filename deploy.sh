#!/bin/bash

mvn clean package
mvn endpoints-framework:openApiDocs
gcloud endpoints services deploy target/openapi-docs/openapi.json
mvn appengine:deploy

