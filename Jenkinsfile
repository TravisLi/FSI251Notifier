pipeline {
    agent {
    	docker {
            image 'maven:3.8.1-openjdk-17' 
            args '--name maven-container'
            args '-v maven_repo:/root/.m2 -v /certs/client:/certs/client'
        }
    }
    stages {
    	stage('Build') {
            steps {
                sh 'mvn -B -DskipTests clean package docker:build'
            }
        }
        /*stage('Test') {
        	environment {
		        azure_endpoint = credentials('azure_endpoint')
		        azure_key = credentials('azure_key')
		        azure_storage = credentials('azure_storage')
		        db_host = '172.17.0.1'
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
                sh 'mvn docker:start test docker:stop' 
            }
            post {
                success {
                    junit 'target/surefire-reports/*.xml' 
                }
            }
        }*/
        stage('Push') {
        	environment {
      			docker_host = 'tcp:////172.17.0.1:2376'
      			docker_cert_path = '/certs/client'
				docker_username = 'travisli'
				docker_password = credentials('docker_password')
    		} 
            steps {
                sh 'mvn -B docker:push'
            }
        }
    }
}