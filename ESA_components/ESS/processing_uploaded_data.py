##############################
## Written by Yonatan Vaizman, May 2018.
##############################

import pdb;
import traceback;

import os,os.path;
import ess_utils;
import json;
from classifiers.es_feature_extraction import *;

##############################
## This module provides functions to organize the uploaded data from ExtraSensory App users.
##
## 1) unpack_user_uploaded_data: run this for each user to unpack the sensor data and feedback files into organized directories
##	(one dir for instance), including nice, readable, textual files of the sensor measurements, and the user-reported labels.
##	If you adjust the code of the ExtraSensory App to include additional features, remember to adjust also this function,
##	to incorportate your additional information.
##	You can use the resulted sensor-measurement files to explore new methods for feature-extraction.
##
## 2) collect_user_data_and_save_csv_file: run this to produce a CSV file for a user,
##	including extracted features and user-reported labels from all the user's (unpacked) examples.
##	The resulted CSV file should be in a similar format to the primary data files in the ExtraSensory Dataset.
##	Notice, that for this function you need to decide in advance what are the labels that you want in the table.
##	Notice, as well, that after creating the CSV, you may apply your own heuristics for when to consider some labels "missing".
##	Notice, as well, that the labels.json files of the unpacked data may contain new fields that were not used for the original ExtraSensory Dataset collection - 
##		these include fields that the revised ExtraSensory App logs about the user's interaction with the app:
##		"timestampOfOpeningFeedbackForm"
##		"timestampOfPressingSendFeedbackButton"
##		"timestampOfNotificationAppear"
##		"timestampOfUserRespondToNotification"
##		"timestampOfSendingFeedback"
##		you may want to adjust the code here to collect also these fields into the summarized CSV file.
##

'''
Unpack the uploaded zip files (and label files) from a participating user,
to nice, readable files with the sensor measurements.

Input:
uuid: string. The unique user identifier for the user.
upload_dir: string. The path to the directory, where all the uploaded zipped data are stored.
unpack_dir: string. The path to the directory, where you wish to unpack the data. (for each user, a subdirectory will be created).
'''
def unpack_user_uploaded_data(uuid,upload_dir,unpack_dir):
	# Source directory for the user's data:
	src_user_dir = os.path.join(upload_dir,uuid);
	if not os.path.exists(src_user_dir):
		raise ValueError("!!! The upload directory %s doesn't exist." % src_user_dir);
		
	# Destination directory for the user's unpacked data:
	dst_user_dir = os.path.join(unpack_dir,uuid);
	if not os.path.exists(dst_user_dir):
		os.mkdir(dst_user_dir);
		print("++ Created directory: %s" % dst_user_dir);
		pass;
	
	# Go over the instances uploaded for this user, and unpack each of them:
	subdirs		= sorted(os.listdir(src_user_dir));
	for (si,subdir) in enumerate(subdirs):
		print("instance %d (out of %d). Timestamp: %s" % (si,len(subdirs),subdir));
		src_instance_dir = os.path.join(src_user_dir,subdir);
		dst_instance_dir = os.path.join(dst_user_dir,subdir);
		# The zip file with sensor-data:
		zip_file = os.path.join(src_instance_dir,'%s-%s.zip' % (subdir,uuid));
		if not os.path.exists(zip_file):
			print("-- Zip file doesn't exist: %s" % zip_file);
			continue;
		ess_utils.unpack_data_instance(zip_file,dst_instance_dir);
		# The labels file:
		convert_feedback_file_to_labels_file(src_instance_dir,dst_instance_dir);
		pass;
	
	return;

