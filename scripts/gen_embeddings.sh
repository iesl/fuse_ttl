#$ -S /bin/sh -cwd -o ../logs/gen_embeddings.out -e  ../logs/gen_embeddings.err
##!/usr/bin/env bash



source config

if [ -z ${TECHTERMS_DIR} ]; then
     echo "TECHTERMS_DIR" variable not set
     echo "set variable in config file"
     exit 1
fi

##### input file path #####

data_file=${DATA_FILE} ## came from config file

###########################


##### embedding model args #####

JVM_memory=3g
threads=15
min_count=10
ignore_stopwords=true
max_vocab_size=2000000
sample=0.00001

################################


##### checks #####

base_filename=$(basename ${data_file})
output_dir="${TECHTERMS_DIR}/output/model"
emb_input_file="${output_dir}/${base_filename}.6gram.data"
techterms_jar="${TECHTERMS_DIR}/src/technicalterms/target/technicalterms-1.0.jar"
factorie_jar="${TECHTERMS_DIR}/src/factorie-factorie_2.11-1.1/target/factorie_2.10-1.1-SNAPSHOT-nlp-jar-with-dependencies.jar"

if [ ! -e $data_file ]; then
    echo "$data_file does not exist. Provide correct file path"
    exit 1
fi

if [ ! -e ${emb_input_file} ]; then
    echo "${emb_input_file} not found. Run ./gen_phrases.sh code"    
fi

if [ ! -e ${techterms_jar} ]; then
    echo "${techterms_jar} file not found. "
    exit 1
fi

if [ ! -e ${factorie_jar} ]; then
   echo "${factorie_jar} file not found. "
   exit  
fi

#################



word2vec_app="java -Xmx${JVM_memory} -classpath ${techterms_jar}:${factorie_jar} cc.factorie.app.nlp.embeddings.WordVec --encoding=UTF8"

${word2vec_app} --train=${emb_input_file} --output=${output_dir}/${base_filename}.6gram.embeddings --save-vocab=${output_dir}/${base_filename}.6gram.vocab --threads=${threads} --min-count=${min_count} --ignore-stopwords=${ignore_stopwords} --max-vocab-size=${max_vocab_size} --sample=${sample}
