import json;
import zipfile;
import os,os.path;

import numpy as np;

import classifiers.es_feature_extraction;

# Load global configuration parameters:
with file('ess_params.json') as fid:
	ess_params			= json.load(fid);
	pass;

g__data_dir				= ess_params['data_dir'];

def get_and_create_upload_instance_dir(uuid,UTime):
	# Create a directory for this instance:
	uuid_dir			= os.path.join(g__data_dir,uuid);
	if not os.path.exists(uuid_dir):
		os.mkdir(uuid_dir);
		pass;

	instance_dir 		= os.path.join(uuid_dir,UTime);
	if not os.path.exists(instance_dir):
		os.mkdir(instance_dir);
		pass;

	return instance_dir;

##########
## Unpacking the data zip file from the mobile app:
def unpack_data_instance(input_zip_file,instance_out_dir):
    if not os.path.exists(instance_out_dir):
        os.mkdir(instance_out_dir);
        pass;

    # Extract the contents of the zip file:
    zf = zipfile.ZipFile(input_zip_file);
    zf.extractall(instance_out_dir);

    # Verify there is the high frequency data file:
    hf_file = os.path.join(instance_out_dir,"HF_DUR_DATA.txt");
    if not os.path.exists(hf_file):
        return False;

    # Read the measurements file and save the different modalities to files:
    (raw_acc,raw_magnet,raw_gyro,\
     proc_timeref,proc_acc,proc_magnet,proc_gyro,proc_gravity,proc_attitude,\
     location,lf_data,watch_acc,watch_compass,\
     location_quick,proc_rotation) = read_datafile(hf_file);

    np.savetxt(os.path.join(instance_out_dir,'m_raw_acc.dat'),raw_acc);
    np.savetxt(os.path.join(instance_out_dir,'m_raw_magnet.dat'),raw_magnet);
    np.savetxt(os.path.join(instance_out_dir,'m_raw_gyro.dat'),raw_gyro);

    np.savetxt(os.path.join(instance_out_dir,'m_proc_timeref.dat'),proc_timeref);

    np.savetxt(os.path.join(instance_out_dir,'m_proc_acc.dat'),proc_acc);
    np.savetxt(os.path.join(instance_out_dir,'m_proc_magnet.dat'),proc_magnet);
    np.savetxt(os.path.join(instance_out_dir,'m_proc_gyro.dat'),proc_gyro);
    np.savetxt(os.path.join(instance_out_dir,'m_proc_gravity.dat'),proc_gravity);
    np.savetxt(os.path.join(instance_out_dir,'m_proc_attitude.dat'),proc_attitude);
    np.savetxt(os.path.join(instance_out_dir,'m_rotation.dat'),proc_rotation);
    
    np.savetxt(os.path.join(instance_out_dir,'m_location.dat'),location);
    np.savetxt(os.path.join(instance_out_dir,'m_watch_acc.dat'),watch_acc);
    np.savetxt(os.path.join(instance_out_dir,'m_watch_compass.dat'),watch_compass);
    
    lf_out_file = os.path.join(instance_out_dir,'m_lf_measurements.json');
    fid = open(lf_out_file,'wb');
    json.dump(lf_data,fid);
    fid.close();
    if len(lf_data) > 0:
        print "++ Created low-frequency measures file";
        pass;

    location_quick_out_file = os.path.join(instance_out_dir,'m_location_quick_features.json');
    fid = open(location_quick_out_file,'wb');
    json.dump(location_quick,fid);
    fid.close();

    # Delete the HF data file from the output directory:
    os.remove(hf_file);

    return True;

def join_data_fields_to_array(jdict,field_names):

    try:
        nf = len(field_names);
        field_dims = [];
        for (fi,name) in enumerate(field_names):
            if name in jdict:
                field_dims.append(len(jdict[name]));
                pass;
            else:
                field_dims.append(-1);
                pass;
            pass;

        data_dim = max(field_dims);
        if data_dim < 0:
            # (If all fields are missing)
            raise Exception;
			
		# Expect all rows to have the same length,
		# so that we can perfectly align them into a single matrix:
        field_dims = np.array(field_dims);
        pos_dims = field_dims[field_dims>0];
        if min(pos_dims) != max(pos_dims):
            print "!!! mismatch in creating data matrix with fields: %s" % field_names;
            raise ValueError("!!! Mismatch in dimensions of fields to integrate.");

        # Prepare a row of nan for missing fields:
        nan_row = [];
        for ii in range(data_dim):
            nan_row.append(np.nan);
            pass;
        
        list_of_rows = [];
        for name in field_names:
            if name in jdict:
                list_of_rows.append(jdict[name]);
                pass;
            else:
                list_of_rows.append(nan_row);
                pass;
            pass;
        arr = np.array(list_of_rows).T;
        pass;
    except:
        arr = np.array([np.nan]);
        pass;

    return arr;

