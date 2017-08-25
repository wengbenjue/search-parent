#!/bin/sh

trap 'echo "You hit ctrl+C! Ignore this deployment process";exit 1' INT

if [ -z $1 ]; then
	echo "WARNING: parameter can't be null, don't run this script directly!"
	exit 0
elif [ $1 = "svn" -o $1 = "war" ]; then
	echo "Start to deployment with type: "$1
else
	echo "Doesn't support deployment type: "$1
	exit 0
fi

#check deploy type, if svn, download codes from svn, if war, copy war to tomcat directory
DEPLOY_TYPE=$1
APP_NAME=$2
SVN_APP_MODULE=$2
TOMCAT_INSTANCE_NAME=$3
SVN_PARENT_MODULE=$4
PROFILE_PARAM=$5
WEB_SERVER_TYPE="apache-tomcat"

if [ $APP_NAME = "logservice" ];then
	SVN_APP_MODULE="logsystem"
fi

SVN_BRANCH_NAME="main"
#SVN_BRANCH_NAME="branches/production/release_2_2_2"
#LOCAL_TOMCAT_DIR="/opt"
LOCAL_TOMCAT_DIR="/usr/local"

SVN_LOCAL_DIR="/app/deploy/svn"/$SVN_BRANCH_NAME
SVN_ROOT_PATH="http://svn.chinascopefinancial.com/ada"/$SVN_BRANCH_NAME
SVN_PATH=$SVN_ROOT_PATH/$SVN_PARENT_MODULE/$SVN_APP_MODULE
LOCAL_PROJECT_HOME=$SVN_LOCAL_DIR/$SVN_PARENT_MODULE/$SVN_APP_MODULE
DEPLOY_TARGET_DIR=$LOCAL_TOMCAT_DIR/$TOMCAT_INSTANCE_NAME/$WEB_SERVER_TYPE/webapps/
BACKUP_PATH=/app/backup/

#war path
if [ $DEPLOY_TYPE = "svn" ]; then
	WAR_PATH=$LOCAL_PROJECT_HOME/target/$APP_NAME.war
else
	WAR_PATH="/app/wars/"$APP_NAME.war
	#check if war file is existing
	if [ -e $WAR_PATH ]; then
        echo "Found war file, continue..."
	else 
        echo "WARNING:" $WAR_PATH "doesn't exist, please upload it to this directory first."
        exit 0
	fi
fi

echo "War path is: " $WAR_PATH
echo "Web apps home is: " $DEPLOY_TARGET_DIR

#if deploy with war, needn't to sync codes from svn
if [ $DEPLOY_TYPE = "svn" ]; then
	echo "========== check out configuration module"
	svn checkout --username comreleaser --password releasercom $SVN_ROOT_PATH/configuration $SVN_LOCAL_DIR/configuration
	
	echo "========== check out app module"
	svn checkout --username comreleaser --password releasercom $SVN_ROOT_PATH/framework/app $SVN_LOCAL_DIR/framework/app
	
	if [ $APP_NAME = "datashift" ];then
		echo "========== check out dataentry module first if deploying datashift"
		svn checkout --username comreleaser --password releasercom $SVN_ROOT_PATH/service/dataentry $SVN_LOCAL_DIR/service/dataentry
	fi
	
	echo "========== check out codes from: " $SVN_PATH
	svn checkout --username comreleaser --password releasercom $SVN_PATH $LOCAL_PROJECT_HOME 
	/home/maven/apache-maven-3.0.4/bin/mvn -f $LOCAL_PROJECT_HOME/pom.xml clean install $PROFILE_PARAM
fi
	
echo "stopping tomcat instance..."
pidlist=`ps -ef|grep $TOMCAT_INSTANCE_NAME/$WEB_SERVER_TYPE |grep -v "grep"|awk '{print $2}'`
if [ -z $pidlist ];then
        echo "can't find tomcat instance: "$TOMCAT_INSTANCE_NAME
else
        echo "stopping..." $pidlist
        kill -9 $pidlist
fi

echo "delete web application..."
rm -rf $DEPLOY_TARGET_DIR$APP_NAME
mv $DEPLOY_TARGET_DIR$APP_NAME.war $BACKUP_PATH$APP_NAME.war.$(date +%Y-%m-%d_%H:%M:%S)

echo "copy war file to tomcat webapps director..."
cp $WAR_PATH $DEPLOY_TARGET_DIR

echo "waiting seconds for copying files"
sleep 3

echo "start tomcat instance: " $TOMCAT_INSTANCE_NAME

sh $LOCAL_TOMCAT_DIR/$TOMCAT_INSTANCE_NAME/apache-tomcat/bin/startup.sh

echo "waiting seconds to verify if tomcat is started..."
sleep 2
pidlist=`ps -ef|grep $TOMCAT_INSTANCE_NAME/$WEB_SERVER_TYPE |grep -v "grep"|awk '{print $2}'`
if [ -z $pidlist ];then
        echo "failed to start tomcat: "$TOMCAT_INSTANCE_NAME
else
        echo "tomcat is successfully started. pid is" $pidlist
fi

echo ======================================================================================================================================
echo "Deployment process has been done with the following parameters:"
echo APP_NAME=$APP_NAME
echo TOMCAT_INSTANCE_NAME=$TOMCAT_INSTANCE_NAME
echo PROFILE_PARAM=$PROFILE_PARAM
echo WEB_SERVER_TYPE=$WEB_SERVER_TYPE
echo SVN_PATH=$SVN_PATH
