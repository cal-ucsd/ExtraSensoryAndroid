import sys;
import shutil;
import json;
import traceback;
import numpy;

from flask import Flask, render_template, request

from ess_utils import *;
#from classifiers.classifier import Classifier;
#from classifiers.es_mlp import es_mlp;

app							= Flask( __name__ );

app.url_map.strict_slashes 	= False;

####################
g__default_classifier_type	= 'es_mlp';

def get_classifier_instance(classifier_type):
	try:
		cla				= attempt_to_get_classifier_instance(classifier_type);
		return (cla,classifier_type);
	except Exception as e:
		print("Failed to instantiate classifier of type '%s'." % classifier_type);
		print("Caught exception with message: %s" % e.message);
		traceback.print_exc();
		raise e;
#		print("Falling back on the default classifier type: %s" % g__default_classifier_type);
#		cla				= attempt_to_get_classifier_instance(g__default_classifier_type);
#		return (cla,g__default_classifier_type);
		
def attempt_to_get_classifier_instance(classifier_type):
	# Assume classifier_type is the name of a module under 'classifiers'
	# and the name of a class inside that module that inherits class 'classifiers.classifier.Classifier'
	
	# Try to import the module:
	imp					= __import__('classifiers.%s' % classifier_type);
	
	# Try to get the module of the classifier type:
	mod					= getattr(imp,classifier_type);
	
	# Try to get the class (assume it has same name as the module):
	cla_cls				= getattr(mod,classifier_type);
	
	# Try to instantiate a classifier:
	cla					= cla_cls();
	
	return cla;
####################


@app.route('/')
def hello_world_root():
	return "<h1 style='color:red'>This is the ExtraSensory Server - root directory</h1>";

@app.route('/extrasensory')
def hello_world():
	return "<h1 style='color:red'>This is the ExtraSensory Server</h1>";

@app.route('/testing')
def testing():
	uuid = request.args.get( 'uuid' );
	return json.dumps( {'api_type':'feedback','success': True, 'uuid':uuid,'timestamp': int(-134) } );
	
	
@app.route('/extrasensory/upload_sensor_data', methods=[ 'POST' ])
def upload_sensor_data():
	print("-"*20);
	print('---- Upload zip request');
	try:
		classifier_type	= request.args['classifier_type'];
		classifier_name = request.args['classifier_name'];
		uploaded_file 	= request.files[ 'file' ]
		#filename = werkzeug.secure_filename( uploaded_file.filename )
		filename		= uploaded_file.filename;

		d				= filename.find('-')
		UTime			= filename[:d] #unique time identifier
		UUID			= filename[d+1:].replace(".zip","") #unique user identifier

		instance_dir	= get_and_create_upload_instance_dir(UUID,UTime);
		full_zip_path	= os.path.join(instance_dir,filename);
		uploaded_file.save(full_zip_path);
		print '>> saved ', full_zip_path;

		#now predict the activity:
		tmp_dir			= get_tmp_dir_to_unpack_data(UUID,UTime);
		print 'tmp directory will be: %s' % tmp_dir;
		unpack_data_instance(full_zip_path,tmp_dir);
		print 'unpacked data into tmp directory';

		print 'Requested to use classifier_type:%s and classifier_name:%s' % (classifier_type,classifier_name);
		
		(cla,classifier_type)		= get_classifier_instance(classifier_type);
		(label_names,label_probs)	= cla.classify(tmp_dir,UTime,extra_data={'classifier_name':classifier_name});

		# This is to be consistent with original setting, where there was a 'main activity' describing body state:
		(predicted_activity,predicted_prob)	= identify_leading_main_activity_in_predictions(label_names,label_probs);
		
		print "Predicted main-activity: %s (p=%.3f) and UTime: %s from classify_zip." % (predicted_activity,predicted_prob,UTime);
		
		# Try to get a representative location point, if location was collected:
		try:
			loc_lat_long			= get_representative_location(tmp_dir);
			print "Got location lat-long: %s" % str(loc_lat_long);
			pass;
		except Exception as ex:
			loc_lat_long			= None;
			print "Failed to get representative lat long coordinates. Caught exception (%s): %s" % (type(ex).__name__,ex.message);
			traceback.print_exc();
			pass;
		
		shutil.rmtree(tmp_dir);
		print "Removed temporary directory: %s" % tmp_dir;
		
		msg 						= ''
		success						= True;
		pass;
	except Exception as e:
		predicted_activity 			= 'none'
		label_names					= [];
		label_probs					= [];
		loc_lat_long				= None;
		msg							= 'Caught exception (%s): |%s|' % (type(e).__name__,e.message);
		traceback.print_exc();
		success 					= False;
		print msg
		try:
			UTime					= filename[:filename.find('-')]
		except:
			UTime					= 0
			pass;
		pass;
		
	return_string = json.dumps( {\
		'api_type':'upload_sensor_data',\
		'filename':uploaded_file.filename,\
		'success': success,\
		'predicted_activity': predicted_activity,\
		'timestamp': int(UTime),\
		'msg': msg,\
		'label_names':label_names,\
		'label_probs':label_probs,\
		'location_lat_long':loc_lat_long,\
		'classifier_type':classifier_type,\
		'classifier_name':classifier_name} );

	print "ESS returning message:";
	print return_string;
	print("-"*20);
	return return_string;
	