'''
Collect sensor-features and user-reported labels from the unpacked data directories of a user,
and summarize them all in a CSV file, like the primary data files of the ExtraSensory Dataset.
Notice, that you need to know in advance what are the possible labels that the user reported,
and you need to supply to this function a list of the labels that you wish to collect data about.
For your convenience, this module includes some functions to give you the lists of labels from the original ExtraSensory App
(look at get_default_list_of_labels()).

Input:
uuid: string. The unique user identifier for the user.
unpack_dir: string. The path to the directory, where you wish to unpack the data. (for each user, a subdirectory should be under this dir).
uuids_csvs_dir: string. The path to the destination directory, where you wish to save the users' CSV files.
interest_labels: list of strings. The names of the labels that you are interested in following.
	The user's reportings will only be collected for these specific labels.
sensors: list of strings. The names (codenames) of the sensors, for which you wish to calculate features.
	If you don't provide this parameter, a default list of sensors will be assumed - the one from the ExtraSensory Dataset.
add_time_of_day_features: boolean. Should we calculate time-of-day features and add them to the features in the final CSV? Default - True.
'''
def collect_user_data_and_save_csv_file(uuid,unpack_dir,uuids_csvs_dir,interest_labels,sensors=None,add_time_of_day_features=True):
	if sensors is None:
		sensors				= get_default_list_of_sensors();
		pass;
		
	uuid_unpack_dir			= os.path.join(unpack_dir,uuid);
	
	# Read the data from the individual per-instance directories:
	(timestamps,X,Y,missing_label_mat,label_sources,\
	feat_names,label_names)	= read_user_features_and_labels(uuid_unpack_dir,sensors,interest_labels,add_time_of_day_features);
	
	# Write all the data from the user to a single csv file:
	write_user_features_and_labels_to_csv(uuids_csvs_dir,uuid,timestamps,X,Y,missing_label_mat,label_sources,feat_names,label_names);
	
	return;
	
##############################
## Helper functions:

g__label_source_values		= [\
	'ACTIVE_START','ACTIVE_CONTINUE',\
	'HISTORY',\
	'NOTIFICATION_BLANK','NOTIFICATION_ANSWER_CORRECT','NOTIFICATION_ANSWER_NOT_EXACTLY',\
	'NOTIFICATION_ANSWER_CORRECT_FROM_WATCH'];

def get_default_list_of_sensors():
	sensors	= [\
	'raw_acc',\
	'proc_gyro',\
	'watch_acc',\
	'location',\
	'location_quick_features',\
	'audio_naive',\
	'discrete_measurements',\
	'lf_measurements',\
	];
	
	return sensors;

def get_classic_main_labels():
	main_labels	= ["Lying down", "Kneeling", "Sitting", "Standing", "Walking", "Running", "Bicycling"];
	return main_labels;

def get_classic_secondary_labels():
	sec_labels	= ["Dancing", "Stairs - going up", "Stairs - going down", "Weights workout", "Playing ballgame",\
	"Skateboarding", "Exercising", "Stretching", "Yoga", "Cardio workout", "Treadmill", "Stationary bike", "Playing sports",\
	"Watching sports", "Cooking", "Cleaning", "Doing laundry", "Vacuuming", "Washing dishes", "Listening to music",\
	"Listening to audio", "Using earphones", "Playing videogames", "Playing phone-games", "Watching TV",\
	"Playing musical instrument", "Singing", "Strolling", "Hiking", "Shopping", "Talking", "Drinking (alcohol)",\
	"Smoking", "Eating", "Drinking (non-alcohol)", "Sleeping", "Toilet", "Bathing - bath", "Bathing - shower", "Grooming",\
	"Dressing", "Lab work", "Written work", "Surfing the internet", "Computer work", "Studying", "Teaching", "In class",\
	"In a meeting", "At home", "At work", "At school", "At a bar", "At a party", "At a sports event", "At the beach",\
	"At the pool", "At the gym", "At a restaurant", "At a live performance", "Outside", "Indoors", "On a bus", "On a plane",\
	"On a train", "Elevator", "Motorbike", "Drive - I'm the driver", "Drive - I'm a passenger", "In a car", "Commuting",\
	"Phone in pocket", "Phone in hand", "Phone in bag", "Phone on table", "Phone away from me", "Phone - someone else using it",\
	"Phone strapped", "On a date", "Walking the dog", "With co-workers", "With family", "With friends", "With kids",\
	"With a pet", "Taking care of kids", "Taking care of pet"];
	return sec_labels;

