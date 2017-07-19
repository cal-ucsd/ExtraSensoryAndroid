#!/usr/bin/env python
'''
Calculate features from the raw data

Written by Yonatan Vaizman. Dec 2015.
'''

import os;
import os.path;
import pickle;
import numpy;
import numpy.fft;
import scipy.signal;
import scipy.spatial.distance;
import shutil;
import datetime;
import pdb;
import warnings;


# Globals:
g__standard_sr	= 40.;
g__standard_cutoff_frequencies	= [0.5,1.,3.,5.];
g__standard_time_lags = [0.5,1.,5.,10.];


def get_dimension_of_features(sensor_type):
	sensor_feature_dims	= {\
	'acc':26,\
	'gyro':26,\
	'magnet':31,\
	'location':11,\
	'watch_acc':46,\
	'watch_compass':9,\
	'proc_attitude':12,\
	'rotation':6,\
	'derived_acc':47,\
	'audio_naive':26
	};
	if sensor_type not in sensor_feature_dims:
		return 0;
	
	return sensor_feature_dims[sensor_type];

'''
Get features of gyroscope 3-axis time series.

Input:
-----
gyro: (T x 3) array. The time series of gyroscope measurements.
sr: scalar. Sampling rate (Hz)
prefix: string. Prefix for the feature names.

Output:
------
feats: 1d-array of features.
feat_names: 1d-array of strings. The feature names.
'''
def get_gyroscope_features(gyro,sr,prefix):
	(feats,feat_names)	= get_3axes_standard_features(gyro,sr);
	(feats,feat_names)	= augment_more_features(feats,feat_names,prefix);
	return (feats,feat_names);

	
'''
Get features of accelerometer 3-axis time series.

Input:
-----
acc: (T x 3) array. The time series of acceleration.
sr: scalar. Sampling rate (Hz)
prefix: string. Prefix for the feature names.

Output:
------
feats: 1d-array of features.
feat_names: 1d-array of strings. The feature names.
'''
def get_accelerometer_features(acc,sr,prefix):
	(feats,feat_names)	= get_3axes_standard_features(acc,sr);
	(feats,feat_names)	= augment_more_features(feats,feat_names,prefix);
	return (feats,feat_names);


'''
Get standard features for 3-axis time series.
Including statistics of magnitude, spectrum features of magnitude
and inter-axis correlations.

Input:
-----
X: (T x 3) array. The 3 axis time series.

Output:
------
feats: 1d-array of features.
feat_names: 1d-array of strings. The feature names.
'''
def get_3axes_standard_features(X,sr):
	if X.size > 1:
		magnitude		= numpy.sum(X**2,axis=1)**0.5;
		pass;
	else:
		magnitude		= numpy.nan*numpy.ones(1);
		pass;
	(stats,stat_names)	= get_1d_statistical_features(magnitude);
	(specs,spec_names)	= get_1d_spectral_features(magnitude,sr,g__standard_cutoff_frequencies);
	(ac,ac_names)		= get_autocorrelation_features(magnitude,sr);
	(f3d,f3d_names)		= get_3d_stats_features(X);
	
	(feats,feat_names)	= augment_more_features(stats,stat_names,'magnitude_stats');
	(feats,feat_names)	= augment_more_features(specs,spec_names,'magnitude_spectrum',feats,feat_names);
	(feats,feat_names)	= augment_more_features(ac,ac_names,'magnitude_autocorrelation',feats,feat_names);
	(feats,feat_names)	= augment_more_features(f3d,f3d_names,'3d',feats,feat_names);

	return (feats,feat_names);
	
	
'''
Augment more features to an array of features.

Input:
-----
more_features: m-array of features to augment.
more_feature_names: m-array of strings. The names of the added features.
augment_prefix: string. A prefix to add to the names of the augmented features.
start_with_features: s-array of pre-existing features to start with. If missing, defaults to empty list.
start_with_feature_names: s-array of strings. The names of the pre-existing features. If missing, defaults to empty list.

Output:
------
feats: [m+s]-array of features.
feat_names: [m+s]-array of strings. The feature names.
'''
def augment_more_features(more_features,more_feature_names,augment_prefix,start_with_features=[],start_with_feature_names=[]):
	if len(start_with_features)<=0:
		start_with_features			= numpy.zeros(0);
		start_with_feature_names	= numpy.array([]);
		pass;
	
	# Add the prefix to the augmented feature names:
	adjusted_more_feature_names		= add_prefix_to_feature_names(more_feature_names,augment_prefix);
	feats							= numpy.concatenate((start_with_features,more_features),axis=0);
	feat_names 						= numpy.concatenate((start_with_feature_names,adjusted_more_feature_names),axis=0);
	
	return (feats,feat_names);

def add_prefix_to_feature_names(feat_names,prefix):
	adjusted_feature_names			= numpy.array([None for ii in range(len(feat_names))]);
	for (fi,name) in enumerate(feat_names):
		new_name					= '%s:%s' % (prefix,name);
		adjusted_feature_names[fi]	= new_name;
		pass;
	
	return adjusted_feature_names;
	
