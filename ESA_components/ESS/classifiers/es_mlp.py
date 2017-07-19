import numpy as np;
import pickle;
import os,os.path;
import pytz;
import tzlocal;

from es_feature_extraction import *;
import ess_utils;
import neural_networks;

from classifier import Classifier;

'''
This class represents a classifier that uses sensor-feature in the same format as in the ExtraSensory Dataset.
This classifier also uses a Multiple Layer Perceptron (MLP) model to make the predictions.
Specifically, it uses a type of MLP that we nickname Multiple Input Classifier (MIC), where each input brach represents a sensor,
and it handles cases when some of the sensors are missing.
'''
class es_mlp(Classifier):

	def __init__(self):
		return; # This class has no data members.

	def classify(self,measurements_dir,UTime,extra_data=None):
		timestamp		= int(UTime);
		
		# Load the trained model (whose name may be given as a parameter):
		classifier_name	= extra_data['classifier_name'] if (extra_data is not None) else 'es6sensors';
		model_file		= os.path.join(os.path.dirname(__file__),'trained_models','es_mlp','%s.model.pickle' % classifier_name);
		if not os.path.exists(model_file):
			raise ValueError("Got bad classifier_name: %s. Missing model file: %s" % (classifier_name,model_file));
			
		print("Loading trained model from %s ..." % model_file);
		with file(model_file) as fid:
			cla			= pickle.load(fid);
			pass;
		
		label_names		= cla['label_names'];
		label_names		= ess_utils.standardize_extrasensory_dataset_label_names(label_names);
		n_labels		= len(label_names);
		
		# Calculate the required features for this classifier:
		sensors			= cla['model_params']['sensors'];
		print("The model relies on ExtraSensory features from the following sensors: %s" % str(sensors));
		sensors_feat	= [];
		for sensor in sensors:
			(feat,feat_names)	= get_features_from_measurements(measurements_dir,timestamp,sensor);
			sensors_feat.append(feat);
			pass;

		if 'add_time_of_day_features' in cla['model_params'] and cla['model_params']['add_time_of_day_features']:
			(time_feats,time_feat_names)	= get_features_from_measurements(measurements_dir,timestamp,'time');
			print("=== Got time features: ");
			print(zip(time_feat_names,time_feats));
			if np.isnan(np.sum(time_feats)):
#				timezone					= pytz.timezone(tzlocal.get_localzone());
				timezone					= tzlocal.get_localzone();
				(tod_feats,tod_feat_names)	= get_time_of_day_features_for_from_timestamp_single_example(timestamp,timezone);
				print("The model also relies on time-of-day indicators as additional features (added, assuming timezone: %s)." % str(timezone));
				pass;
			else:
				hour						= time_feats[0];
				minute						= time_feats[1];
				(tod_feats,tod_feat_names)	= get_time_of_day_features_for_from_hour_single_example(hour);
				print("The model also relies on time-of-day indicators as additional features (added, based on provided time (24hr) %.0f:%.0f)." % (hour,minute));
				pass;
			print(zip(tod_feat_names,tod_feats));
			sensors_feat.append(tod_feats);
			pass;
		else:
			print("The model does not use time-of-day indicators as features.");
			pass;
		
		print("Standardizing the feature vector...");
		feat_vec		= np.concatenate(tuple(sensors_feat));
		feat_vec		= ess_utils.standardize_feature_vector(feat_vec,cla['mean_vec'],cla['std_vec']);
		feat_mat		= feat_vec.reshape((1,-1));
		
		print("Initializing the classifier - a neural network of type MIC...");
		mic_params		= cla['mic_network_params'];
		mic				= neural_networks.MIC(None,mic_params);

		print("The MIC (multiple inputs classifier) requires preparing a mask indicating how much to weight each input-branch (sensor)...");
		existing_branch_mat	= neural_networks.mic_infer_existing_branch_mat(mic,numpy.isnan(feat_mat));
		Bmask			= neural_networks.construct_Bmask(existing_branch_mat);
		
		print("Performing the prediction...");
		prob_mat		= mic.output(feat_mat,Bmask).eval();
		label_probs		= prob_mat.reshape(n_labels).tolist();

		for (li,label) in enumerate(label_names):
			print("%s: %.3f" % (label.rjust(30),label_probs[li]));
			pass;
		
		return (label_names,label_probs);
	
	pass; # end class ES_MLP