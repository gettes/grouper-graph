FROM tier/grouper:latest

# this is based on the TIER Grouper Docker distribution

MAINTAINER Michael R Gettes <gettes@ufl.edu>

ENV TZ=America/New_York
RUN ln -snf /usr/share/zoneinfo/$TZ /etc/localtime && echo $TZ > /etc/timezone

COPY ./InCommon.pem $GROUPER_HOME/conf
RUN $JAVA_HOME/bin/keytool -import -file $GROUPER_HOME/conf/InCommon.pem -storepass changeit -keystore $JAVA_HOME/jre/lib/security/cacerts

RUN yum install -y epel-release \
	&& yum update -y \
	&& yum install -y graphviz \
	&& yum clean all \
	&& rm -rf /var/cache/yum 

CMD ["gsh"]
