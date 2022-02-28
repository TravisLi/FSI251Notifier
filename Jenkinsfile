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
    agent{
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
		        db_host = 'mongodb'
		        db_user = credentials('db_user')
		        db_password = credentials('db_password')
		        web_user = credentials('web_user')
		        web_password = credentials('web_password')
		        email_username = credentials('email_username')
		        email_password = credentials('email_password')
        	} 
           	steps {
            	echo 'Test start'
            	script{
            		withDockerNetwork{n ->
            			docker.image('mongo:latest').withRun('-e "MONGO_INITDB_ROOT_USERNAME=admin" -e "MONGO_INITDB_ROOT_PASSWORD=password" -p 27017:27017 --network-alias mongodb')
            		}
            	}
                sh 'mvn test' 
            }
            post {
                success {
                    junit 'target/surefire-reports/*.xml' 
                }
            }
        }
    }
}