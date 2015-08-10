from sklearn.cluster import MiniBatchKMeans
import numpy as np
import codecs
from sets import Set
import sys

disp_vocab      = {}
disp_vocab_file = sys.argv[1] 
embedding_file  = sys.argv[2]
kmeans_out_file = sys.argv[3]
K               = int(sys.argv[4])



def load_disp_vocab():
	global disp_vocab
	print disp_vocab_file
	for line in codecs.open(disp_vocab_file, 'r', encoding='utf-8'):
		 l = line.strip().split()
		 disp_vocab[l[0]] = 1
	print '# of words', len(disp_vocab)

def make_trXY():
	trX      = []
	trX_wrds = []
	i        = 0
	for line in codecs.open(embedding_file, 'r', encoding='utf-8'):
		l = line.strip().split()
	        if len(l) > 2:
		     wrd = l[0]
		     vec = [ float(v) for v in l[1:] ]
		     if wrd in disp_vocab:
		          trX.append(vec)
	                  trX_wrds.append(wrd)
		i += 1
		if i % 20000 == 0:
		   print i, len(trX)
        X = np.array(trX)
	return X, trX_wrds

def do_kmeans(X, X_wrds):
	k_means = MiniBatchKMeans(n_clusters=K, n_init = 2, batch_size=10000, init = 'random',  max_iter=10000, verbose=1)
	print 'starting kmeans'
	Y = k_means.fit_transform(X)
        print 'done K-means'

	fw = codecs.open(kmeans_out_file, 'w', encoding='utf-8')
	cl = {}
	i  = 0
	for i in range(len(X)):
	     cl_id = int(k_means.predict(X[i]))
	     if cl_id not in cl:
		     cl[cl_id] = []
             cl[cl_id].append(X_wrds[i])
	     if i % 10000 == 0:
	         print 'done-', i
	for cl_id in cl:
		line = ','.join(cl[cl_id])
		fw.write(line)
	        fw.write('\n')
	fw.close()


if __name__ == '__main__': 
         print "\n\n Starting KMeans \n\n"
         
	 load_disp_vocab()
         X, X_wrds = make_trXY()
	 do_kmeans(X, X_wrds)
