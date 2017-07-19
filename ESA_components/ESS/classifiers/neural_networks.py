'''
Neural Networks

--------------------------------------------------------------------------
Written by Yonatan Vaizman. March 2016.
'''
import numpy;
import theano;
import theano.tensor as T;

####################
## SingleLayerMachine (affine transform followed by elementwise nonlinearity)
class SingleLayerMachine(object):
	def __init__(self,rng,d_in,d_out,W_init=None,Wname='W',b_init=None,bname='b',activation=T.nnet.sigmoid,act_param=None):

		# Set the mixing matrix:
		if W_init is None:
			# Randomly initialize the weights:
			high	= numpy.sqrt(6. / (d_in+d_out));
			low		= -high;
			W_init	= numpy.asarray(rng.uniform(low=low,high=high,size=(d_in,d_out)));
			if activation == T.nnet.sigmoid:
				W_init	*= 4.;
				pass;
			pass; # end if W_init is None...
		else:
			assert (d_in,d_out) == W_init.shape;
			pass; # end else (if W_init not None)
			
		self.W	= theano.shared(\
			value=W_init.astype(theano.config.floatX),\
			name=Wname,borrow=True);

		# Set the bias vector:
		if b_init is None:
			b_init	= numpy.zeros(d_out);
			pass; # end if b_init None
		else:
			assert (d_out,) == b_init.shape;
			pass; # end else (if b_init not None)
			
		self.b	= theano.shared(\
			value=b_init.astype(theano.config.floatX),\
			name=bname,borrow=True);
		
		# Set the nonlinear activation function:
		self.activation	= activation;
		self.act_param	= act_param;
		
		self.rng 		= rng;
		self.params		= [self.W, self.b];
			
		pass; # end __init__
	
	# Norms of the weights:
	def L1(self):
		return abs(self.W).sum();
	
	def L2_sqr(self):
		return (self.W ** 2).sum();
	
	def n_weights(self):
		return self.W.size;
	
	def output(self,X):
		lin_mix		= T.dot(X,self.W) + self.b;
		if self.activation is None:
			return lin_mix;
		elif self.act_param is not None:
			return self.activation(lin_mix,self.act_param);
		else:
			return self.activation(lin_mix);
	
	def output_with_dropout(self,X,p_keep):
		srng 		= theano.tensor.shared_randomstreams.RandomStreams(self.rng.randint(9999));
		mask		= srng.binomial(n=1, p=p_keep, size=X.shape);
		Xdropout	= T.switch(mask,X,0);
		Wdropout	= self.W / p_keep;
		
		lin_mix		= T.dot(Xdropout,Wdropout) + self.b;
		if self.activation is None:
			return lin_mix;
		elif self.act_param is not None:
			return self.activation(lin_mix,self.act_param);
		else:
			return self.activation(lin_mix);
		
	pass; # end class SingleLayerMachine

## SingleLayerMachine
####################
	
def activation_codename_map(activation_full_codename):
	parts				= activation_full_codename.split(':');
	activation_codename	= parts[0];
	act_param			= None if len(parts)==1 else float(parts[1]);
	if activation_codename == 'sigmoid':
		return (T.nnet.sigmoid,act_param);
	if activation_codename == 'tanh':
		return (T.tanh,act_param);
	if activation_codename == 'relu':
		return (T.nnet.relu,act_param);
	if activation_codename == 'identity':
		return (None,None);
	if activation_codename == 'softmax':
		return (T.nnet.softmax,act_param);
	
	raise ValueError("!!! Unsupported activation codename: %s (%s,%f)" % (activation_full_codename,activation_codename,act_param));
		
####################
## MLP (Multiple Layer Perceptron)

