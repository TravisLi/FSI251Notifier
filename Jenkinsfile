def withDockerNetwork(Closure inner) {
  try {
    networkId = UUID.randomUUID().toString()
    sh "docker network create ${networkId}"
    inner.call(networkId)
  } finally {
    sh "docker network rm ${networkId}"
  }
}

pipeline {
    agent {
    	docker {
            image 'maven:3.8.1-openjdk-17' 
            args '-v maven_repo:/root/.m2 -v /certs/client:/certs/client'
        }
    }
    stages {
        stage('Test') {
        	environment {
		        azure_endpoint = credentials('azure_endpoint')
		        azure_key = credentials('azure_key')
		        azure_storage = credentials('azure_storage')
		        db_host = 'mongo-db'
		        db_user = credentials('db_user')
		        db_password = credentials('db_password')
		        web_user = credentials('web_user')
		        web_password = credentials('web_password')
		        email_username = credentials('email_username')
		        email_password = credentials('email_password')
		        spring_profiles_active = 'dev'
        	} 
           	steps {
            	echo 'Test start'
                sh 'mvn docker:start test -Dtest="EmailSenderIntegrationTest" docker:stop' 
            }
            post {
                success {
                    junit 'target/surefire-reports/*.xml' 
                }
            }
        }
    }
}