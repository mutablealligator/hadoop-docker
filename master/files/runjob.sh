#!/bin/bash

: ${HADOOP_PREFIX:=/usr/local/hadoop}

echo "Going into HADOOP Root folder..."
cd $HADOOP_PREFIX
pwd

bin/hdfs dfs -mkdir -p input
bin/hdfs dfs -put $HADOOP_PREFIX/bigram/input.txt input/

bin/hdfs dfs -ls input
bin/hdfs dfs -cat input/input.txt

bin/hadoop jar share/hadoop/mapreduce/hadoop-mapreduce-examples-2.7.1.jar wordcount input wordCntRes
bin/hdfs dfs -ls wordCntRes
bin/hdfs dfs -cat wordCntRes/part-r-00000

<<"COMMENT"
  echo "Compiling Wordcount code and creating jar..."
  cd /bigram
  mkdir wordcount_classes
  javac -classpath ${HADOOP_HOME}/hadoop-${HADOOP_VERSION}-core.jar -d wordcount_classes WordCount.java
  jar -cvf wordcount.jar -C wordcount_classes/ .

  echo "Checking if input is available on dfs..."
  bin/hadoop dfs -ls input

  echo "Running the job..."
  bin/hadoop jar wordcount.jar WordCount input output

  echo "Waiting for job to complete..."

  echo "Checking if output is available on dfs..."
  bin/hadoop dfs -ls output
  bin/hadoop dfs -cat output/part-00000
COMMENT