class MLP(object):

	def mlp_structure_str(self):
		dim_strs			= ['%d' % dim for dim in self.layer_dims];
		mlp_str				= '->'.join(dim_strs);
		return mlp_str;

	@classmethod
	def init_from_params(cls,rng,mlp_params,prefix=''):
		return cls(rng,mlp_params['layer_dims'],mlp_params['Ws'],mlp_params['bs'],mlp_params['activation_codenames'],prefix);

	def __init__(self,rng,layer_dims,Ws_init,bs_init,activation_codenames,prefix=''):
		n_node_layers		= len(layer_dims);
		n_layer_machines	= n_node_layers - 1;
		activations			= [];
		act_params			= [];
		for li in range(n_layer_machines):
			(act_func,act_param)	= activation_codename_map(activation_codenames[li]);
			activations.append(act_func);
			act_params.append(act_param);
			pass;
			
		assert len(Ws_init) == len(bs_init) == len(activations) == n_layer_machines;
		
		self.layer_dims				= layer_dims;
		self.activation_codenames	= activation_codenames;
		self.layerMachines	= [];
		for li in range(n_layer_machines):
			d_in			= layer_dims[li];
			d_out			= layer_dims[li+1];
			Wname			= '%sW%d' % (prefix,li);
			bname			= '%sb%d' % (prefix,li);
			self.layerMachines.append(\
				SingleLayerMachine(rng,d_in,d_out,Ws_init[li],Wname,bs_init[li],bname,activations[li],act_params[li]));
			pass; # end for li...
			
		self.params			= [];
		for layerMachine in self.layerMachines:
			self.params		+= layerMachine.params;
			pass;
			
		pass; # end __init__
	
	def get_evaluated_params(self):
		Ws_copy				= [];
		bs_copy				= [];
		for layerMachine in self.layerMachines:
			layer_W			= layerMachine.W.eval();
			Ws_copy.append(numpy.copy(layer_W));
			bs_copy.append(numpy.copy(layerMachine.b.eval()));
			pass;
		mlp_params			= {\
			'Ws':Ws_copy,\
			'bs':bs_copy
			};
		mlp_params['activation_codenames']	= self.activation_codenames;
		mlp_params['layer_dims'] 			= self.layer_dims;
		
		return mlp_params;
	
	# Norms of weights:
	def L1(self):
		L1			= 0.;
		for layerMachine in self.layerMachines:
			L1		+= layerMachine.L1();
			pass;
			
		return L1;
		
	def L2_sqr(self):
		L2_sqr		= 0.;
		for layerMachine in self.layerMachines:
			L2_sqr	+= layerMachine.L2_sqr();
			pass;
			
		return L2_sqr;
		
	def n_weights(self):
		n_tot		= 0;
		for layerMachine in self.layerMachines:
			n_tot	+= layerMachine.n_weights();
			pass;
		
		return n_tot;
	
	def output(self,X):
		# Feed forward the activations through the layers:
		for layerMachine in self.layerMachines:
			X				= layerMachine.output(X);
			pass;
			
		return X; # end output
	
	def output_with_dropout(self,X,p_keeps):
		assert len(self.layerMachines) == len(p_keeps);
		# Feed forward the activations through the layers:
		for (li,layerMachine) in enumerate(self.layerMachines):
			X				= layerMachine.output_with_dropout(X,p_keeps[li]);
			pass;
			
		return X; # end output

	def layers_outputs(self,X):
		# Feed forward the activations through the layers:
		layers_outputs		= [];
		for layerMachine in self.layerMachines:
			X				= layerMachine.output(X);
			layers_outputs.append(X);
			pass;
			
		return layers_outputs; # end layers_outputs

	def layers_outputs_with_dropout(self,X,p_keeps):
		assert len(self.layerMachines) == len(p_keeps);
		# Feed forward the activations through the layers:
		layers_outputs		= [];
		for (li,layerMachine) in enumerate(self.layerMachines):
			X				= layerMachine.output_with_dropout(X,p_keeps[li]);
			layers_outputs.append(X);
			pass;
			
		return layers_outputs; # end layers_outputs
		
	def evaluate_layers_outputs(self,X):
		layers_outputs		= self.layers_outputs(X);
		layers_outputs_eval	= [output.eval() for output in layers_outputs];
		return layers_outputs_eval;
		# # Feed forward the activations through the layers:
		# layers_outputs		= [];
		# for layerMachine in self.layerMachines:
			# X				= layerMachine.output(X);
			# layers_outputs.append(X.eval());
			# pass;
			
		# return layers_outputs; # end evaluate_layers_outputs

	def negative_log_likelihood_cost(self,X,Ygt,Ymask):
		Yprob		= self.output(X);
		return self.negative_log_likelihood_cost_helper(Yprob,Ygt,Ymask);
		
	def negative_log_likelihood_cost_with_dropout(self,X,Ygt,Ymask,dropout_p_keeps):
		Yprob		= self.output_with_dropout(X,dropout_p_keeps);
		return self.negative_log_likelihood_cost_helper(Yprob,Ygt,Ymask);
		
	def negative_log_likelihood_cost_helper(self,Yprob,Ygt,Ymask):
		YgtLogP_pos	= plogq(Ygt,Yprob); #Ygt * T.log(Yprob);
		YgtLogP_neg	= plogq(1-Ygt,1.-Yprob); #(1-Ygt) * T.log(1.-Yprob);

		cost_mat	= -(YgtLogP_pos+YgtLogP_neg);
		cost_mat_w	= Ymask * cost_mat;
		
		cost		= T.mean(cost_mat_w);
		return cost;
	
	def square_difference_cost_helper(self,Yout,Ygt,Ymask):
		diff		= Yout - Ygt;
		sq_diffs	= diff**2;
		cost_mat	= Ymask * sq_diffs;
		cost		= T.mean(cost_mat);
		return cost;
		
	def abs_difference_power_cost_helper(self,Yout,Ygt,Ymask,power1,power2):
		diff		= Yout - Ygt;
		absdiff		= abs(diff);
		diff_power	= absdiff ** power1;
		
		power1_mat	= Ymask * diff_power;
		p1_per_ex	= T.mean(power1_mat,axis=1);
		p2_per_ex	= p1_per_ex ** power2;
		
		cost		= T.mean(p2_per_ex);
		return cost;
	
	def gradient_updates_momentum(self,cost,learning_rate,momentum):
		assert momentum < 1 and momentum >= 0;
		
		updates		= [];
		for param in self.params:
			# Initialize (just in the first call of this function):
			latest_gradient	= theano.shared(\
				param.get_value()*0.,\
				broadcastable=param.broadcastable);
				
			current_grad	= T.grad(cost=cost,wrt=param);
			momentum_grad	= momentum*latest_gradient + (1.-momentum)*current_grad;
			update_step		= -learning_rate*momentum_grad;
			#update_step		= -learning_rate*current_grad;
			updates.append((param,param + update_step));
			updates.append((latest_gradient,momentum_grad));
			pass; # end for param...
		
		return updates;
		
	pass; # end class MLP

