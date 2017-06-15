#!/bin/sh
APP_NAME="search"
TOMCAT_INSTANCE_NAME="search"
SVN_PARENT_MODULE="service"
PROFILE_PARAM="-Pdp"

if [ -z $1 ]; then
	DEPLOY_TYPE="svn"
elif [ $1 = "svn" -o $1 = "war" ]; then
	DEPLOY_TYPE=$1
else
	echo "Doesn't support deployment type: "$1
	exit 0
fi

/app/deploy/process_deployment.sh $DEPLOY_TYPE $APP_NAME $TOMCAT_INSTANCE_NAME $SVN_PARENT_MODULE $PROFILE_PARAM
