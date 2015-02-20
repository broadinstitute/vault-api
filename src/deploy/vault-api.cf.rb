template.project = 'vault-api'

template.elastic_ip = case environment
when 'ci'
  '107.20.180.241'
when 'production'
  '107.22.184.238'
end

template.service_port = "8009"