'''
Input:
x: 1d array - the values over which to calculate the statistics.
	array with size 0 or 1 will result in NaN features and the original feature names.

Output:
stats: 7-array of the statistical features calculated.
feat_names: 7-array of strings. The names of each feature.
'''
def get_1d_statistical_features(x):
#	stats       	= numpy.nan*numpy.ones(7);
#	feat_names		= numpy.array([None for ii in range(7)]);
	
	if x.size > 1:
		# Moments:
		x_mean    	= numpy.mean(x);
		x_std		= numpy.std(x);
		mom3        = scipy.stats.moment(x,moment=3);
		x_mom3    	= numpy.sign(mom3)*(abs(mom3)**(1./3.));
		x_mom4    	= scipy.stats.moment(x,moment=4)**(1./4.);

		# Quantiles:
		perc25    	= scipy.stats.scoreatpercentile(x,25);
		perc50    	= numpy.median(x);
		perc75    	= scipy.stats.scoreatpercentile(x,75);

		# Entropy of the values of the array:
		if (sum(abs(x)) > 0.):
			bin_counts 	= numpy.histogram(x,bins=20)[0];
			val_ent		= entropy(bin_counts);
			# "Entropy" over time, to distinguish sudden burst events from more stationary events:
			time_ent	= entropy(numpy.abs(x));
			pass;
		else:
			val_ent		= 0.;
			time_ent	= 0.;
			pass;
			
		feats		= numpy.array([\
			x_mean,x_std,\
			x_mom3,x_mom4,\
			perc25,perc50,perc75,\
			val_ent,time_ent]);
		pass; # end if x.size > 1
	else:
		feats		= numpy.nan*numpy.ones(9);
		#feats		= numpy.nan*numpy.ones(7);
		pass;
	
	feat_names		= numpy.array([\
		'mean','std',\
		'moment3','moment4',\
		'percentile25','percentile50','percentile75',\
		'value_entropy','time_entropy']);
    
#	if x.size > 1:
#		stats[9]    = scipy.stats.skew(x) if (x_std > 0) else 0.;
#		stats[10]   = scipy.stats.kurtosis(x) if (x_std > 0) else 0.;
#		pass;
#	feat_names[9]	= 'skewness';
#	feat_names[10]	= 'kurtosis';
			
	return (feats,feat_names);

def entropy(counts):
    if numpy.any(numpy.isnan(counts)):
        return None;
    
    if numpy.any(counts < 0):
        return None;

    if numpy.sum(counts) <= 0:
        return 0.;

    counts      = counts.astype(float);

    pos_counts  = counts[numpy.where(counts > 0)[0]];
    probs       = pos_counts / numpy.sum(pos_counts);
    logprobs    = numpy.log(probs);
    plogp       = probs * logprobs;

    entropy     = -numpy.sum(plogp);
    return entropy;

'''
Input:
x: 1d array - the time series over which to calculate the features.
sr: scalar. The sampling rate of the time series, in Hz.
subband_cutoffs: list of b cutoff frequencies (Hz) defining [b+1] subbands.
	These cutoff frequencies are assomed to be ascending.

Output:
feats: [b+4]-array of the statistical features calculated.
feat_names: [b+4]-array of strings. The names of each feature.
'''
def get_1d_spectral_features(x,sr,subband_cutoffs):
	b 				= len(subband_cutoffs);
	if x.size > 1:
		(freqs,nPS)	= get_normalized_power_spectrum(x,sr);
		pass;

		# Subband energies:
	if x.size > 1:
		log_energies	= get_subband_log_energies(freqs,nPS,subband_cutoffs);
		pass;
	else:
		log_energies	= numpy.nan*numpy.ones(b+1);
		pass;
#	sqrt_ratios		= numpy.nan*numpy.ones(b);
	ener_names		= numpy.array([None for ii in range(b+1)]);
#	ratio_names		= numpy.array([None for ii in range(b)]);
#	epsilon			= 0.000001;
	for bi in range(b+1):
		ener_names[bi]			= 'log_energy_band%d' % bi;
#		if bi < b:
#			if x.size > 0:
#				sqrt_ratios[bi]	= (sqrt_energies[bi+1]+epsilon) / (sqrt_energies[bi]+epsilon);
#				pass;
#			ratio_names[bi]		= 'band_ratio_%d_%d' % (bi+1,bi);
#			pass;
		pass;

	if x.size > 1:
		# Spectral entropy:
		spectral_ent	= entropy(nPS);
		
		# Dominant periodicity:
#		nPS[0]			= -1.; # We're interested in non-zero frequency.
#		dom_ind			= numpy.argmax(nPS);
#		dom_freq		= freqs[dom_ind]; # in Hz.
#		dom_period		= 1./dom_freq; # in sec.
		
#		more_feats		= numpy.array([spectral_ent,dom_freq,dom_period]);
		more_feats		= numpy.array([spectral_ent]);
		pass;
	else:
#		more_feats		= numpy.nan*numpy.ones(3);
		more_feats		= numpy.nan*numpy.ones(1);
		pass;
