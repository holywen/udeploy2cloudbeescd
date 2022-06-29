#/bin/bash
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
environments_file=environments.txt

rm -f $components_file $app_processes_file $environments_file

project_name=$1
param_filename=$2

cat  << EOF > sharedFiles/config.json
{
  "projectName" : "${project_name}"
}
EOF

generate_element_list_file () {
  # $1 type: components/processes/environments, $2 list file, $3 json file
  if [ ! -f "$2" ]; then
    jq ".$1 [] | .name" $3 > $2
  fi
}

generate_element_list_file "components" "${components_file}" "${param_filename}"
generate_element_list_file "processes" "${app_processes_file}" "${param_filename}"
generate_element_list_file "environments" "${environments_file}" "${param_filename}"

run_migration_script () {
  OLD_IFS=$IFS
  IFS=$'\n'
  # $1 target data file, $2 list file, $3 script file, $4 parameter json file

  for item in $(cat ${2})
  do
    echo "${item//\"/}" > sharedFiles/$1
    echo "processing ${item//\"/}"
    ectool  evalDsl --dslFile $3 --clientFiles ./sharedFiles --parametersFile ${4} --overwrite 0
    rm -f clientFiles_*.zip
  done
  IFS=${OLD_IFS}
}

run_migration_script "targetComponent.txt" "${components_file}" "udeploy2cloudbeescdComponent.groovy" "${param_filename}"
run_migration_script "targetProcess.txt" "${app_processes_file}" "udeploy2cloudbeescdApplication.groovy" "${param_filename}"
run_migration_script "targetEnvironment.txt" "${environments_file}" "udeploy2cloudbeescdEnvironment.groovy" "${param_filename}"
