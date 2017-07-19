#!/bin/sh
cd $1
. ess_env/bin/activate
uwsgi --ini=ess.ini --mount /extrasensory=ess_wsgi_entry:app
