#!/bin/bash
export JAVA_HOME=/opt/homebrew/opt/openjdk@21
export PATH=$JAVA_HOME/bin:$PATH

mvn clean package 
mvn install:install-file \
  -Dfile=target/aerospike-fluent-client-0.8.0-jar-with-dependencies.jar \
  -DgroupId=com.aerospike \
  -DartifactId=aerospike-fluent-client \
  -Dversion=0.8.0 \
  -Dpackaging=jar

echo "Copying JARS..."
cp target/aerospike-fluent-client-0.8.0-jar-* ../aerospike-fluent-client-workshop/external_jars
echo "Done!"