def prepare_mlp_params(mlp_params,dummy_mlp_in_dim=-1):
	if mlp_params is None:
		mlp_params			= {'layer_dims':[dummy_mlp_in_dim],'activation_codenames':[]};
		pass;
	
	if 'Ws' not in mlp_params.keys():
		mlp_params['Ws']	= [None for li in range(len(mlp_params['layer_dims'])-1)];
		pass;
		
	if 'bs' not in mlp_params.keys():
		mlp_params['bs']	= [None for li in range(len(mlp_params['layer_dims'])-1)];
		pass;

	return mlp_params;

## MLP (Multiple Layer Perceptron)
####################

	
####################
### MIC (Multiple Input branches) :

'''
MIC: Multiple Input Classifier
The input is fed in slices into multiple input branches (MLPs).
The outputs of these branches are concatenated and fed into the out_MLP.
This architecture also enables modeling missing input channels (modalities).
'''
class MIC(object):

	def mic_structure_str(self):
		branch_strs			= ['%s:%s' % (self.branch_names[bi],self.in_mlps[bi].mlp_structure_str()) for bi in range(self.n_branches)];
		branches_str		= '|'.join(branch_strs);
		out_mlp_str			= self.out_mlp.mlp_structure_str();
		mic_str				= '%d{%s}%s' % (self.in_dim,branches_str,out_mlp_str);
		return mic_str;

	def __init__(self,rng,mic_params):
		self.rng			= rng;
		# Input channels:
		self.in_dim			= mic_params['in_dim'];
		self.n_branches		= len(mic_params['in_mlps']);
		self.branch_names	= list(mic_params['branch_names']);
		assert len(self.branch_names) == self.n_branches;
		
		self.in_mlps		= [None for bi in range(self.n_branches)];
		self.in_starts		= [None for bi in range(self.n_branches)];
		self.in_stops		= [None for bi in range(self.n_branches)];
		
		in_dim_so_far		= 0;
		z_dim_so_far		= 0;
		self.in_mlps_params	= [];
		for bi in range(self.n_branches):
			start				= in_dim_so_far;
			in_mlp_init_params	= prepare_mlp_params(mic_params['in_mlps'][bi],dummy_mlp_in_dim=self.in_dim);
			in_mlp				= MLP.init_from_params(rng,in_mlp_init_params);
			self.in_mlps_params.extend(in_mlp.params);
			
			in_dim_so_far		= start + in_mlp.layer_dims[0];
			z_dim_so_far		+= in_mlp.layer_dims[-1];
			
			self.in_starts[bi]	= start;
			self.in_mlps[bi]	= in_mlp;
			self.in_stops[bi]	= in_dim_so_far;
			pass;
		self.z_dim			= z_dim_so_far;
		
		# Output channel:
		out_mlp_init_params	= prepare_mlp_params(mic_params['out_mlp']);
		self.out_mlp		= MLP.init_from_params(rng,out_mlp_init_params);
		self.out_mlp_params	= self.out_mlp.params;

		self.params			= [];
		self.params.extend(self.in_mlps_params);
		self.params.extend(self.out_mlp_params);
		assert self.out_mlp.layer_dims[0] == self.z_dim;
		
		print("<MIC> initializing network. %s" % self.mic_structure_str());
		return; # end __init__
	
	def get_evaluated_params(self):
		mic_params						= {'in_mlps':[None for bi in range(self.n_branches)]};
		for bi in range(self.n_branches):
			mic_params['in_mlps'][bi]	= self.in_mlps[bi].get_evaluated_params();
			pass;
			
		mic_params['out_mlp']			= self.out_mlp.get_evaluated_params();
		mic_params['branch_names']		= list(self.branch_names);
		mic_params['in_dim']			= self.in_dim;
		
		return mic_params;
		
	def L1(self):
		total_l1			= 0.;
		for bi in range(self.n_branches):
			total_l1		+= self.in_mlps[bi].L1();
			pass;
			
		total_l1			+= self.out_mlp.L1();
		return total_l1;
	
	def L2_sqr(self):
		total_l2s			= 0.;
		for bi in range(self.n_branches):
			total_l2s		+= self.in_mlps[bi].L2_sqr();
			pass;
			
		total_l2s			+= self.out_mlp.L2_sqr();
		return total_l2s;

	'''
	Bmask: (n_examples x n_branches). For each example and each branch: is it existing (1) or missing (0).
	'''
	def output(self,X,Bmask):
		# Propagate through the input branches:
		Zs					= [None for bi in range(self.n_branches)];
		for bi in range(self.n_branches):
			Xbranch			= X[:,self.in_starts[bi]:self.in_stops[bi]];
			Zbranch			= self.in_mlps[bi].output(Xbranch);
			# Nullify this branch's output for the examples that are missing this branch:
			branch_weights	= Bmask[:,bi].reshape((-1,1));
			Zbranch			= Zbranch * branch_weights;
			Zs[bi]			= Zbranch;
			pass;
		
		Z					= T.concatenate(Zs,axis=1);
		return self.out_mlp.output(Z);

	pass; # end class MIC

def mic_infer_existing_branch_mat(mic,missing_feat_mat):
	existing_feat_mat			= numpy.logical_not(missing_feat_mat);
	existing_branch_mat			= numpy.zeros((existing_feat_mat.shape[0],mic.n_branches),dtype=bool);
	for bi in range(mic.n_branches):
		starti						= mic.in_starts[bi];
		stopi						= mic.in_stops[bi];
		existing_branch_mat[:,bi]	= numpy.any(existing_feat_mat[:,starti:stopi],axis=1);
		pass;
	
	return existing_branch_mat;

def construct_Bmask(existing_branch_mat,normalize_branch_contributions=True):
	if normalize_branch_contributions:
		# Make sure the total contribution of the branches sums up to number of branches:
		branch_prop				= numpy.mean(existing_branch_mat,axis=1).reshape((-1,1)).astype(float);
		Bmask					= numpy.where(branch_prop<=0.,0.,existing_branch_mat / branch_prop);
		pass;
	else:
		Bmask					= numpy.where(existing_branch_mat,1.,0.);
		pass;
	
	return Bmask;

## MIC
####################