def get_classic_mood_labels():
	mood_labels	= ["Active", "Afraid", "Alert", "Amused", "Angry", "Ashamed", "Attentive", "Bored", "Calm", "Crazy",\
	"Determined", "Disgusted", "Distressed", "Dreamy", "Energetic", "Enthusiastic", "Excited", "Frustrated", "Guilty",\
	"Happy", "High", "Hostile", "Hungry", "In emotional pain", "In love", "In physical pain", "Inspired", "Interested",\
	"Irritable", "Jittery", "Lonely", "Nervous", "Normal", "Nostalgic", "Optimistic", "Physically sick", "Proud",\
	"Romantic", "Sad", "Scared", "Serious", "Sexy", "Sleepy", "Stressed", "Strong", "Tired", "Untroubled", "Upset", "Worried"];
	return mood_labels;

def get_default_list_of_labels(get_main=True,get_secondary=True,get_mood=False):
	labels	= [];
	if get_main:
		labels.extend(get_classic_main_labels());
		pass;
	if get_secondary:
		labels.extend(get_classic_secondary_labels());
		pass;
	if get_mood:
		labels.extend(get_classic_mood_labels());
		pass;
	
	return labels;
	
def standardize_label(label):
    label   = label.upper();
    label   = label.replace(' ','_');
    label   = label.replace("'",'_');
    label   = label.replace('(','_');
    label   = label.replace(')','_');

    return label;

def get_label_source_code(feedback):
	label_source_key		= "label_source";
	if label_source_key not in feedback:
		raise KeyError("!!! User feedback missing key '%s'" % label_source_key);

	raw_label_source		= feedback[label_source_key];
	label_source			= raw_label_source.replace('ES_LABEL_SOURCE_','');
	label_source			= label_source.replace('ACTIVE_FEEDBACK_','ACTIVE_');
	if label_source not in g__label_source_values:
		raise ValueError('!!! Got unsupported label source: %s' % (label_source));
	
	lscode					= g__label_source_values.index(label_source);
	return lscode;
	
def get_instance_labels_from_feedback_file(instance_dir):
    feedback_file           = os.path.join(instance_dir,'feedback');
    if not os.path.exists(feedback_file):
        return None;

    fid = file(feedback_file,'rb');
    try:
        feedback_list       = json.load(fid);
        pass;
    except:
        print "!!! Problem reading feedback file: %s" % feedback_file;
        raise;
    
    fid.close();
	# The file may contain a list of updates of the user-feedback.
    # Get the most up-to-date feedback:
    if (type(feedback_list) == list):
        feedback            = feedback_list[-1];
        pass;
    else:
		# there's no list, just a single user feedback:
        feedback            = feedback_list;
        pass;

    main_activity           = feedback.pop('corrected_activity',None);
    main_activity           = standardize_label(main_activity);
    
    secondary_activities    = [];
    if 'secondary_activities' in feedback:
        for act in feedback['secondary_activities']:
            if len(act) > 0:
                secondary_activities.append(standardize_label(act));
                pass; # end if len(act)....
            pass; # end for act...
        pass; # end if secondary in feedback...

    moods                   = [];
    if 'moods' in feedback:
        for mood in feedback['moods']:
            if len(mood) > 0:
                moods.append(standardize_label(mood));
                pass; # end if len(mood)...
            pass; # end for mood
        pass; # end if moods in feedback...
    
	lscode					= get_label_source_code(feedback);
	
	# Put the standardized fields back in the structure:
	feedback['main_activity'] 			= main_activity;
	feedback['secondary_activities']	= secondary_activities;
	feedback['moods']					= moods;
	feedback['label_source']			= lscode;

	return feedback;

