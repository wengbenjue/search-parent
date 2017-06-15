SVN_COMMAND="export --username comreleaser --password releasercom --force -r HEAD http://svn.chinascopefinancial.com/ada/main/configuration/bin/deploy_script"

svn $SVN_COMMAND/process_deployment.sh

svn $SVN_COMMAND/qa/deploy_ada.sh 
svn $SVN_COMMAND/qa/deploy_exportservice.sh
svn $SVN_COMMAND/qa/deploy_search.sh
svn $SVN_COMMAND/qa/deploy_ccas.sh

chmod +x *.sh
