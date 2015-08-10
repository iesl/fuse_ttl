#$ -S /bin/sh -cwd -o ../logs/gen_disp_data.out -e  ../logs/gen_disp_data.err
##!/usr/bin/env bash


source config

if [ -z ${TECHTERMS_DIR} ]; then
     echo "TECHTERMS_DIR" variable not set
     echo "set variable in config file"
     exit 1
fi



disp_dir=${TECHTERMS_DIR}/output/terms/${DISP}
if [ ! -d ${disp_dir} ]; then
   mkdir -p ${disp_dir}
fi 


gen_disp_data_code="${TECHTERMS_DIR}/src/python/gen_disp_data.py"


echo "Running ${gen_disp_data_code} to create disp level data and vocab for ${DISP}"

/opt/Python-2.6.4-x86_64/bin/python2.6 ${gen_disp_data_code} ${TECHTERMS_DIR} ${DATA_FILE} ${DISP} ${DISP_MAPPING_FILE}
