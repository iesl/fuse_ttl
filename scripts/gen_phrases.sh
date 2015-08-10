#$ -S /bin/sh -cwd -o ../logs/gen_phrases.out -e  ../logs/gen_phrases.err
##!/usr/bin/env bash


source config

if [ -z ${TECHTERMS_DIR} ]; then
     echo "TECHTERMS_DIR" variable not set
     echo "set variable in config file"
     exit 1
fi


##### input file path #####

data_file=${DATA_FILE}

###########################


#### word2phrase app options #####

memory=3g

##############################


##### checks #####

base_filename=$(basename ${data_file})
output_dir="${TECHTERMS_DIR}/output/model"
techterms_jar="${TECHTERMS_DIR}/src/technicalterms/target/technicalterms-1.0.jar"
factorie_jar="${TECHTERMS_DIR}/src/factorie-factorie_2.11-1.1/target/factorie_2.10-1.1-SNAPSHOT-nlp-jar-with-dependencies.jar"

if [ ! -e $data_file ]; then
    echo "$data_file does not exist. Provide correct file path"
    exit 1
fi

if [ ! -e ${techterms_jar} ]; then
    echo "${techterms_jar} file not found. "
    exit 1
fi

if [ ! -e ${factorie_jar} ]; then
   echo "${factorie_jar} file not found. "
   exit 1 
fi

#################


word2phrase_app="java -Xmx${memory} -classpath ${techterms_jar}:${factorie_jar} edu.umass.cs.WordPhrase --encoding=UTF8"


${word2phrase_app} --train-file=${data_file}  --phrase-file=${output_dir}/${base_filename}.2gram.phrase --output-file=${output_dir}/${base_filename}.2gram.data
${word2phrase_app} --train-file=${output_dir}/${base_filename}.2gram.data --phrase-file=${output_dir}/${base_filename}.3gram.phrase --output-file=${output_dir}/${base_filename}.3gram.data
${word2phrase_app} --train-file=${output_dir}/${base_filename}.3gram.data --phrase-file=${output_dir}/${base_filename}.4gram.phrase --output-file=${output_dir}/${base_filename}.4gram.data
${word2phrase_app} --train-file=${output_dir}/${base_filename}.4gram.data --phrase-file=${output_dir}/${base_filename}.5gram.phrase --output-file=${output_dir}/${base_filename}.5gram.data
${word2phrase_app} --train-file=${output_dir}/${base_filename}.5gram.data --phrase-file=${output_dir}/${base_filename}.6gram.phrase --output-file=${output_dir}/${base_filename}.6gram.data
