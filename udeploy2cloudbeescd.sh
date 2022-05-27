usage () {
    cat <<HELP_USAGE
    $0 <json file>
HELP_USAGE
}

if [ $# != 1 ]; then
  usage
  exit 0
fi

ectool evalDsl --dslFile udeploy2cloudbeescd.groovy --parametersFile $1
