#!/bin/bash
GIT_DIR="/home/grid/site.git"
BRANCH="public"

while read oldrev newrev ref
do
	  # only checking out the master (or whatever branch you would like to deploy)
	  if [ "$ref" = "refs/heads/$BRANCH" && "$HOSTNAME" = "grid" ]; # only for the server
	  then
		    echo "Ref $ref received. Deploying ${BRANCH} branch to production..."
		    git --work-tree=$TARGET --git-dir=$GIT_DIR checkout -f $BRANCH
        # cd $GIT_DIR && make publish # run our pollen makefile
	  else
		    echo "Ref $ref received. Doing nothing: only the ${BRANCH} branch may be deployed on this server."
	  fi
done
