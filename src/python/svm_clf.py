"""Copyright (C) 2015 University of Massachusetts Amherst.
   This file is part of “fuse_ttl”
   https://github.com/iesl/fuse_ttl
   Licensed under the Apache License, Version 2.0 (the "License");
   you may not use this file except in compliance with the License.
   You may obtain a copy of the License at
    http://www.apache.org/licenses/LICENSE-2.0
   Unless required by applicable law or agreed to in writing, software
   distributed under the License is distributed on an "AS IS" BASIS,
   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
   See the License for the specific language governing permissions and
   limitations under the License."""





import sys
import numpy as np
from sklearn.svm import SVC
from sklearn.linear_model import LogisticRegression
from sklearn import cross_validation
from sklearn.ensemble import RandomForestClassifier
from sklearn.kernel_approximation import RBFSampler, Nystroem
import codecs
import sys
import os.path


### ptr to dir 
EMB_DIR               = 'output/model'
TERMS_DIR             = 'output/terms'
### args
techterms_dir         = sys.argv[1]
data_filename         = sys.argv[2]
disp                  = sys.argv[3]
disp_mapping_filename = sys.argv[4]
prob_cut_off          = float(sys.argv[5])
### file names and ptr
emb_file              = ''
terms_file            = ''
pos_ifile             = ''
neg_ifile             = ''
pos_ofile             = ''
neg_ofile             = ''
pos_prob_ofile        = ''
neg_prob_ofile        = ''


def init_terms_file():
  global emb_file, terms_file, pos_ifile, neg_ifile, pos_ofile, neg_ofile, pos_prob_ofile, neg_prob_ofile
  
  base_filename  = os.path.basename(data_filename)
  emb_file       = os.path.join(techterms_dir, EMB_DIR, base_filename + '.6gram.embeddings')
  terms_file     = os.path.join(techterms_dir, EMB_DIR, base_filename + '.' +  disp + '.6gram.vocab')
  pos_ifile      = os.path.join(techterms_dir, TERMS_DIR, disp, 'pos.list')
  neg_ifile      = os.path.join(techterms_dir, TERMS_DIR, disp, 'neg.list')
  pos_ofile      = os.path.join(techterms_dir, TERMS_DIR, disp, 'pred_pos.list')
  neg_ofile      = os.path.join(techterms_dir, TERMS_DIR, disp, 'pred_neg.list')
  pos_prob_ofile = os.path.join(techterms_dir, TERMS_DIR, disp, 'pred_prob_pos.list')
  neg_prob_ofile = os.path.join(techterms_dir, TERMS_DIR, disp, 'pred_prob_neg.list')


  if not os.path.exists(emb_file) or not os.path.exists(terms_file):
     
     if not os.path.exists(emb_file):
         print emb_file, "does not exist"
	 print "run gen_embeddings.sh"

     if not os.path.exists(terms_file):
	 print terms_file, "does not exist"
	 print "run gen_disp_data.sh"
     
     exit(1)
  
def make_XY(pos_eg, neg_eg):
     i = 0 
     print "# of terms provided in pos.list - ", len(pos_eg)
     print "# of terms provided in neg.list - ", len(neg_eg)
     pos_cnt = 0
     neg_cnt = 0
     Y  = []
     X  = []
     eg = [] 
     for line in codecs.open(emb_file, 'r', 'utf-8'):
	     l = line.strip().split()
	     if i > 0:
		wrd = l[0]
	        vec = [ float(v) for v in l[1:]]
		assert(len(wrd) > 0)
	        if wrd in pos_eg:
		    Y.append(1) 
		    X.append(vec)
		    eg.append((wrd, 1))
		    pos_cnt += 1
		if wrd in neg_eg:
                    Y.append(0)
		    X.append(vec)
		    neg_cnt += 1
		    eg.append((wrd, 0))
	        if pos_cnt == len(pos_eg) and neg_cnt == len(neg_eg):
	           break
	     i += 1
     print 
     print "# of positive terms found in disp vocab", pos_cnt
     print "# of negative terms found in disp vocab", neg_cnt
     print
     if pos_cnt < 10:
         raise Exception('number of positive examples found in vocab is less than 10')
	 sys.exit(1) 
     if neg_cnt < 10:
         raise Exception('number of negative examples found in vocab is less than 10')
	 sys.exit(1)
									           
     X = np.array(X)
     Y = np.array(Y)
     return np.array(Y), np.array(X), eg

def load_examples():
      pos_eg = {}
      neg_eg = {}
      for line in codecs.open(pos_ifile, 'r', 'utf-8'):
	     wrd = line.strip().split()
	     if len(wrd[0]) > 0:
	        pos_eg[wrd[0]] = 1
      for line in codecs.open(neg_ifile, 'r', 'utf-8'):
	      wrd = line.strip().split()
	      if len(wrd[0]) > 0:
	         neg_eg[wrd[0]] = 1
      return pos_eg, neg_eg

def predict(clf):
      i             = 0
      cnt           = 0
      pos_cnt       = 0
      neg_cnt       = 0
      fpos_prob     = codecs.open(pos_prob_ofile, 'w', 'utf-8')
      fneg_prob     = codecs.open(neg_prob_ofile, 'w', 'utf-8')
      fpos          = codecs.open(pos_ofile, 'w', 'utf-8')
      fneg          = codecs.open(neg_ofile, 'w', 'utf-8')
      pred_pos_wrds = []
      pred_neg_wrds = []
      wrds          = {}
      for wrd in codecs.open(terms_file, 'r', 'utf-8'):
	      wrds[wrd.strip().split()[0]] = 1
      ans = [] 
      print 
      print '# of examples classified, # of pos, # of neg'
      for line in codecs.open(emb_file, 'r', 'utf-8'):
            l = line.strip().split()
            if i > 0:
              wrd = l[0]
	      if wrd in wrds:
	         vec     = [ float(v) for v in l[1:]]
                 pred_y  = clf.predict(vec)
	         prob    = clf.predict_proba(vec)
                 cnt += 1
	         
	         if int(pred_y[0]) == 1 and prob[0][1] >= prob_cut_off:
		   pred_pos_wrds.append( (prob[0][1], wrd) )
		   pos_cnt += 1 
                 else :
		   pred_neg_wrds.append( (prob[0][1], wrd) )
		   neg_cnt += 1
            i +=1  

      print cnt, pos_cnt, neg_cnt
      pred_pos_wrds.sort(reverse=True)
      pred_neg_wrds.sort(reverse=True)
      for prob, wrd in pred_pos_wrds:
               fpos_prob.write(wrd + ' ' + str(prob) + '\n')
	       fpos.write(wrd + '\n')
      for prob, wrd in pred_neg_wrds:
               fneg_prob.write(wrd + ' ' + str(prob) + '\n')
	       fneg.write(wrd + '\n')
               
      fpos.close()
      fneg.close()
      fpos_prob.close()
      fneg_prob.close()
                 
if __name__ == '__main__' :
      init_terms_file()
      pos_eg, neg_eg = load_examples()
      Y_train, X_train, eg = make_XY(pos_eg, neg_eg)
      print X_train.shape, Y_train.shape
      clf = SVC(probability=True, kernel='linear')      
      scores = cross_validation.cross_val_score(clf, X_train, Y_train, cv=5)
      print 'cross validation scores - ', scores
      clf.fit(X_train, Y_train) 
      predict(clf)
