
#!/bin/bash
TARGET="/varl/www/html"
GIT_DIR="/home/grid/site"
BRANCH="public"

while read oldrev newrev ref
do
	  # only checking out the master (or whatever branch you would like to deploy)
    # and only firing on appropriate machine
	  if [ "$ref" = "refs/heads/$BRANCH" && "$HOSTNAME" = "grid"];
	  then
		    echo "Ref $ref received. Deploying ${BRANCH} branch to production..."
		    git --work-tree=$TARGET --git-dir=$GIT_DIR checkout -f $BRANCH
        make all
        make publish
	  else
		    echo "Ref $ref received. Doing nothing: only the ${BRANCH} branch may be deployed on this server."
	  fi
done
