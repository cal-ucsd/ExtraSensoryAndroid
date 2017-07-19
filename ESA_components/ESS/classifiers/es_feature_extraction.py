import os,os.path;
import warnings;

import numpy;
import json;
import datetime;

import compute_features;

g__low_freq_measurements	= ['light','pressure','proximity_cm','proximity',\
								'relative_humidity',\
								'battery_level','screen_brightness',\
								'temperature_ambient'];
g__discrete_measurements	= {'on_the_phone':[False,True],\
								'wifi_status':['not_reachable','reachable_via_wifi','reachable_via_wwan'],\
								'app_state':['active','inactive','background'],\
								'battery_state':['unknown','unplugged','not_charging','discharging','charging','full'],\
								'battery_plugged':['ac','usb','wireless'],\
								'ringer_mode':['normal','silent_no_vibrate','silent_with_vibrate']};
g__location_quick_features	= ['std_lat','std_long','lat_change','long_change',\
								'mean_abs_lat_deriv','mean_abs_long_deriv'];
g__audio_properties			= ['max_abs_value','normalization_multiplier'];
g__time_properties			= ['hour_of_day','minute_in_hour'];
g__pseudo_sensors			= ['lf_measurements','location_quick_features','audio_properties','discrete_measurements','time'];
g__feats_needing_log_comp	= ['max_abs_value','normalization_multiplier','light'];
g__lqf_positive_degrees		= ['std_lat','std_long',\
								'mean_abs_lat_deriv','mean_abs_long_deriv'];
g__dummy_main_label			= 'DON_T_REMEMBER';
g__sr						= 40.;
g__g_units_in_1msqs			= 0.101972;
g__sensors_with_3axes		= ['raw_acc','raw_gyro','raw_magnet',\
								'proc_acc','proc_gravity','proc_gyro',\
								'proc_attitude','proc_magnet',\
								'watch_acc'];

def get_feature_dimension_for_sensor_type(sensor):
	if sensor in ['raw_acc','proc_acc']:
		dim     = compute_features.get_dimension_of_features('acc');
	elif sensor in ['raw_gyro','proc_gyro']:
		dim		= compute_features.get_dimension_of_features('gyro');
	elif sensor in ['raw_magnet','proc_magnet']:
		dim		= compute_features.get_dimension_of_features('magnet');
	elif sensor in ['location','watch_acc','watch_compass','proc_attitude','derived_acc','audio_naive']:
		dim     = compute_features.get_dimension_of_features(sensor);

	elif sensor == 'lf_measurements':
		dim     = len(g__low_freq_measurements);
	elif sensor == 'discrete_measurements':
		dim     = get_dim_of_discrete_measurements();
	elif sensor == 'location_quick_features':
		dim     = len(g__location_quick_features);
	elif sensor == 'audio_properties':
		dim     = len(g__audio_properties);
	elif sensor == 'time':
		dim		= len(g__time_properties);
	elif sensor == 'is_iphone':
		dim     = 1;
	else:
		raise ValueError('!!! Unsupported sensor: %s' % sensor);
		pass;

	return dim;

def get_dim_of_discrete_measurements():
    dim         = 0;
    for key in g__discrete_measurements.keys():
        dim     += (len(g__discrete_measurements[key]) + 1); # count extra feature for 'missing key'
        pass;

    return dim;

def get_discrete_feature_name(key,value_or_none):
	if value_or_none is None:
		return 'discrete:%s:missing' % (key);
	return 'discrete:%s:is_%s' % (key,str(value_or_none));
	
def get_discrete_feature_ind(key,value_or_none,feat_names):
	feat_name	= get_discrete_feature_name(key,value_or_none);
	if feat_name not in feat_names:
		raise ValueError("!!! Got unsupported discrete value: %s" % feat_name);
	return feat_names.index(feat_name);
	
def get_discrete_measurements(instance_dir):
	dim         = get_dim_of_discrete_measurements();
	feats       = numpy.zeros(dim,dtype=bool);
	
	# Prepare the feature names:
	feat_names	= [];
	for key in sorted(g__discrete_measurements.keys()):
		values	= g__discrete_measurements[key];
		for value in values:
			feat_name	= get_discrete_feature_name(key,value);
			feat_names.append(feat_name);
			pass;
		# Add 'missing' indicator:
		feat_names.append(get_discrete_feature_name(key,None));
		pass;
	
	input_file  = os.path.join(instance_dir,'m_lf_measurements.json');
	if not os.path.exists(input_file):
		return (feats,feat_names);

	fid         = file(input_file,'rb');
	lf_meas     = json.load(fid);
	fid.close();

	for key in sorted(g__discrete_measurements.keys()):
		if key in lf_meas:
			value   	= lf_meas[key];
			
			# Some exceptional conversions:
			
			if key == 'app_state':
				if value == 0:
					value	= 'active';
					pass;
				elif value == 1:
					value	= 'inactive';
					pass;
				elif value == 2:
					value	= 'background';
					pass;
				pass; # end if key==app_state

			if key == 'battery_state':
				if value == 0:
					value	= 'unknown';
					pass;
				elif value == 1:
					value	= 'unplugged';
					pass;
				elif value == 2:
					value	= 'charging';
					pass;
				elif value == 3:
					value	= 'full';
					pass;			
				pass; # end if key==battery_state
				
			if key == 'wifi_status':
				if value == 0:
					value	= 'not_reachable';
					pass;
				elif value == 1:
					value	= 'reachable_via_wifi';
					pass;
				elif value == 2:
					value	= 'reachable_via_wwan';
					pass;
				elif value == False:
					value	= 'not_reachable';
					pass;
				elif value == True:
					value	= 'reachable_via_wifi';
					pass;
				pass; # end if key==wifi_status
			
			# If android app already reported 'missing value':
			if value	== 'missing':
				value	= None;
				pass;
				
			mark_ind	= get_discrete_feature_ind(key,value,feat_names);
		else:
			mark_ind	= get_discrete_feature_ind(key,None,feat_names);
			pass;
		# Mark the appropriate indicator:
		feats[mark_ind]	= True;
		pass;

	feat_names	= numpy.array(feat_names);
	#feats		= feats.astype(float);
	return (feats,feat_names);
	
