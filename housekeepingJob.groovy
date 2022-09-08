
String basePath = 'teams/<team_name>'
String product = 'xxx'
String triggerPath = "xxxx/${basePath}"
String service = 'ftp-app'
String serverRole = 'app'
String provider = 'aws'

def envSlaves = ['staging':'staging_eu-west-1_cloudbees_agent_ondemand_ftpadmin-management', 'production':'production_eu-west-1_cloudbees_agent_ondemand_ftpadmin-management']
def customers = [ 'shd' ]
def targets = ['eu-west-1']

// create folder structure
for (env in envSlaves) {
  for (customer in customers) {
    for (target in targets) {

      folder("${basePath}/${env.key}/${product}") {
        displayName("${product}")
        description("${env.key} ${product} jobs")
      }

      folder("${basePath}/${env.key}/${product}/${service}") {
        displayName("${service}")
        description("${env.key} ${product} ${service} jobs")
      }

      folder("${basePath}/${env.key}/${product}/${service}/${customer}") {
        displayName("${customer}")
        description("${env.key} ${product} ${service} ${customer} jobs")
      }

      folder("${basePath}/${env.key}/${product}/${service}/${customer}/${serverRole}") {
        displayName("${serverRole}")
        description("${env.key} ${product} ${service} ${customer} ${serverRole} jobs")
      }

      folder("${basePath}/${env.key}/${product}/${service}/${customer}/${serverRole}/${provider}") {
        displayName("${provider}")
        description("${env.key} ${product} ${service} ${customer} ${serverRole} ${provider} jobs")
      }

      folder("${basePath}/${env.key}/${product}/${service}/${customer}/${serverRole}/${provider}/${target}") {
        displayName("${target}")
        description("${env.key} ${product} ${service} ${customer} ${serverRole} ${provider} ${target} jobs")
      }
    }
  }
}

// create jenkins jobs
for (env in envSlaves) {
  for (customer in customers) {
    for (target in targets) {

      freeStyleJob("$basePath/${env.key}/${product}/${service}/${customer}/${serverRole}/${provider}/${target}/ftp_cleanup") {

        displayName('01 Cleanup FTP')
        description('01 Remove files older than 15 days from ftp servers')
        
        logRotator {
          numToKeep(10)
        }
        triggers {
           cron('H 03 * * *')
        }
        wrappers {
          preBuildCleanup()
        }
        label(env.value)

        steps {
          // Clean before build
        
          shell("""
            #!/bin/bash

            export ANSIBLE_HOST_KEY_CHECKING=False
            export ANSIBLE_FORCE_COLOR=true
            export PYTHONUNBUFFERED=1
            export AWS_DEFAULT_REGION=eu-west-1

            #clone repos
            git clone --branch master git@github.com:metapack-infrastructure/devops-infrastructure.git
            git -C /home/ubuntu/ clone --branch metapack-2.4 git@github.com:metapack-infrastructure/ansible.git ansible-metapack-2.4

            source ~/ansible-metapack-2.4/hacking/env-setup
            ansible --version

            cd devops-infrastructure/ansible2/
            ansible-playbook -i tasks/${product}/${service}/${serverRole}/${env.key}/hosts -vv tasks/${product}/${service}/${serverRole}/main.yml --sudo -e provider=${provider} -e target=${target} -e env=${env.key} -e product=${product} -e service=${service} -e customer=${customer} -e server_role=${serverRole} --vault-password-file ~/.vault/${env.key}

          """.stripIndent().trim())
        }

        wrappers {
          colorizeOutput('xterm')
          timestamps()
        }

      }

    }
  }
}
