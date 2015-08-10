
## create the directory structure

if [ ! -d output ]; then
    mkdir output
    mkdir -p output/terms
    mkdir -p output/model
    mkdir -p output/eval
fi
if [ ! -d logs ]; then
    mkdir logs
fi

chmod -R +x scripts/*.sh