#	more_feat_names	= numpy.array(['spectral_entropy','dominant frequency','dominant period']);
	more_feat_names	= numpy.array(['spectral_entropy']);
	
	# Assemble the features:
	feats			= numpy.concatenate(\
		(log_energies,more_feats),\
		axis=0);
	feat_names		= numpy.concatenate(\
		(ener_names,more_feat_names),\
		axis=0);
	
	return (feats,feat_names);

def get_subband_log_energies(freqs,nPS,subband_cutoffs):
	b				= len(subband_cutoffs);
	energies 		= numpy.zeros(b+1);
	# Energy of the first subband:
	low				= 0.;
	high			= subband_cutoffs[0];
	band			= numpy.logical_and(freqs>low,freqs<=high);
	energy			= numpy.sum(nPS[band]);
	energies[0]		= energy;
	for bi in range(b):
		low			= subband_cutoffs[bi];
		if bi < b-1:
			high	= subband_cutoffs[bi+1];
			pass;
		else:
			high	= numpy.inf;
			pass;
		band		= numpy.logical_and(freqs>=low,freqs<high);
		energy		= numpy.sum(nPS[band]);
		energies[bi+1]	= energy;
		pass;
	
	log_energies	= log_compression(energies);
	return log_energies;

def get_normalized_power_spectrum(x,sr):
	T 				= x.size;
	freqs 			= numpy.fft.fftfreq(T,1./sr);

	# Window the time series:
	ham   			= numpy.hamming(T);
	x     			= ham*x;

	# Calculate the DFT and the power spectrum:
	y     			= numpy.fft.fft(x);
	# Get rid of duplicate frequencies:
	inds			= numpy.where(freqs >=0.)[0];
	freqs			= freqs[inds];
	y				= y[inds];
	PS    			= abs(y)**2;
	power 			= numpy.sum(PS);
	# Normalize the power spectrum (to sum to total energy of 1):
	if power > 0.:
		nPS 		= PS / power;
		pass;
	else:
		nPS			= PS;
		pass;

	return (freqs,nPS);

def get_autocorrelation_features(x,sr):
	if x.size > 1:
		# Remove the DC component, to get positive and negative values:
		x			= x - numpy.nanmean(x);
		acf			= numpy.correlate(x,x,'same');
		# Get autocorrelation from 0-lag on, normalized by 0-lag acf:
		zerolagind	= numpy.argmax(acf);
		acf_from0	= acf[zerolagind:]/float(acf[zerolagind]);
		# Avoid detecting the first lobe:
		neginds		= numpy.where(acf_from0<0)[0];
		if len(neginds) <= 0:
			# Then decide there is no periodicity, and take the farthest lag's autocorrelation:
			period_lag	= len(acf_from0)-1;
			pass;
		else:
			zerocrossi	= neginds[0];
			acf_from0[:zerocrossi]	= 0;
			# Now select the max autocorr, from second lobe on:
			period_lag	= numpy.argmax(acf_from0);
			pass;
			
		period		= float(period_lag) / sr; # in seconds.
		period_ac	= acf_from0[period_lag];
		
		feats		= numpy.array([period,period_ac]);
		pass;
	else:
		feats		= numpy.nan*numpy.ones(2);
		pass;
		
	feat_names		= numpy.array(['period','normalized_ac']);
	
	return (feats,feat_names);

'''
Get the correlation coefficients between axes from a 3-dimensional time series.

Input:
-----
X: (T x 3) array. The time series.
	An array of size <= 1 will result in NaN features, and original feature names.

Output:
------
feats: 3-array. The 3 inter-axis correlation coefficients.
feat_names: 3-array of strings. The names of correlation features.
'''
def get_3d_inter_axis_correlation_coefficients(X):
	if X.size > 1:
		C		= numpy.corrcoef(X.T);
		feats	= numpy.array([C[0,1],C[0,2],C[1,2]]);
	
#		T 		= X.shape[0];
#		C 		= numpy.dot(X.T,X) / float(T);
#		C_sign	= numpy.sign(C);
#		C_abs	= numpy.abs(C);
#		# Power compress the correlation values:
#		C_comp	= C_sign * (C_abs**0.5);
		
#		feats	= numpy.array([C_comp[0,1],C_comp[0,2],C_comp[1,2]]);
		pass;
	else:
		feats	= numpy.nan*numpy.ones(3);
		pass;
	feat_names	= numpy.array(['ro_xy','ro_xz','ro_yz']);
	
	return (feats,feat_names);

	
'''
Get basic statistics for 3 axes time series.
'''
def get_3d_stats_features(X):
	if X.size > 1:
		axis_means			= numpy.mean(X,axis=0);
		axis_stds			= numpy.std(X,axis=0);
		(corrs,corr_names)	= get_3d_inter_axis_correlation_coefficients(X);
		feats				= numpy.concatenate((\
			axis_means,axis_stds,corrs),axis=0);
		pass;
	else:
		feats				= numpy.nan*numpy.ones(9);
		pass;
	feat_names				= numpy.array([\
		'mean_x','mean_y','mean_z',\
		'std_x','std_y','std_z',\
		'ro_xy','ro_xz','ro_yz']);

	return (feats,feat_names);
	
	