'''
This function is to convert the feedback files (with the user-reported labels) to a more readable, nicer format.
'''
def convert_feedback_file_to_labels_file(src_instance_dir,dst_instance_dir):
	try:
		label_data = get_instance_labels_from_feedback_file(src_instance_dir);
		pass;
	except Exception as ex:
		raise ValueError("!!! Failed reading feedback file from dir %s. Got exception: %s" % (src_instance_dir,ex));
	
	if label_data is None:
		# Then there is no feedback file.
		return;
	
	label_file	= os.path.join(dst_instance_dir,'labels.json');
	fid			= file(label_file,'wb');
	json.dump(label_data,fid);
	fid.close();
	
	print("++ Wrote labels file: %s" % label_file);
	return;

def get_instance_labels(instance_dir):
	labels_file			= os.path.join(instance_dir,'labels.json');
	if not os.path.exists(labels_file):
		return (None,[],[],-1); # -1 is the code for "user didn't reported labels for this instance"
		
	fid					= file(labels_file,'rb');
	labels_data			= json.load(fid);
	fid.close();

	main_activity		= labels_data['main_activity'];
	sec_activities		= labels_data['secondary_activities'];
	moods				= labels_data['moods'];
	lscode				= labels_data['label_source'];
	return (main_activity,sec_activities,moods,lscode);

def read_user_features_and_labels(uuid_unpack_dir,sensors,interest_labels,add_time_of_day_features):
	# First, collect the features from the user's examples:
	print("="*40);
	print("Extracting features from the sensors and collecting them...");
	print("Sensors: [%s]" % ','.join(sensors));
	(timestamps,X,feat_names)	= read_user_features(uuid_unpack_dir,sensors,add_time_of_day_features);
	n_ex						= len(timestamps);
	
	# Then, for each example, collect the labels, focusing on the list of desired interest_labels:
	print("="*40);
	print("Collecting user-reported labels...");
	interest_labels				= [standardize_label(label) for label in interest_labels];
	n_labels					= len(interest_labels);
	Y							= numpy.zeros((n_ex,n_labels),dtype=int); # Default is "label is not relevant"
	missing_label_mat			= numpy.ones((n_ex,n_labels),dtype=int); # Default is "label information missing"
	lscodes						= -1 * numpy.ones(n_ex,dtype=int);
	for i in range(n_ex):
		timestamp				= timestamps[i];
		instance_dir			= os.path.join(uuid_unpack_dir,'%d' % timestamp);
		(main_activity,sec_activities,moods,lscode)	= get_instance_labels(instance_dir);
		
		# The UI method of reporting labels:
		lscodes[i]				= lscode;
		
		# Collect all the labels (main, secondary, mood) that the user reported:
		user_labels				= [];
		if main_activity is not None:
			user_labels.append(main_activity);
			pass;
		user_labels.extend(sec_activities);
		user_labels.extend(moods);
		# Standardize the reported label names:
		user_labels				= [standardize_label(label) for label in user_labels];
		# If user reported any labels, consider all the interest labels not-missing:
		if len(user_labels) > 0:
			missing_label_mat[i,:]	= 0;
			pass;
			
		# Go over the user reported labels, and mark the ones that are part of the interest labels:
		for label in user_labels:
			if label in interest_labels:
				Y[i,interest_labels.index(label)]	= 1;
				pass;
			pass;
			
		pass;
	
	# After going over all the user's examples, identify labels that were never reported, and consider them always missing:
	label_counts	= numpy.sum(Y,axis=0);
	never_reported	= (label_counts <= 0);
	missing_label_mat[:,never_reported]	= 1;
	
	return (timestamps,X,Y,missing_label_mat,lscodes,feat_names,interest_labels);
	