def get_pseudo_sensor_features(instance_dir,sensor):
	if sensor == 'discrete_measurements':
		(features,feat_names)	= get_discrete_measurements(instance_dir);
		return (features,feat_names);
    
	input_file  = os.path.join(instance_dir,'m_%s.json' % sensor);
	if sensor == 'lf_measurements':
		expected_features = g__low_freq_measurements;
		pass;
	elif sensor == 'location_quick_features':
		expected_features = g__location_quick_features;
		pass;
	elif sensor == 'audio_properties':
		expected_features = g__audio_properties;
		pass;
	elif sensor == 'time':
		input_file  = os.path.join(instance_dir,'m_lf_measurements.json');
		expected_features = g__time_properties;
	else:
		return None;

	dim         = get_feature_dimension_for_sensor_type(sensor);
	features    = numpy.nan*numpy.ones(dim);
	feat_names	= compute_features.add_prefix_to_feature_names(expected_features,sensor);

	if not os.path.exists(input_file):
		return (features,feat_names);

	fid         = file(input_file,'rb');
	try:
		input_data  = json.load(fid);
		pass;
	except:
		print "!!! problem reading %s" % input_file;
		raise;
    
	fid.close();

	for (fi,feature_name) in enumerate(expected_features):
		if feature_name in input_data:
			value           = input_data[feature_name];
			if feature_name in g__feats_needing_log_comp:
				epsilon     = 0.00001;
				value       = numpy.log(value + epsilon);
				pass;

			if feature_name in g__location_quick_features:
				# Detect and adjust invalid values of location degrees:
				if abs(value) > 0.5:
					# Deviations of more than half a degree are infeasible
					value   = numpy.nan;
					pass;
                
			if feature_name in g__lqf_positive_degrees:
				# These values must be non negative
				if value < 0.:
					value   = numpy.nan;
					pass;
				pass;

			features[fi]    = value;
			pass;
		pass;

	return (features,feat_names);

def read_measurements(instance_dir,sensor,is_iphone):
	timerefs				= None;
	empty_time_series		= numpy.nan*numpy.ones(1);
	sr						= None;
	
	measurements_file       = os.path.join(instance_dir,'m_%s.dat' % sensor);
	if not os.path.exists(measurements_file):
		return (timerefs,empty_time_series,sr);

	# Load the measurements time-series:
	with warnings.catch_warnings():
		warnings.simplefilter("ignore");
		try:
			X                   = numpy.genfromtxt(measurements_file);
			pass;
		except:
			print "!!! Problem reading file %s" % measurements_file;
			raise;
        
		pass;

	if (len(X.shape) <= 0):
		return (timerefs,empty_time_series,sr);

	if (X.size <= 0):
		return (timerefs,empty_time_series,sr);
		
	if (len(X.shape) == 1):
		X = numpy.reshape(X,(1,-1));
		pass;

	if sensor[:3] == 'raw' or sensor[:4] == 'proc':
		# Then the first column is for time reference:
		timerefs            = X[:,0];
		X                   = X[:,1:];
		if sensor in ['raw_acc','proc_acc','proc_gravity'] and not is_iphone:
			# Then lets rescale the acceleration values from m/(s^2) to G:
			X				= g__g_units_in_1msqs * X;
			pass;
		pass;

	if sensor == 'watch_compass':
		if (X.shape[0] <= 1):
			return (timerefs,empty_time_series,sr);
        
		timerefs            = X[:,0]/1000.;
		X                   = X[:,1];

		# Validate that there are at least 2 different time points:
		if (numpy.max(timerefs) <= numpy.min(timerefs)):
			return (timerefs,empty_time_series,sr);
        
		pass;
    
	if sensor == 'watch_acc':
		if X.shape[1] == 4:
			timerefs        = X[:,0]/1000.;
			X               = X[:,1:];
			# Get rid of leftover entries in the beginning of the sequence:
			valid_inds      = numpy.where(timerefs<=timerefs[-1])[0];
			if len(valid_inds) < timerefs.size:
				timerefs    = timerefs[valid_inds];
				X           = X[valid_inds,:];
				pass; # end if len(valid_inds)...
			pass; # end if X.shape...
		else:
			timerefs        = 0.040 * numpy.array(range(X.shape[0])); # (Assuming constant sampling rate of 25Hz)
			pass;
		
		pass;

	sr						= None;
	if sensor in g__sensors_with_3axes:
		if len(timerefs) <= 1:
			return (timerefs,empty_time_series,sr);
		
		# Estimate the average sampling rate:
		dur                 = timerefs[-1] - timerefs[0];
		if dur > 30:
			# Then there's something screwed up with the timerefs. Assume standard duration:
			dur				= 20;
			pass;
		sr                  = float(X.shape[0]) / dur;
		pass;

	return (timerefs,X,sr);