'''
Get features derived from the estimated components of acceleration (gravity and user).

Input:
-----
user_acc: (T x 3). Time series of the user-generated acceleration in each axis.
grav: (T x 3). Time series of the direction of gravitation relative to the phone's axes system.
sr: scalar. The sampling rate of the time series.

Output:
------
feats: 1d-array of features.
feat_names: 1d-array of strings. The feature names.
'''
def get_derived_acc_features(user_acc,grav,sr):
	
	if grav.size > 1 and user_acc.size > 1:
		# Calculate the projection of the user acceleration on the direction of gravity:
		acc_grav_proj 	= numpy.sum(grav*user_acc,axis=1); # just the amplitudes in direction of gravity
		# Separate user acceleration to vertical axis and horizontal plane:
		vertical_acc	= numpy.outer(acc_grav_proj,numpy.ones(3)) * grav; # full user-acceleration vectors (all in axis of gravity)
		horizontal_acc	= user_acc - vertical_acc;
		hor_acc_mag		= numpy.sum(horizontal_acc**2,axis=1)**0.5;
		pass;
	else:
		acc_grav_proj	= numpy.nan*numpy.ones(1);
		hor_acc_mag		= numpy.nan*numpy.ones(1);
		pass;
	
	# Standard acceleration features on the vertical axis, and on magnitude of horizontal plane:
	(ver_acc_stat_feats,ver_acc_stat_feat_names)		= get_1d_statistical_features(acc_grav_proj);
	(ver_acc_spec_feats,ver_acc_spec_feat_names)		= get_1d_spectral_features(acc_grav_proj,sr,g__standard_cutoff_frequencies);
	(ver_acc_ac_feats,ver_acc_ac_feat_names)			= get_autocorrelation_features(acc_grav_proj,sr);
	
	(hor_acc_stat_feats,hor_acc_stat_feat_names)		= get_1d_statistical_features(hor_acc_mag);
	(hor_acc_spec_feats,hor_acc_spec_feat_names)		= get_1d_spectral_features(hor_acc_mag,sr,g__standard_cutoff_frequencies);
	(hor_acc_ac_feats,hor_acc_ac_feat_names)			= get_autocorrelation_features(hor_acc_mag,sr);
	
	# Correlation between vertical and horizontal acceleration:
	ver_hor_corr_coeff									= numpy.corrcoef(acc_grav_proj,hor_acc_mag)[0,1];
	abs_ver_hor_corr_coeff								= numpy.corrcoef(abs(acc_grav_proj),hor_acc_mag)[0,1];
	acc_ver_hor_corr_feats								= numpy.array([ver_hor_corr_coeff,abs_ver_hor_corr_coeff]);
	acc_ver_hor_corr_feat_names							= numpy.array(['vertical_vs_horizontal_mag_corr_coef','vertical_mag_vs_horizontal_mag_corr_coef']);

	# Get absolute direction features:
	if grav.size > 1:
		grav_abs_dir_feats		= numpy.concatenate((numpy.mean(grav,axis=0),numpy.std(grav,axis=0)),axis=0);
		pass;
	else:
		grav_abs_dir_feats		= numpy.nan*numpy.ones(6);
		pass;
	grav_abs_dir_feat_names	= numpy.array([\
		'grav_mean_x','grav_mean_y','grav_mean_z',\
		'grav_std_x','grav_std_y','grav_std_z']);
	
	# Get relative direction features:
	(grav_rel_dir_feats,grav_rel_dir_feat_names)		= get_relative_direction_statistics(grav,sr);

	# Assemble the features:
	(feats,feat_names)		= augment_more_features(grav_abs_dir_feats,grav_abs_dir_feat_names,'gravity');
	(feats,feat_names)		= augment_more_features(grav_rel_dir_feats,grav_rel_dir_feat_names,'gravity_relative_directions',feats,feat_names);
	(feats,feat_names)		= augment_more_features(ver_acc_stat_feats,ver_acc_stat_feat_names,'vertical_acc_stats',feats,feat_names);
	(feats,feat_names)		= augment_more_features(ver_acc_spec_feats,ver_acc_spec_feat_names,'vertical_acc_spectrum',feats,feat_names);
	(feats,feat_names)		= augment_more_features(ver_acc_ac_feats,ver_acc_ac_feat_names,'vertical_acc_autocorrelation',feats,feat_names);
	(feats,feat_names)		= augment_more_features(hor_acc_stat_feats,hor_acc_stat_feat_names,'horizontal_acc_magnitude_stats',feats,feat_names);
	(feats,feat_names)		= augment_more_features(hor_acc_spec_feats,hor_acc_spec_feat_names,'horizontal_acc_magnitude_spectrum',feats,feat_names);
	(feats,feat_names)		= augment_more_features(hor_acc_ac_feats,hor_acc_ac_feat_names,'horizontal_acc_autocorrelation',feats,feat_names);
	(feats,feat_names)		= augment_more_features(acc_ver_hor_corr_feats,acc_ver_hor_corr_feat_names,'vertical_horizontal_acc',feats,feat_names);

	feat_names				= add_prefix_to_feature_names(feat_names,'derived_acc');
	return (feats,feat_names);
	
