# di2e-analytics
di2e analytics draft for synchronizing metadata to Elasticsearch as a Kibana friendly index

To test,
 #Create a bridge network
 docker network create elastic-net
 # Launch elastic and expose port 9200
 docker run --net elastic-net -p 9200:9200 docker.elastic.co/elasticsearch/elasticsearch:6.2.2
 # Launch kibana and export port 5601
 docker run --net elastic-net -p 5601:5601 --add-host=elasticsearch:`hostname` docker.elastic.co/kibana/kibana:6.2.2
 # Launch the DIB and create and index and sync it 
 cd $DDF_HOME/bin
 ./dib
 cdr:index
 cdr:sync
