#!/bin/bash

#$ -S /bin/sh -cwd -o ../logs/kmeans.out -e  ../logs/kmeans.err
##!/usr/bin/env bash

source config

if [ -z ${TECHTERMS_DIR} ]; then
     echo "TECHTERMS_DIR" variable not set
     echo "set variable in config file"
     exit 1
fi

##### input file path #####

data_file=${DATA_FILE} ## came from config file
base_filename=$(basename ${data_file})
model_dir=${TECHTERMS_DIR}/output/model
disp_dir=${TECHTERMS_DIR}/output/terms/${DISP}
disp_vocab_file=${model_dir}/${base_filename}.${DISP}.6gram.vocab
embedding_file=${model_dir}/${base_filename}.6gram.embeddings
kmeans_out_file=${disp_dir}/kmeans.out

if [ ! -e ${disp_dir} ]; then
  echo "${disp_dir} not found. Run gen_disp_data.sh"
  exit 1
fi


if [ ! -e ${disp_vocab_file} ]; then
    echo "${disp_vocab_file} not found. Run gen_disp_data.sh"
    exit 1
fi

if [ ! -e ${embedding_file} ]; then
   echo "${embedding_file} not found. Run gen_embeddings.sh"
   exit 1
fi


K=400
kmeans_code=${TECHTERMS_DIR}/src/python/kmeans.py
/opt/Python-2.6.4-x86_64/bin/python2.6 ${kmeans_code} ${disp_vocab_file} ${embedding_file} ${kmeans_out_file} ${K}
