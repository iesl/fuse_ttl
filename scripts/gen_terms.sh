#$ -S /bin/sh -cwd -o ../logs/gen_terms.out -e  ../logs/gen_terms.err
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

if [ ! -e ${disp_dir}/pos.list ]; then
    echo "${disp_dir} not found"
    echo "make list of postive examples for the disp ${DISP}"
    exit 1
fi

if [ ! -e ${disp_dir}/neg.list ]; then
    echo "${disp_dir} not found"
    echo "make list of negative examples for the disp ${DISP}"
    exit 1
fi


svm_clf_code="${TECHTERMS_DIR}/src/python/svm_clf.py"


echo "Running ${svm_clf_code} to create scientific entites for ${DISP}. Output is ${TECHTERMS_DIR}/output/terms/${DISP}/pred_pos.list"

##### args #####

prob_cutoff=0.8

################

/opt/Python-2.6.4-x86_64/bin/python2.6 ${svm_clf_code} ${TECHTERMS_DIR} ${DATA_FILE} ${DISP} ${DISP_MAPPING_FILE} ${prob_cutoff}