def read_datafile(hf_file):
    # open the file for reading
    fid = open(hf_file, "r");
    jdict = json.load(fid);
    fid.close();

    raw_acc = join_data_fields_to_array(jdict,['raw_acc_timeref','raw_acc_x','raw_acc_y','raw_acc_z']);
    raw_gyro = join_data_fields_to_array(jdict,['raw_gyro_timeref','raw_gyro_x','raw_gyro_y','raw_gyro_z']);
    raw_magnet = join_data_fields_to_array(jdict,['raw_magnet_timeref','raw_magnet_x','raw_magnet_y','raw_magnet_z']);

    proc_acc_time_field = 'processed_user_acc_timeref' if 'processed_user_acc_timeref' in jdict else 'processed_timeref';
    proc_acc = join_data_fields_to_array(jdict,[proc_acc_time_field,'processed_user_acc_x','processed_user_acc_y','processed_user_acc_z']);
    proc_magnet_time_field = 'processed_magnet_timeref' if 'processed_magnet_timeref' in jdict else 'processed_timeref';
    proc_magnet = join_data_fields_to_array(jdict,[proc_magnet_time_field,'processed_magnet_x','processed_magnet_y','processed_magnet_z']);
    proc_gyro_time_field = 'processed_gyro_timeref' if 'processed_gyro_timeref' in jdict else 'processed_timeref';
    proc_gyro = join_data_fields_to_array(jdict,[proc_gyro_time_field,'processed_gyro_x','processed_gyro_y','processed_gyro_z']);

    proc_gravity_time_field = 'processed_gravity_timeref' if 'processed_gravity_timeref' in jdict else 'processed_timeref';
    proc_gravity = join_data_fields_to_array(jdict,[proc_gravity_time_field,'processed_gravity_x','processed_gravity_y','processed_gravity_z']);
    # Iphone attitude:
    proc_attitude = join_data_fields_to_array(jdict,['processed_timeref','processed_roll','processed_pitch','processed_yaw']);
    # Android attitude:
    proc_rotation = join_data_fields_to_array(jdict,['processed_rotation_vector_timeref','processed_rotation_vector_x','processed_rotation_vector_y','processed_rotation_vector_z','processed_rotation_vector_cosine','processed_rotation_vector_accuracy']);

    proc_timeref = join_data_fields_to_array(jdict,['processed_timeref']);

    loc_time_field = 'location_timeref' if 'location_timeref' in jdict else 'location_timestamp';
    location = join_data_fields_to_array(jdict,[loc_time_field,'location_latitude','location_longitude',\
                                              'location_altitude','location_speed',\
                                              'location_horizontal_accuracy','location_vertical_accuracy']);

    # Data from watch:
    if 'watch_acc_timeref' in jdict:
        watch_acc = join_data_fields_to_array(jdict,['watch_acc_timeref','raw_watch_acc_x','raw_watch_acc_y','raw_watch_acc_z']);
        pass;
    else:
        watch_acc = join_data_fields_to_array(jdict,['raw_watch_acc_x','raw_watch_acc_y','raw_watch_acc_z']);
        pass;

    watch_compass = join_data_fields_to_array(jdict,['watch_compass_timeref','watch_compass_heading']);

    if 'low_frequency' in jdict:
        lf_data = jdict['low_frequency'];
        pass;
    else:
        lf_data = {};
        pass;

    if 'location_quick_features' in jdict:
        location_quick = jdict['location_quick_features'];
        pass;
    else:
        location_quick = {};
        pass;
    
    return (raw_acc,raw_magnet,raw_gyro,proc_timeref,proc_acc,proc_magnet,proc_gyro,proc_gravity,proc_attitude,location,lf_data,watch_acc,watch_compass,location_quick,proc_rotation);

