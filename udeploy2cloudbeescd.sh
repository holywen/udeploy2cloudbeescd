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
app_processes_file=processes.txt

project_name=$1
param_filename=$2

cat  << EOF > sharedFiles/config.json
{
  "projectName" : "${project_name}"
}
EOF

if [ ! -f "${components_file}" ]; then
  jq '.components [] | .name' ${param_filename} > ${components_file}
fi

if [ ! -f "${app_processes_file}" ]; then
  jq '.processes [] | .name' ${param_filename} > ${app_processes_file}
fi

IFS=$'\n'
for component in $(cat ${components_file})
do
  echo "${component//\"/}" > sharedFiles/targetComponent.txt
  echo "processing ${component//\"/}"
  ectool  evalDsl --dslFile udeploy2cloudbeescdComponent.groovy --clientFiles ./sharedFiles --parametersFile ${param_filename} --overwrite 0
  rm -f clientFiles_*.zip
done

for process in $(cat ${app_processes_file})
do
  echo "${process//\"/}" > sharedFiles/targetProcess.txt
  ectool  evalDsl --dslFile udeploy2cloudbeescdApplication.groovy --clientFiles ./sharedFiles --parametersFile ${param_filename} --overwrite 0
  rm -f clientFiles_*.zip
done
