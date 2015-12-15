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



import codecs
import os.path
import sys

##### const
EMB_DIR                = 'output/model'
##### files 
techterms_dir          = sys.argv[1]
data_filename          = sys.argv[2]
disp                   = sys.argv[3]
disp_mapping_filename  = sys.argv[4]
fdata_file             = ''
fvocab_file            = ''
fdisp_data_file        = ''
fdisp_vocab_file       = ''
##### data structures 
mappings               = {}
vocab                  = {}
disp_vocab             = {}



def init_disp_files(): 
    global fdata_file, fvocab_file, fdisp_data_file, fdisp_vocab_file	
    base_filename     = os.path.basename(data_filename) 
    
    data_file         = os.path.join(techterms_dir, EMB_DIR, base_filename + '.6gram.data')
    vocab_file        = os.path.join(techterms_dir, EMB_DIR, base_filename + '.6gram.vocab')
    disp_data_file    = os.path.join(techterms_dir, EMB_DIR, base_filename + '.' + disp + '.6gram.data')
    disp_vocab_file   = os.path.join(techterms_dir, EMB_DIR, base_filename + '.' + disp + '.6gram.vocab')

    fdata_file        = codecs.open(data_file,       'r', encoding='utf-8')
    fvocab_file       = codecs.open(vocab_file,      'r', encoding='utf-8')
    fdisp_data_file   = codecs.open(disp_data_file,  'w', encoding='utf-8')
    fdisp_vocab_file  = codecs.open(disp_vocab_file, 'w', encoding='utf-8')


def load_disp_mappings():
	global mappings
	for line in open(disp_mapping_filename, 'r'):
	      try :
	        article_id, year = line.strip().split()
		article_id       = article_id.strip()
		year             = year.strip()
                id               = '{0}_{1}'.format(article_id, year)
	        mappings[id]     = 1
              except :
	        raise Exception('unable to parse the line - ' + line.strip() + '\ndisp mapping file doesnot follow the format of <article-id><space><year>') 	
	        sys.exit(1)
	         
        print 'loaded mappings'

def load_data_vocab():
	global vocab
	cnt = 0
	for line in fvocab_file:
           if len(line.strip().split()) == 2:
	     wrd, c = line.strip().split()
	     vocab[wrd.strip()] = int(c.strip())
	   else :
	     cnt += 1 
	print 'loaded data vocab'


def apply():
	global disp_vocab
        data_cnt = 0
        disp_cnt = 0
	for line in fdata_file:
	       splits = map(lambda x : x.strip(), line.strip().split())
	       id = '_'.join( splits[:2] )
	       yr = splits[1]
	       if id in mappings:
		    fdisp_data_file.write(line)
		    for wrd in splits:
			    if wrd in vocab:
			         disp_vocab[wrd] = 1
	            disp_cnt += 1 
	       data_cnt += 1
        print '# of docs in disp', disp_cnt
	fdisp_vocab_wrds = []	 
	for wrd in disp_vocab:
	     fdisp_vocab_wrds.append( (vocab[wrd], wrd) )
	fdisp_vocab_wrds.sort(reverse=True)
        for cnt, wrd in fdisp_vocab_wrds:
              fdisp_vocab_file.write(wrd + '\n')
        fdisp_vocab_file.close()	
	fdisp_data_file.close()
	print 'done generating disp level data'
	   

if __name__ == '__main__':

       init_disp_files()
       load_disp_mappings()
       load_data_vocab()
       apply()
