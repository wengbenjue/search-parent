SVN_COMMAND="export --username comreleaser --password releasercom --force -r HEAD http://svn.chinascopefinancial.com/ada/main/configuration/bin/deploy_script"

svn $SVN_COMMAND/process_deployment.sh

svn $SVN_COMMAND/dp/deploy_indexer.sh
svn $SVN_COMMAND/dp/deploy_cjobsched.sh
svn $SVN_COMMAND/dp/deploy_adminservice.sh 
svn $SVN_COMMAND/dp/deploy_logservice.sh

chmod +x *.sh
