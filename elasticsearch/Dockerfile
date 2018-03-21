FROM docker.elastic.co/elasticsearch/elasticsearch:6.2.3
VOLUME /usr/share/elasticsearch/data
COPY --chown=elasticsearch:elasticsearch jvm.options /usr/share/elasticsearch/config/
COPY --chown=elasticsearch:elasticsearch elasticsearch.yml /usr/share/elasticsearch/config/
