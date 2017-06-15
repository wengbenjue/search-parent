#!/bin/sh
APP_NAME="adminservice"
TOMCAT_INSTANCE_NAME="adminservice"
SVN_PARENT_MODULE="tools"
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