def get_features_from_measurements(instance_dir,timestamp,sensor,is_iphone=False):
#	print "%s:%s" % (instance_dir,sensor);
	
	if sensor == 'is_iphone':
		return (is_iphone,numpy.array(['is_iphone']));
    
	if sensor in g__pseudo_sensors:
		return get_pseudo_sensor_features(instance_dir,sensor);

	if sensor == 'audio_naive':
		return compute_features.get_audio_naive_statistics_of_mfcc(instance_dir);

	# Read measurements:
	(timerefs,X,sr) 	= read_measurements(instance_dir,sensor,is_iphone);
       
	# Calculate features:
	if sensor in ['raw_acc','proc_acc']:
		(features,feat_names)	= compute_features.get_accelerometer_features(X,sr,sensor);
		return (features,feat_names);
	if sensor in ['raw_gyro','proc_gyro']:
		(features,feat_names)	= compute_features.get_gyroscope_features(X,sr,sensor);
		return (features,feat_names);
	if sensor in ['raw_magnet','proc_magnet']:
		(features,feat_names)	= compute_features.get_magnetometer_features(X,sr,sensor);
		return (features,feat_names);
	if sensor == 'location':
		# Remember Andoid uses relative timestamps and iphone uses epoch timestamps:
		validate_timestamps	= is_iphone;
		(features,feat_names)            = compute_features.get_location_features(X,timestamp,validate_timestamps);
		return (features,feat_names);
	if sensor == 'watch_acc':
		(features,feat_names)            = compute_features.get_watch_acc_features(X,sr);
		return (features,feat_names);
	if sensor == 'watch_compass':
		(features,feat_names)	= compute_features.get_watch_compass_features(X);
		return (features,feat_names);
		
	if sensor == 'proc_attitude':
		(features,feat_names)	= compute_features.get_attitude_features(X);
		return (features,feat_names);
	
	raise ValueError("!!! Unsupported sensor: %s" % sensor);
	return;

def get_time_of_day_features_from_timestamps(timestamps,timezone):
	n_examples				= len(timestamps);
	hours					= numpy.zeros(n_examples);
	for ii in range(n_examples):
		dt					= datetime.datetime.fromtimestamp(timestamps[ii],tz=timezone);
		hours[ii]			= dt.hour;
		pass;
	return get_time_of_day_features_from_hours(hours);

def get_time_of_day_features_from_hours(hours):
	n_examples				= len(hours);
	part_lims				= numpy.array([\
		[0,6],\
		[3,9],\
		[6,12],\
		[9,15],\
		[12,18],\
		[15,21],\
		[18,24]]);
	n_parts					= part_lims.shape[0];
	
	is_part					= numpy.zeros((n_examples,n_parts+1),dtype=bool);
	feat_names				= numpy.array([None for pi in range(n_parts+1)]);
	for pi in range(n_parts):
		low					= part_lims[pi,0];
		high				= part_lims[pi,1];
		is_part[:,pi]		= numpy.logical_and(hours>=low,hours<high);
		feat_names[pi]		= 'discrete:time_of_day:between%dand%d' % (low,high);
		pass;
	
	is_part[:,-1]			= numpy.logical_or(numpy.logical_and(hours>=21,hours<=24),numpy.logical_and(hours>=0,hours<=3));
	feat_names[-1]			= 'discrete:time_of_day:between%dand%d' % (21,3);
	
	feats					= is_part.astype(float);
	return (feats,feat_names);

def get_time_of_day_features_for_from_timestamp_single_example(timestamp,timezone):
	(feats,feat_names)		= get_time_of_day_features_from_timestamps([timestamp],timezone);
	return (feats[0,:],feat_names);
	
def get_time_of_day_features_for_from_hour_single_example(hour):
	(feats,feat_names)		= get_time_of_day_features_from_hours(numpy.array([hour]));
	return (feats[0,:],feat_names);
	
	
def get_representative_location(instance_dir):
	(timerefs,locations,sr)	= read_measurements(instance_dir,'location',False);
	lat_long			= compute_features.get_absolute_location_representative(locations);
	if numpy.isnan(lat_long).any():
		lat_long		= None;
		pass;
	else:
		lat_long		= lat_long.tolist();
		pass;
		
	return lat_long;