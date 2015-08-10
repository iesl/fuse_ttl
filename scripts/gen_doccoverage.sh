#!/bin/bash

#$ -S /bin/sh -cwd -o ../logs/gen_doccoverage.out -e  ../logs/gen_doccoverage.err
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

##### checks #####

base_filename=$(basename ${data_file})
model_dir=${TECHTERMS_DIR}/output/model
disp_dir=${TECHTERMS_DIR}/output/terms/${DISP}
eval_dir=${TECHTERMS_DIR}/output/eval/${DISP}

input_file=${model_dir}/${base_filename}.${DISP}.6gram.data
terms_file=${disp_dir}/pred_pos.list
output_file=${eval_dir}/doccoverage.txt

techterms_jar="${TECHTERMS_DIR}/src/technicalterms/target/technicalterms-1.0.jar"
factorie_jar="${TECHTERMS_DIR}/src/factorie-factorie_2.11-1.1/target/factorie_2.10-1.1-SNAPSHOT-nlp-jar-with-dependencies.jar"



if [ ! -e ${data_file} ]; then
    echo "$data_file does not exist. Provide correct file path"
    exit 1
fi

if [ ! -e ${input_file} ]; then
    echo "${input_file} not found. Run ./gen_disp_data.sh"
    exit 1    
fi

if [ ! -d ${disp_dir} ]; then
    echo "${disp_dir} not found. Run ./gen_disp_data.sh" 
    exit 1
fi

if [ ! -e ${terms_file} ]; then
    echo "${terms_file} not found. Run ./gen_terms.sh" 
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

if [ ! -d ${eval_dir} ]; then
    mkdir -p ${eval_dir}
fi




##### args #####

JVM_memory=3g

################


coverage_app="java -Xmx${JVM_memory} -cp ${techterms_jar}:${factorie_jar} edu.umass.cs.docCoverage"

echo ${coverage_app}

${coverage_app} --train-file=${input_file} --term-file=${terms_file} --output-file=${output_file}
