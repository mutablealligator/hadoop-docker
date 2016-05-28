echo "Creating docker machine using virtualbox driver..."
docker-machine create --driver virtualbox vbk
docker-machine env vbk
eval $(docker-machine env vbk)
echo "docker machine created"

echo "Pulling hadoop image 2.7.1. Wait for few minutes..."
docker pull sequenceiq/hadoop-docker:2.7.1
echo "Image downloaded"

echo "Deleting any previous running master..."
docker rm -f master

echo "Starting master..."
docker run -t -d --name master -h master.vbk.com -w /root sequenceiq/hadoop-docker:2.7.1 bash
echo "Container started with name 'master'"

FIRST_IP=$(docker inspect --format="{{.NetworkSettings.IPAddress}}" master)
echo "IP Address of master is : $FIRST_IP"

N=4
i=0
while [ $i -lt $N ]
do
    docker rm -f slave$i &> /dev/null
    echo "Start slave$i container..."
    docker run -t -d --name slave$i -h slave$i.vbk.com -e JOIN_IP=$FIRST_IP sequenceiq/hadoop-docker:2.7.1 bash
    i=$(( $i + 1 ))
done

docker images
docker ps -a

docker exec -it master cd $HADOOP_PREFIX
docker exec -it master bin/hadoop jar share/hadoop/mapreduce/hadoop-mapreduce-examples-2.7.1.jar grep input output 'dfs[a-z.]+'
docker exec -it master bin/hdfs dfs -cat output/*

sleep 10

docker stop slave0
docker rm -f slave0

docker stop slave1
docker rm -f slave1

docker stop slave2
docker rm -f slave2

docker stop slave3
docker rm -f slave3

docker-machine stop vbk
docker-machine rm -y vbk
