switch(args.type){
    case "TEXT":
        formalParameter args.name, defaultValue: args.value, {
            description = args.description
            label = args.label
            required = args.required == true? '1':'0';
            type = 'entry'
        }
        break;
    case "SELECT":
        formalParameter args.name, defaultValue:  args.value, {
            expansionDeferred = '0'
            label = args.label
            def optionsMap = [:]
            args.allowedValues.each { allowedValue ->
                optionsMap[(allowedValue.label)] = allowedValue.value
            }

            options = optionsMap
            required = args.required == true? '1':'0';
            type = 'select'
        }
        break;
    case "TEXTAREA":
        formalParameter args.name, defaultValue: args.value, {
            description = args.description
            label = args.label
            required = args.required == true? '1':'0';
            type = 'textarea'
        }
        break;
    case "CHECKBOX":
        formalParameter args.name, defaultValue: args.value,{
            checkedValue = 'true'
            type = 'checkbox'
            uncheckedValue = 'false'
        }
        break;
    case "SECURE":
        formalParameter args.name, defaultValue: null, {
            label = args.label
            required = args.required == true? '1':'0';
            type = 'credential'
        }
        break;
    default:
        println "unsupported component parameter type " + args
}