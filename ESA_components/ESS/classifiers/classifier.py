'''
This module defines an abstract class Classifier to clearly define the required functionality of any derived custom classifiers.
'''

'''
An abstract base class to represent a classifier.
'''
class Classifier(object):

	def __init__(self):
		return; # end __init__

	'''
	This function is the main functionality of a classifier.
	
	Input:
	-----
	measurements_dir: string. The directory in which you can expect to find the unpacked sensor-measurements of an example.
	UTime: int. The timestamp of the example.
	extra_data: dict. Any extra data that a specific classifier type may require.
	
	Output:
	------
	label_names: list (length L). List of L strings, the names of the labels that the classifier produces predictions for.
	label_probs: list (length L). List of L doubles, the probability predictions assigned to each label.
	'''
	def classify(self,measurements_dir,UTime,extra_data=None):
		raise NotImplementedError("This is an abstract base class for all classifiers. Function classify() should be called on a derived class that actually implemented it.");
	
	pass; # end class Classifier