'''
Get features for attitude measurements.

Input:
-----
attitude: (T x 3). Time series of [roll,pitch,yaw] angles of the orientation of the phone, relative to [some] reference frame. Or nan if missing.

Output:
------
feats: 1d-array of features.
feat_names: 1d-array of strings. The feature names.
'''
def get_attitude_features(attitude):
	(attitude_feats,attitude_feat_names) 	= get_attitude_statistic_features(attitude);
	feat_names		= add_prefix_to_feature_names(attitude_feat_names,'attitude');
	return (attitude_feats,feat_names);
	
def get_attitude_statistic_features(attitude):
	if attitude.size == 1 and any(numpy.isnan(attitude)):
		attitude_feats	= numpy.nan*numpy.ones(12);
		pass;
	else:
		cos_angles			= numpy.cos(attitude);
		sin_angles			= numpy.sin(attitude);
		attitude_feats		= numpy.concatenate((\
			numpy.mean(cos_angles,axis=0),\
			numpy.mean(sin_angles,axis=0),\
			numpy.std(cos_angles,axis=0),\
			numpy.std(sin_angles,axis=0)),axis=0);
		pass;
	attitude_feat_names	= numpy.array([\
		'mean_cos_roll','mean_cos_pitch','mean_cos_yaw',\
		'mean_sin_roll','mean_sin_pitch','mean_sin_yaw',\
		'std_cos_roll','std_cos_pitch','std_cos_yaw',\
		'std_sin_roll','std_sin_pitch','std_sin_yaw'\
		]);
	return (attitude_feats,attitude_feat_names);

'''
Get features of statistics over relative directions.

Input:
-----
X: (T x d). Time series of d-axis directional vectors (perhaps with magnitude).
	array of size <= 1 will result in NaN features, and original feature names.
sr: scalar. Sampling rate of the time series.

Output:
------
feats: 1d-array of features.
feat_names: 1d-array of strings. The feature names.
'''
def get_relative_direction_statistics(X,sr):
	L					= len(g__standard_time_lags) + 1;
	if X.size > 1:
		(T,d)			= X.shape;
		# Calculate all cross cosine-similarities between all timepoints:
		norms			= numpy.sum(X**2,axis=1)**0.5;
		divs			= numpy.where(norms<=0.,1,norms);
		Xnormalized		= X / numpy.outer(divs,numpy.ones(d));
		cos_sim_mat		= numpy.dot(Xnormalized,Xnormalized.T);
		# Calculate statistics over the similarity values (not including self-similarity):
		avr_cos_per_lag	= numpy.zeros(T);
		for lag in range(T):
			cos_vals				= numpy.diagonal(cos_sim_mat,lag);
			avr_cos_per_lag[lag]	= numpy.nanmean(cos_vals);
			pass;

		cos_sims		= numpy.zeros(L);
		lag_lows		= [1];
		lag_lows.extend([int(numpy.round(sr*t)) for t in g__standard_time_lags]);
		for (li,lag) in enumerate(lag_lows):
			low				= lag_lows[li];
			high			= lag_lows[li+1] if li<(L-1) else T;
			avr_cos_vals	= avr_cos_per_lag[low:high];
			if len(avr_cos_vals)>0:
				cos_sims[li]= numpy.nanmean(avr_cos_vals);
				pass;
			pass;
			
		pass;
	else:
		cos_sims		= numpy.nan*numpy.ones(L);
		pass;
	
	feats			= cos_sims;
	feat_names		= numpy.array(["avr_cosine_similarity_lag_range%d" % lag for lag in range(L)]);
		
	return (feats,feat_names);

'''
Get features of magnetometer 3-axis time series.

Input:
-----
magnet: (T x 3) array. The time series of magnetometer measurements (possibly bias-removed).
	array with size <= 1 will result in NaN features and original feature names.
sr: scalar. Sampling rate.
prefix: string. Prefix for the feature names.

Output:
------
feats: 1d-array of features.
feat_names: 1d-array of strings. The feature names.
'''
def get_magnetometer_features(magnet,sr,prefix):
	# If the signal is just zeros, return NaN features:
	if (abs(magnet)).all() <= 0.:
		magnet	= numpy.nan*numpy.ones(1);
		pass;
	(basic_feats,basic_feat_names)		= get_3axes_standard_features(magnet,sr);
	(reldir_feats,reldir_feat_names)	= get_relative_direction_statistics(magnet,sr);
	
	(feats,feat_names)	= augment_more_features(basic_feats,basic_feat_names,prefix);
	(feats,feat_names)	= augment_more_features(reldir_feats,reldir_feat_names,prefix,feats,feat_names);
	return (feats,feat_names);
	

	
LOC_TIME    = 0;
LOC_LAT     = 1;
LOC_LONG    = 2;
LOC_ALT     = 3;
LOC_SPEED   = 4;
LOC_HOR     = 5;
LOC_VER     = 6;

