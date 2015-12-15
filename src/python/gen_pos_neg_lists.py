"""Copyright (C) 2015 University of Massachusetts Amherst.
   This file is part of "fuse_ttl"
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
import codecs
import os.path
import random
from sets import Set

### ptr to dir 
EMB_DIR               = 'output/model'
TERMS_DIR             = 'output/terms'

### args
techterms_dir         = sys.argv[1]
data_filename         = sys.argv[2]
disp                  = sys.argv[3]
neg_len_cut_off       = int(sys.argv[4])
pos_len_cut_off       = int(sys.argv[5])
max_len               = int(sys.argv[6])
score_low_threshold   = float(sys.argv[7])
score_high_threshold   = float(sys.argv[8])
### file names and ptr
emb_file              = ''
terms_file            = ''
pos_file              = ''
neg_file              = ''



def init_terms_file():
  global emb_file, terms_file, pos_file, neg_file, score_file

  base_filename  = os.path.basename(data_filename)
  emb_file       = os.path.join(techterms_dir, EMB_DIR, base_filename + '.6gram.embeddings')
  terms_file     = os.path.join(techterms_dir, EMB_DIR, base_filename + '.' +  disp + '.6gram.vocab')
  score_file     = os.path.join(techterms_dir, EMB_DIR, base_filename + '.' +  disp + '.6gram.vocab.score')
  pos_file       = os.path.join(techterms_dir, TERMS_DIR, disp, 'pos.list')
  neg_file       = os.path.join(techterms_dir, TERMS_DIR, disp, 'neg.list')


  if not os.path.exists(emb_file) or not os.path.exists(terms_file) or not os.path.exists(score_file):

     if not os.path.exists(emb_file):
         print emb_file, "does not exist"
         print "run gen_embeddings.sh"

     if not os.path.exists(terms_file):
         print terms_file, "does not exist"
         print "run gen_disp_data.sh"

     if not os.path.exists(score_file):
         print score_file, "does not exist"
         print "run gen_disp_data.sh"

     exit(1)


def make_neg_eg():
    cnt = 0
    neg_ifile = codecs.open(neg_file, 'w' , 'utf-8')
    for line in codecs.open(terms_file, 'r' , 'utf-8'):
           l = line.strip()
           if cnt < neg_len_cut_off:
              neg_ifile.write(l)
              neg_ifile.write('\n')
              cnt += 1
           elif l.replace('.','').isdigit() and cnt < neg_len_cut_off + 30:
              neg_ifile.write(l)
              neg_ifile.write('\n')
              cnt += 1
    neg_ifile.close()

def make_pos_eg():
    pos_list = []
    num_pos = 0
    countline = 0
    count = 0
    pos_ifile = codecs.open(pos_file, 'w' , 'utf-8')
    randomlines = Set()
    for line in codecs.open(score_file, 'r' , 'utf-8'):
           l = line.strip().split()
           score = float(l[1])
           if (not l[0].isdigit()) and countline < max_len and score >= score_low_threshold and score < score_high_threshold:
              pos_list.append(l[0])
              num_pos +=1
           countline+=1
    random.seed(0)
    for i in range(num_pos):
        rand = random.randint(0, num_pos-1)
        if count < pos_len_cut_off:
            randomlines.add(rand)
        count+=1
    for i in randomlines:
       pos_ifile.write(pos_list[i].strip() + '\n')
    pos_ifile.close()

if __name__ == '__main__' :
      init_terms_file()
      make_pos_eg()
      make_neg_eg()



