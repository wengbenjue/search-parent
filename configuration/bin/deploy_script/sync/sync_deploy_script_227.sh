SVN_COMMAND="export --username comreleaser --password releasercom --force -r HEAD http://svn.chinascopefinancial.com/ada/main/configuration/bin/deploy_script"

svn $SVN_COMMAND/process_deployment.sh

svn $SVN_COMMAND/dev/deploy_ada.sh 
svn $SVN_COMMAND/dev/deploy_exportservice.sh
svn $SVN_COMMAND/dev/deploy_search.sh
svn $SVN_COMMAND/dev/deploy_ccas.sh

chmod +x *.sh