def get_absolute_location_representative(locations):
	
	lat_long		= numpy.nan*numpy.ones(2);
	if locations.size <= 1:
		return lat_long;

	# Horizontal locations:
	is_valid_hor	= is_valid_accuracy(locations[:,LOC_HOR]);
	is_valid_hor	= numpy.logical_and(is_valid_hor,locations[:,LOC_LAT]>-500.);
	is_valid_hor	= numpy.logical_and(is_valid_hor,locations[:,LOC_LONG]>-500.);
	
	if sum(is_valid_hor) < 1:
		return lat_long;
		
	locations		= locations[is_valid_hor,:];
	best_hor_acc	= numpy.min(locations[:,LOC_HOR]);
	best_inds		= numpy.where(locations[:,LOC_HOR]<=best_hor_acc)[0];
	# Take the latest update among the 'best accuracy' updates:
	best_ind		= best_inds[-1];
	lat_long		= numpy.array([locations[best_ind,LOC_LAT],locations[best_ind,LOC_LONG]]);
	return lat_long;

	
'''
Get location features.

Input:
-----
X: (T x 7) matrix. Each row is a record indicating significant change from
    the previous location state and includes:
    timestamp, latitude, longitude, altitude, estimated speed,
    horizontal accuracy and vertical accuracy

Output:
------ 
feats: The features.
feat_names: array of strings. The names of the features.
'''
def get_location_features(locations,timestamp,validate_timestamps=True):

	loc_feat_names	= numpy.array([\
		'num_valid_updates',\
		'log_latitude_range','log_longitude_range',\
		'min_altitude','max_altitude',\
		'min_speed','max_speed',\
		'best_horizontal_accuracy','best_vertical_accuracy',\
		'diameter','log_diameter']);
	loc_feat_names		= add_prefix_to_feature_names(loc_feat_names,'location');
	default_feats		= numpy.nan*numpy.ones(11);
	
	if locations.size <= 1:
		return (default_feats,loc_feat_names);

	if validate_timestamps:
		is_valid_time	= get_is_valid_times(locations[:,LOC_TIME],timestamp);
		locations		= locations[is_valid_time,:];
		pass;
	
	if locations.size <= 1:
		return (default_feats,loc_feat_names);

	# Horizontal locations:
	is_valid_hor	= is_valid_accuracy(locations[:,LOC_HOR]);
	is_valid_hor	= numpy.logical_and(is_valid_hor,locations[:,LOC_LAT]>-500.);
	is_valid_hor	= numpy.logical_and(is_valid_hor,locations[:,LOC_LONG]>-500.);
	num_valid_hor	= numpy.sum(is_valid_hor);
	if num_valid_hor <= 1:
		lat_range2	= numpy.nan;
		long_range2	= numpy.nan;
		pass;
	else:
		min_lat		= numpy.min(locations[is_valid_hor,LOC_LAT]);
		max_lat		= numpy.max(locations[is_valid_hor,LOC_LAT]);
		min_long	= numpy.min(locations[is_valid_hor,LOC_LONG]);
		max_long	= numpy.max(locations[is_valid_hor,LOC_LONG]);
		
		lat_range	= max_lat - min_lat;
		long_range	= max_long - min_long;
		lat_range2	= log_compression(lat_range);
		long_range2	= log_compression(long_range);
		pass;
	
	# Vertical locations:
	is_valid_ver	= is_valid_accuracy(locations[:,LOC_VER]);
	num_valid_ver	= numpy.sum(is_valid_ver);
	if num_valid_ver <= 0:
		min_alt		= numpy.nan;
		max_alt		= numpy.nan;
		pass;
	else:
		min_alt		= numpy.min(locations[is_valid_ver,LOC_ALT]);
		max_alt		= numpy.max(locations[is_valid_ver,LOC_ALT]);
		pass;
		
	# Speed:
	is_valid_speed	= is_valid_positive_value(locations[:,LOC_SPEED]);
	num_valid_speed	= numpy.sum(is_valid_speed);
	if num_valid_speed <= 0:
		min_speed	= numpy.nan;
		max_speed	= numpy.nan;
		pass;
	else:
		min_speed	= numpy.min(locations[is_valid_speed,LOC_SPEED]);
		max_speed	= numpy.max(locations[is_valid_speed,LOC_SPEED]);
		pass;
		
	# Accuracies:
	if num_valid_hor > 0:
		best_hor_acc	= numpy.min(locations[is_valid_hor,LOC_HOR]);
		pass;
	else:
		best_hor_acc	= numpy.nan;
		pass;
	if num_valid_ver > 0:
		best_ver_acc	= numpy.min(locations[is_valid_ver,LOC_VER]);
		pass;
	else:
		best_ver_acc	= numpy.nan;
		pass;
	
	# Diameter:
	epsilon			= 0.001;
	max_distance	= find_largest_geographic_distance(\
		locations[is_valid_hor,LOC_LAT],\
		locations[is_valid_hor,LOC_LONG]);
	log_diameter	= numpy.log(numpy.max([max_distance,epsilon]));
	
	# Assemble features:
	loc_feats		= numpy.array([\
		num_valid_hor,\
		lat_range2,long_range2,\
		min_alt,max_alt,\
		min_speed,max_speed,\
		best_hor_acc,best_ver_acc,\
		max_distance,log_diameter]);
		
	return (loc_feats,loc_feat_names);
	
