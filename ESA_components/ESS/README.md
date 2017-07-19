# ExtraSensoryServer

# Things you need to prepare:
1. In the directory where you place the ExtraSensoryServer, run the create_environment.sh script.

2. If you want to run the server manually (this is for initial trial):
2.1. Activate the python environment:
	source ess_env/bin/activate
2.2. Manually run the uwsgi command to make the application listen to a certain port:
	uwsgi --socket 0.0.0.0:8080 --protocol=http --mount -w ess_wsgi_entry:app
2.3. Now you can try in a browser to access your server at the listened port (8080 in this example).
2.4. You can stop the app with ctrl+c, and get out of the python environment by:
	deactivate

3. If you want the application to continuously run in the background, the setting may depend on the operating system and on your preferences.
There are many ways. One optional setting is to use nginx in combination with Upstart or systemd (depending on your OS).
nginx will be the general web server (and reverse proxy) for the machine.
It can handle both secured (https) and unsecured (http) communication and redirect the relevant requests to the ExtraSensoryServer app.
systemd is a mechanism to keep the app (or actually the uwsgi command) running continuously in the background, to keep the app available.
3.1. nginx setting:
Place a settings file in the appropriate configuration directory for nginx (something like /etc/nginx/), under sites-available.
We provide a template file called .............. You need to edit this file and place the relevant server name and directory names.
Link that file to a file with the same name under sites-enabled to make this website enabled:
	sudo ln -s /etc/nginx/sites-available/extrasensory /etc/nginx/sites-enabled/extrasensory
3.2. systemd:


https, certificcates....
file ownerships (group www-data) and directory for upload....