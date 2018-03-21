# di2e-analytics
di2e analytics project for synchronizing metadata to Elasticsearch as a Kibana friendly index

#Building:
 Build a configuration of elasticsearch and kibana that have the graph enabled...
 cd elastic && docker build . -t docker.elastic.co/elasticsearch/elasticsearch:di2e-cfg
 cd kibana && docker build . -t docker.elastic.co/kibana/kibana:di2e-cfg 

 mvn clean install

# Deploying
# Launch the DIB
   cd $DDF_HOME/bin
   ./dib
# Deplot the kar
 cp ecdr-analytics-app/target/ecdr-analytics-app-1.0.kar $DDF_HOME/deploy
#OR
 kar:install $BUILD_DIR/ecdr-analytics-app/target/ecdr-analytics-app-1.0.kar

# NOTE: elasticsearch has been giving me memory issues on the two small time hosts I have.
#       so, I created an AWS service deploy of it at ec2-18-219-95-168.us-east-2.compute.amazonaws.com
# The EC2 instance had to be tailored as follows....
 1) update the file handle limits allowed to docker containers from 1024:4096 to something much bigger...
  vi /etc/sysconfig/docker
  OPTIONS="--default-ulimit nofile=1024000:1024000"

 2) hit the traditional O/S user soft and hard file handle limits as well for good measure.
  vi /etc/security/limits.conf and add the following....
  *                soft    nofile          1024000
  *                hard    nofile          1024000

 3) make sure the system as a whole is grabbing the memory
  vi /etc/sysctl.conf
    # max memory segments
    vm.max_map_count=262144
    # max open file handles
    fs.file-max=10240000
# Then reboot the EC2 instance...

# Running / Testing:
  Make sure CKAN is up using docker-compose restart ckan from the ckan/src/contrib/docker dir after you git clone ckan.
  See:  
  Login to CKAN and create a user
  Leave the web tab open so you can capture the user's token
 
  Go to the DDF Amin page 
  Select the Ecdr Analytics App.
    Configure the CKAN host
    Configure the user token for the client to use for its run-as
 
    Configure the Elasticsearch host to ec2-18-219-95-168.us-east-2.compute.amazonaws.com
    
#Bring up Kibana so you can scope your Elastic data.
 # Launch kibana and export port 5601
 docker run --net elastic-net -p 5601:5601 --add-host=elasticsearch:yourip docker.elastic.co/kibana/kibana:di2e-cfg
 # Launch the DIB and create and index and sync it 
