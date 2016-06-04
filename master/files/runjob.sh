#!/bin/bash

: ${HADOOP_PREFIX:=/usr/local/hadoop}

echo "Going into HADOOP Root folder..."
cd $HADOOP_PREFIX

bin/hdfs dfs -mkdir -p /user/root
bin/hdfs dfs -put bigram .

#bin/hdfs dfs -cat bigram/input.txt

#bin/hadoop jar share/hadoop/mapreduce/hadoop-mapreduce-examples-2.7.1.jar wordcount bigram/input.txt wordCntRes
bin/hadoop jar bigram/bigram.jar BigramCount bigram/input.txt wordCntRes 4
#bin/hadoop jar bigram/wc.jar WordCount bigram/input.txt wordCntRes 4

#echo "Outputs are at : "
#bin/hdfs dfs -ls wordCntRes

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