def get_tmp_dir_to_unpack_data(UUID,UTime):
	tmp_supdir			= os.path.join(g__data_dir,'tmp');
	if not os.path.exists(tmp_supdir):
		os.mkdir(tmp_supdir);
		print("++ Created superdir for tmp files: %s" % tmp_supdir);
		pass;
	tmp_dir				= os.path.join(tmp_supdir,'%s.%s.tmp' % (UUID,str(UTime)));
	return tmp_dir;

## Unpacking
##########

def standardize_feature_vector(feat_vec,mean_vec,std_vec,replace_missing_with_zero=True):
	normalizers			= np.where(std_vec<=0.,1,std_vec);
	centered			= feat_vec - mean_vec;
	standard			= centered / normalizers;
	if replace_missing_with_zero:
		standard[np.isnan(standard)]	= 0;
		pass;
	
	return standard;

def convert_extrasensory_dataset_label_to_standard_network_label(label):
	if label == 'FIX_walking':
		return 'WALKING';
	if label == 'FIX_running':
		return 'RUNNING';
	if label == 'LOC_main_workplace':
		return 'AT_WORK';
	if label == 'OR_indoors':
		return 'INDOORS';
	if label == 'OR_outside':
		return 'OUTSIDE';
	if label == 'LOC_home':
		return 'AT_HOME';
	if label == 'FIX_restaurant':
		return 'AT_A_RESTAURANT';
	if label == 'OR_exercise':
		return 'EXERCISE';
	if label == 'LOC_beach':
		return 'AT_THE_BEACH';
	if label == 'OR_standing':
		return 'STANDING';
	if label == 'WATCHING_TV':
		return 'WATCHING_TV';

	return label;
	
'''
Convert the label as it appears in the public ExtraSensory Dataset to a pretty and human-readable form,
which should correspond to the original labels as they appear in the menu of the users' mobile ExtraSensory App.
'''
def get_label_pretty_name(label):
	label = convert_extrasensory_dataset_label_to_standard_network_label(label);
	if label.endswith('_'):
		label = label[:-1] + ')';
		pass;

	label = label.replace('__',' (').replace('_',' ');
	label = label[0] + label[1:].lower();
	label = label.replace('i m','I\'m');
	label = label.replace(' tv',' TV');
	return label;

'''
Standardize label name, in the same way the ExtraSensory-App does before it transmits labels through the network
'''
def standardize_label_for_network(label):
	label	= label.replace(' ','_');
	label	= label.replace("'",'_');
	label	= label.replace('(','_');
	label	= label.replace(')','_');
	label	= label.upper();
	return label;

def standardize_extrasensory_dataset_label_names(label_names):
	for (li,label) in enumerate(label_names):
		standard_label	= convert_extrasensory_dataset_label_to_standard_network_label(label);
		label_names[li]	= standard_label;
		#print("=== %d) Converted from '%s' to '%s'" % (li,label,standard_label));
		pass;
	
	return label_names;

def get_label_inds_from_names(all_label_names,desired_label_names,validate=True):
	label_inds	= [];
	for label_name in desired_label_names:
		if validate and label_name not in all_label_names:
			raise ValueError("!!! Asked to locate nonexisting label %s" % label_name);
		label_inds.append(all_label_names.index(label_name));
		pass;
	
	return label_inds;

g__standard_main_activities	= ['LYING_DOWN','SITTING','STANDING','WALKING','RUNNING','BICYCLING'];
def identify_leading_main_activity_in_predictions(label_names,label_probs):
	main_activity_inds		= get_label_inds_from_names(label_names,g__standard_main_activities,validate=False);
	if len(main_activity_inds) <= 0:
		return ('none',0.5);
	
	main_activity_names		= np.array(label_names)[main_activity_inds].tolist();
	main_activity_probs		= np.array(label_probs)[main_activity_inds].tolist();
	winner_i				= np.argmax(main_activity_probs);
	return (main_activity_names[winner_i],main_activity_probs[winner_i]);

def get_representative_location(instance_dir):
	return classifiers.es_feature_extraction.get_representative_location(instance_dir);