def get_is_valid_times(update_times,minute_timestamp):
	is_later_than_start	= update_times > minute_timestamp;
	is_reasonable_time	= update_times > 1420125356; # later than Jan 1st 2015
	return numpy.logical_and(is_later_than_start,is_reasonable_time);

def find_largest_geographic_distance(latitudes,longitudes):
    # Convert degrees to radians:
    deg2rad         = numpy.pi / 180.;
    r_latitudes     = deg2rad * latitudes;
    r_longitudes    = deg2rad * longitudes;

    max_dist        = 0.;
    for ii in range(len(latitudes)):
        for jj in range(ii+1,len(latitudes)):
            d       = distance_between_geographic_points(\
                r_latitudes[ii],r_longitudes[ii],\
                r_latitudes[jj],r_longitudes[jj]);
            if d > max_dist:
                max_dist    = d;
                pass;
            
            pass;
        pass;

    return max_dist;

EARTH_RADIUS        = 6371000.;

def distance_between_geographic_points(r_lat1,r_long1,r_lat2,r_long2):
    lat_diff        = r_lat1 - r_lat2;
    long_diff       = r_long1 - r_long2;

    a               = numpy.sin(lat_diff/2.)**2 + \
                      numpy.cos(r_lat1)*numpy.cos(r_lat2)*(numpy.sin(long_diff/2.)**2);
    arc_angle       = 2*numpy.arctan2((a**0.5)*((1-a)**0.5),1.);
    arc_length      = EARTH_RADIUS * arc_angle;

    return arc_length;

	
def is_valid_accuracy(accuracies):
	is_valid_pos	= is_valid_positive_value(accuracies);
	is_not_huge		= accuracies < 200.;
	is_valid		= numpy.logical_and(is_valid_pos,is_not_huge);
	return is_valid;
	
def is_valid_positive_value(values):
	is_not_nan		= numpy.logical_not(numpy.isnan(values));
	is_positive		= values > 0.;
	is_valid_pos	= numpy.logical_and(is_not_nan,is_positive);
	return is_valid_pos;

'''
Get features of 3-axis time series of watch accelerometer.

Input:
-----
watch_acc: (T x 3) array. The time series of watch accelerometer measurements.
sr: scalar. The sampling rate of the time series.

Output:
------
feats: 1d-array of features.
feat_names: 1d-array of strings. The feature names.
'''	
def get_watch_acc_features(watch_acc,sr):
	(standard_feats,standard_names)	= get_3axes_standard_features(watch_acc,sr);

	# Spectral features per axis:
	b						= len(g__standard_cutoff_frequencies);
	spec_feats				= numpy.array([]);
	spec_feat_names			= numpy.array([]);
	for (axi,axis) in enumerate(['x','y','z']):
		if watch_acc.size > 1:
			(freqs,nPS)		= get_normalized_power_spectrum(watch_acc[:,axi],sr);
			
			# Subband energies:
			log_energies	= get_subband_log_energies(freqs,nPS,g__standard_cutoff_frequencies);
			pass;
		else:
			log_energies	= numpy.nan*numpy.ones(b+1);
			pass;
		names				= numpy.array([None for ii in range(b+1)]);
		for bi in range(b+1):
			names[bi]		= '%s_log_energy_band%d' % (axis,bi);
			pass;
		
		spec_feats			= numpy.concatenate((spec_feats,log_energies),axis=0);
		spec_feat_names		= numpy.concatenate((spec_feat_names,names),axis=0);
		pass;
	
	# Relative directions:
	(reldir_feats,reldir_feat_names)	= get_relative_direction_statistics(watch_acc,sr);
	
	# Assemble features:
	(feats,feat_names)		= augment_more_features(standard_feats,standard_names,'watch_acceleration');
	(feats,feat_names)		= augment_more_features(spec_feats,spec_feat_names,'watch_acceleration:spectrum',feats,feat_names);
	(feats,feat_names)		= augment_more_features(reldir_feats,reldir_feat_names,'watch_acceleration:relative_directions',feats,feat_names);

	return (feats,feat_names);
	
def get_watch_compass_features(heading_degrees):
	if heading_degrees.size > 1:
		heading_radians		= (numpy.pi / 180.) * heading_degrees;
		cos_vals			= numpy.cos(heading_radians);
		sin_vals			= numpy.sin(heading_radians);
		
		# Cosine stats:
		mean_cos			= numpy.nanmean(cos_vals);
		std_cos				= numpy.nanstd(cos_vals);
		mom3_tmp			= scipy.stats.moment(cos_vals,moment=3);
		cos_mom3    		= numpy.sign(mom3_tmp)*(abs(mom3_tmp)**(1./3.));
		cos_mom4    		= scipy.stats.moment(cos_vals,moment=4)**(1./4.);
		
		# Sine stats:
		mean_sin			= numpy.nanmean(sin_vals);
		std_sin				= numpy.nanstd(sin_vals);
		mom3_tmp			= scipy.stats.moment(sin_vals,moment=3);
		sin_mom3    		= numpy.sign(mom3_tmp)*(abs(mom3_tmp)**(1./3.));
		sin_mom4    		= scipy.stats.moment(sin_vals,moment=4)**(1./4.);

		# Entropy:
		bins				= range(0,361,45);
		(counts,bins1)		= numpy.histogram(heading_degrees,bins);
		if sum(counts) <= 0:
			ent				= 0.;
			pass;
		else:
			ent				= entropy(counts);
			pass;
			
		heading_stats		= numpy.array([\
			mean_cos,std_cos,cos_mom3,cos_mom4,\
			mean_sin,std_sin,sin_mom3,sin_mom4,\
			ent]);
		pass;
	else:
		heading_stats		= numpy.nan*numpy.ones(9);
		pass;
	heading_names			= numpy.array([\
		'mean_cos','std_cos','mom3_cos','mom4_cos',\
		'mean_sin','std_sin','mom3_sin','mom4_sin',\
		'entropy_8bins']);
	
	(feats,feat_names)		= augment_more_features(heading_stats,heading_names,'watch_heading');
	return (feats,feat_names);
		
		
