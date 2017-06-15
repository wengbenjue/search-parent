SVN_COMMAND="export --username comreleaser --password releasercom --force -r HEAD http://svn.chinascopefinancial.com/ada/main/configuration/bin/deploy_script"

svn $SVN_COMMAND/process_deployment.sh

svn $SVN_COMMAND/dp/deploy_datapub.sh
svn $SVN_COMMAND/dp/deploy_datashift.sh 

chmod +x *.sh
