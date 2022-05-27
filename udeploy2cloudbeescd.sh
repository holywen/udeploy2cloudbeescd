usage () {
    cat <<HELP_USAGE
    $0 <json file>
HELP_USAGE
}

if [ $# != 1 ]; then
  usage
  exit 0
fi
ectool --timeout 3600 evalDsl --dslFile udeploy2cloudbeescd.groovy --parametersFile $1 --overwrite 0

#ectool evalDsl --dslFile udeploy2cloudbeescd.groovy --clientFiles ./sharedFiles --parametersFile $1
