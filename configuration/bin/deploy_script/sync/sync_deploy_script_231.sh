SVN_COMMAND="export --username comreleaser --password releasercom --force -r HEAD http://svn.chinascopefinancial.com/ada/main/configuration/bin/deploy_script"

svn $SVN_COMMAND/process_deployment.sh

svn $SVN_COMMAND/qa/deploy_cjobsched.sh
svn $SVN_COMMAND/qa/deploy_datapub.sh
svn $SVN_COMMAND/qa/deploy_datashift.sh 
svn $SVN_COMMAND/qa/deploy_adminservice.sh 
svn $SVN_COMMAND/qa/deploy_logservice.sh

chmod +x *.sh
