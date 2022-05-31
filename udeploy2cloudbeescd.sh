set -x
usage () {
    cat <<HELP_USAGE
    $0 <project name> <json file>
HELP_USAGE
}

if [ $# != 2 ]; then
  usage
  exit 0
fi

components_file=components.txt
project_name=$1
param_filename=$2

cat  << EOF > sharedFiles/config.json
{
  "projectName" : "${project_name}"
}
EOF

if [ ! -f "${components_file}" ]; then
  jq '.components [] | .name' $1 > ${components_file}
fi

IFS=$'\n'
for component in $(cat ${components_file})
do
  echo "${component//\"/}" > sharedFiles/targetComponent.txt
  echo "processing ${component//\"/}"
  #ectool  --timeout 3600 evalDsl --timeout 3600 --dslFile udeploy2cloudbeescdComponent.groovy --clientFiles ./sharedFiles --parametersFile ${param_filename} --overwrite 0
  #ectool evalDsl --dslFile udeploy2cloudbeescd.groovy --clientFiles ./sharedFiles --parametersFile $1
  rm -f clientFiles_*.zip
done

ectool  --timeout 3600 evalDsl --timeout 3600 --dslFile udeploy2cloudbeescdApplication.groovy --clientFiles ./sharedFiles --parametersFile ${param_filename} --overwrite 0