def read_user_features(uuid_dir,sensors,add_time_of_day_features):
	subdirs		= sorted(os.listdir(uuid_dir));
	
	timestamps	= [];
	X_parts		= [];
	feat_names	= None;
	for (si,subdir) in enumerate(subdirs):
		timestamp	= int(subdir);
		print("Calculating features for instance %s (%d out of %d)" % (subdir,si,len(subdirs)));
		instance_dir= os.path.join(uuid_dir,subdir);
		try:
			(feat_vec_i,feat_names_i)	= read_user_instance_features(instance_dir,timestamp,sensors,add_time_of_day_features);
			# Verify getting the same feature names:
			if feat_names is None:
				feat_names	= feat_names_i;
				pass;
			else:
				if len(feat_names) != len(feat_names_i):
					raise ValueError("!!! Inconsistent feature names length. %s" % instance_dir);
				for fi in range(len(feat_names)):
					if feat_names[fi] != feat_names_i[fi]:
						raise ValueError("!!! Inconsistent feature %d: %s vs. %s" % (fi,feat_names[fi],feat_names_i[fi]));
					pass;
					
				pass;
			
			X_parts.append(numpy.reshape(feat_vec_i,(1,len(feat_names))));
			timestamps.append(timestamp);
			pass;
		except Exception as ex:
			print("-- Failed extracting features from %s. exception: %s" % (instance_dir,ex));
			traceback.print_stack();
			raise ex;
			pass;
		pass;
	
	X			= numpy.concatenate(tuple(X_parts),axis=0);
	return (timestamps,X,feat_names);
	
def read_user_instance_features(instance_dir,timestamp,sensors,add_time_of_day_features):
	feat_parts	= [];
	feat_names	= [];
	for sensor in sensors:
		(feat1,feat_names1)	= get_features_from_measurements(instance_dir,timestamp,sensor);
		feat_parts.append(feat1);
		feat_names.extend(feat_names1);
		pass;
	
	if add_time_of_day_features:
		(time_feats,time_feat_names)= get_features_from_measurements(instance_dir,timestamp,'time');
		hour						= time_feats[0];
		minute						= time_feats[1];
		(tod_feats,tod_feat_names)	= get_time_of_day_features_for_from_hour_single_example(hour);
		feat_parts.append(tod_feats);
		feat_names.extend(tod_feat_names);
		pass;
	
	feat_vec	= numpy.concatenate(tuple(feat_parts));
	return (feat_vec,feat_names);
	

def write_user_features_and_labels_to_csv(uuids_csvs_dir,uuid,timestamps,X,Y,missing_label_mat,label_sources,feat_names,label_names):
	print("="*40);
	print("Summarizing user %s data in single file..." % uuid);
	# Rearrange it all in a single matrix:
	
	# Initialize the table:
	delimiter				= ',';
	n_ex					= X.shape[0];
	header					= 'timestamp';
	val_mat					= numpy.reshape(timestamps,(n_ex,1));
	format					= '%.0f';
	
	# Add the features:
	header					+= delimiter + delimiter.join(feat_names);
	val_mat					= numpy.concatenate((val_mat,X),axis=1);
	format					+= delimiter + delimiter.join(['%f' for feat in feat_names]);
	
	# Add the labels:
	lab_prefix				= 'label';
	adjusted_label_names	= ['%s:%s' % (lab_prefix,lab) for lab in label_names];
	lab_mat					= numpy.where(missing_label_mat,numpy.nan,Y);
	header					+= delimiter + delimiter.join(adjusted_label_names);
	val_mat					= numpy.concatenate((val_mat,lab_mat),axis=1);
	format					+= delimiter + delimiter.join(['%.0f' for lab in adjusted_label_names]);
	
	# Add label source code:
	header					+= delimiter + 'label_source';		
	sources_codes			= numpy.reshape(label_sources,(n_ex,1));
	val_mat					= numpy.concatenate((val_mat,sources_codes),axis=1);
	format					+= delimiter + '%.0f';
	
	out_file				= os.path.join(uuids_csvs_dir,'%s.features_labels.csv.gz' % uuid);
	numpy.savetxt(out_file,val_mat,delimiter=delimiter,header=header,comments='',fmt=format);
	print(">> Saved data to file %s" % out_file);
	return;

##############################

