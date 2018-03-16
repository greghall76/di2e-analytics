# di2e-analytics
di2e analytics project for synchronizing metadata to Elasticsearch as a Kibana friendly index

To Build:
 Build a configuration of elasticsearch and kibana that have the graph enabled...
 cd elastic && docker build -t docker.elastic.co/elasticsearch/elasticsearch:di2e-cfg
 cd kibana && docker build -t docker.elastic.co/kibana/kibana:di2e-cfg 

 mvn clean install
 cp ecdr-analytics-app/target/ecdr-analytics-app-1.0.kar $DDF_HOME/deploy

To test:
 #Create a bridge network
 docker network create elastic-net
 # Launch elastic and expose port 9200
 # For dev: add  -e "discovery.type=single-node" for a quick start
 # Basic X-Pack only... 
 docker run --net elastic-net -p 9200:9200 docker.elastic.co/elasticsearch/elasticsearch:di2e-cfg
 # Launch kibana and export port 5601
 docker run --net elastic-net -p 5601:5601 --add-host=elasticsearch:`hostname` docker.elastic.co/kibana/kibana:di2e-cfg
 # Launch the DIB and create and index and sync it 
 cd $DDF_HOME/bin
 ./dib
 cdr:index
 cdr:sync
