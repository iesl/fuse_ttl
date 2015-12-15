#$ -S /bin/sh -cwd -o ../logs/gen_pos_neg_lists.out -e  ../logs/gen_pos_neg_lists.err
##!/usr/bin/env bash

source config


if [ -z ${TECHTERMS_DIR} ]; then
     echo "TECHTERMS_DIR" variable not set
     echo "set variable in config file"
     exit 1
fi


disp_dir=${TECHTERMS_DIR}/output/terms/${DISP}

if [ ! -d ${disp_dir} ]; then
   echo "${disp_dir} not found."
   echo run "${TECHTERMS_DIR}/scripts/gen_disp_data.sh"
   exit 1
fi

gen_pos_neg_code="${TECHTERMS_DIR}/src/python/gen_pos_neg_lists.py"
neg_len_cut_off=100
pos_len_cut_off=150
max_len=50000
lower_score_threshold=0.5
high_score_threshold=1.0

echo "Running ${gen_pos_neg_code} to create generate positive and negative examples ${DISP}. Output is ${TECHTERMS_DIR}/output/terms/${DISP}/pos.list and ${TECHTERMS_DIR}/output/terms/${DISP}/neg.list"

/opt/Python-2.6.4-x86_64/bin/python2.6 ${gen_pos_neg_code} ${TECHTERMS_DIR} ${DATA_FILE} ${DISP} ${neg_len_cut_off} ${pos_len_cut_off} ${max_len} ${lower_score_threshold} ${high_score_threshold}
