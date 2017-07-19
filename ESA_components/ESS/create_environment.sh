#!/bin/sh
virtualenv ess_env
. ess_env/bin/activate
pip install uwsgi
pip install flask
pip install numpy
pip install scipy
pip install theano
pip install pytz
pip install tzlocal
