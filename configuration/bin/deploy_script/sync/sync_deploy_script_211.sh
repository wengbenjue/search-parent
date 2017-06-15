SVN_COMMAND="export --username comreleaser --password releasercom --force -r HEAD http://svn.chinascopefinancial.com/ada/main/configuration/bin/deploy_script"

svn $SVN_COMMAND/process_deployment.sh

svn $SVN_COMMAND/qapub/deploy_ada.sh 
svn $SVN_COMMAND/qapub/deploy_exportservice.sh
svn $SVN_COMMAND/qapub/deploy_search.sh
svn $SVN_COMMAND/qapub/deploy_indexer.sh 
svn $SVN_COMMAND/qapub/deploy_ccas.sh

chmod +x *.sh