@app.route( '/extrasensory/user_labels' )
def handle_user_labels():
	print("-"*20);
	print '---- feedback from user';
	'''
		Handles saving feedback that is sent from the app.

		Parameters
		----------
		uuid                - UUID of device sending the feedback
								
	  timestamp			- Timestamp of activity in question

		predicted_activity  - The activity our system predicted

		corrected_activity    - The activity the user corrected

		secondary_activities - The set of user secondary activities (separated with commas)

		mood - The mood of the user

		label_source - a string to describe in which mechanism the user supplied these labels

		Results
		-------
		JSON success if all params are present and correctly parsed
		JSON failure if something is not present or incorrectly parsed
	'''

	print("request was: |%s|" % request);
	fback = {}
	try:
		# Check for required parameters
		if 'uuid' not in request.args:
			raise Exception( 'Missing uuid' )
		if 'timestamp' not in request.args:
			raise Exception( 'Missing timestamp')
		if 'predicted_activity' not in request.args:
			raise Exception( 'Missing predicted_activity' )
		if 'corrected_activity' not in request.args:
			raise Exception( 'Missing corrected_activity' )
		if 'secondary_activities' not in request.args:
			raise Exception( 'Missing secondary_activities' )
		if 'moods' not in request.args:
			raise Exception( 'Missing moods' )
		if 'label_source' not in request.args:
			raise Exception( 'Missing label_source' )

		fback[ 'uuid' ]                 	= request.args.get( 'uuid' )
		fback[ 'timestamp' ]		    	= request.args.get( 'timestamp' ) 
		fback[ 'predicted_activity' ]   	= request.args.get( 'predicted_activity' ).upper()
		fback[ 'corrected_activity' ]     	= request.args.get( 'corrected_activity' ).upper()
		fback[ 'secondary_activities' ]     = request.args.get( 'secondary_activities' ).upper().split( ',' )
		fback[ 'moods' ]                    = request.args.get( 'moods' ).upper().split( ',' )
		fback[ 'label_source' ]             = request.args.get( 'label_source' ).upper();

		# Are there any other properties in the request? Add them to the feedback object:
		for key in request.args.keys():
			if key not in fback:
				fback[key] = request.args.get(key);
				pass;
			pass;
		
		
		UUID 	= str(fback['uuid'])
		UTime 	= str(fback['timestamp'])
		instance_dir = get_and_create_upload_instance_dir(UUID,UTime);
		#feats_path 	= os.path.join(current_app.config['CLASSIFIER_FOLDER'],'feats',UUID,UTime)
		#if not os.path.exists(feats_path):
		#	raise Exception( 'Can''t find corresponding data on the server' )
		#else:
		feedback_file = os.path.join(instance_dir,'feedback');
		if os.path.exists(feedback_file):
			fp_in = open(feedback_file,'r');
			old_fback = json.load(fp_in);
			fp_in.close();
				
			if type(old_fback) == list:
				fbacks = old_fback;
				pass;
			else:
				fbacks = [old_fback];
				pass;
			pass;
		else:
			# No older feedback file:
			fbacks = [];
			pass;

		# Add the new feedback to the feedback history:
		fbacks.append(fback);

		fp = open(feedback_file,'w')
		json.dump( fbacks, fp)
		fp.close()


		sys.stdout.flush();
		pass;
	except Exception, exception:
		print 'Caught exception:';
		print exception;
		sys.stdout.flush();
		out_str = json.dumps( {'api_type':'user_labels','success': False, 'timestamp': int(UTime), 'msg': str( exception ) } );
		pass;

	out_str		= json.dumps( {'api_type':'user_labels','success': True, 'timestamp': int(UTime) } ) ;
	print("ESS returning response:");
	print(out_str);
	print("-"*20);

	return out_str;

if __name__ == '__main__':
	app.run();