def log_compression(vals):
    epsilon = 0.001;
    logvals = numpy.log(epsilon+abs(vals)) - numpy.log(epsilon);
    compvals = numpy.sign(vals)*logvals;
    return compvals;
	
g__naive_feat_names		= numpy.array([]);
def get_audio_naive_feature_names():
	global g__naive_feat_names;
	if g__naive_feat_names.size > 1:
		return g__naive_feat_names;
	
	names			= [];
	names.extend(["audio_naive:mfcc%d:mean" % ii for ii in range(13)]);
	names.extend(["audio_naive:mfcc%d:std" % ii for ii in range(13)]);
	#names.extend(["audio_naive:normalized_mfcc%d:mean" % ii for ii in range(13)]);
	#names.extend(["audio_naive:normalized_mfcc%d:std" % ii for ii in range(13)]);
	
	g__naive_feat_names = numpy.array(names);
	return g__naive_feat_names;


def get_audio_naive_statistics_of_mfcc(instance_dir):
	audio_naive_dim	= get_dimension_of_features('audio_naive');
	default_feats	= numpy.nan*numpy.ones(audio_naive_dim);
	feat_names		= get_audio_naive_feature_names();
	augment_frames	= False;
	(mfccs_norm,mfccs)	= get_instance_audio_features(instance_dir,augment_frames);
	if mfccs.size <= 0:
		return (default_feats,feat_names);
	
	mean_vec		= numpy.nanmean(mfccs,axis=0);
	std_vec			= numpy.nanstd(mfccs,axis=0);
	#norm_mean_vec	= numpy.nanmean(mfccs_norm,axis=0);
	#norm_std_vec	= numpy.nanstd(mfccs_norm,axis=0);
	feats			= numpy.concatenate((\
		mean_vec,std_vec,\
		#norm_mean_vec,norm_std_vec\
		),axis=0);
	
	return (feats,feat_names);

'''
Read the audio (MFCC) features for a specific instance
and produce the appropriate raw feature vectors of it.
This includes taking windows of 3 consecutive 13d frames,
and normalizing each 39d feature vector to have unit norm.

In case the MFCC file doesn't exist, or has invalid data,
the returned features will have 0 rows.

Input:
instance_dir: string. The directory of the instance to get audio features for
augment_frames: boolean. Should we augment 3 consecutive time frames?

Output:
feats: (T x 39) array. T windows (each is concatenation of 3 consecutive time frames), normalized.
pre_norm: (T x 39) array. Unnormalized features.
'''
def get_instance_audio_features(instance_dir,augment_frames=True):
	raw_d                       = 13;
	out_d                       = raw_d*3;
	empty_features              = numpy.zeros((0,out_d));
	mfcc_file                   = os.path.join(instance_dir,'sound.mfcc');
	if not os.path.exists(mfcc_file):
		return (empty_features,empty_features);

	with warnings.catch_warnings():
		warnings.simplefilter("ignore");
		mfcc                        = numpy.genfromtxt(mfcc_file,delimiter=',');
		pass;
    
	if len(mfcc.shape) != 2:
		return (empty_features,empty_features);

	(T,d)                       = mfcc.shape;
	if d == 14:
		mfcc                    = mfcc[:,:-1];
		d                       = mfcc.shape[1];
		pass;
	if d != 13:
		return (empty_features,empty_features);

	if T < 3:
		return (empty_features,empty_features);

	if numpy.any(numpy.isnan(mfcc)) or numpy.any(numpy.isinf(mfcc)):
		return (empty_features,empty_features);
    
	if augment_frames:
		# Get windows of 3 frames:
		pre_norm                = numpy.concatenate((mfcc[:-2,:],mfcc[1:-1,:],mfcc[2:,:]),axis=1);
		pass;
	else:
		pre_norm				= mfcc;
		pass;

	# Normalize each feature vector to have unit L2-norm:
	feats                       = normalize_feature_vectors(pre_norm);

	return (feats,pre_norm);

def normalize_feature_vectors(pre_norm_feats):
    norms                       = (numpy.sum(pre_norm_feats**2,axis=1))**0.5;
    norms[norms<=0]             = 1.;
    feats                       = pre_norm_feats / (numpy.outer(norms,numpy.ones(pre_norm_feats.shape[1])));

    return feats;