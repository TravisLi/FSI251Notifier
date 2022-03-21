pipeline {
    agent {
    	docker {
            image 'maven:3.8.1-openjdk-17' 
            args '--rm -v maven_repo:/root/.m2 -v /certs/client:/certs/client --name maven-container -e docker_host=tcp://172.17.0.1:2376 -e docker_cert_path=/certs/client'
        }
    }
    stages {
    	stage('Build') {
            steps {
                sh 'mvn -B -DskipTests clean package'
            }
        }
        stage('Test') {
        	environment {
		        azure_recognition_endpoint = credentials('azure_recognition_endpoint')
		        azure_recognition_key = credentials('azure_recognition_key')
		        azure_storage = credentials('azure_storage')
		        azure_client_secret = credentials('azure_client_secret')
		        azure_tenant_id = credentials('azure_tenant_id')
		        onedrive_share_url = credentials('onedrive_share_url')
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
                sh 'mvn verify' 
            }
            post {
                success {
                    junit 'target/*-reports/*.xml'
                    
                }
            }
        }
        stage('Push') {
        	environment {
		        docker_username = 'travisli'
				docker_password = credentials('docker_password')
        	} 
            steps {
                sh 'mvn -B -Ddocker.username=${docker_username} -Ddocker.password=${docker_password} docker:push'
            }
        }
        stage('Clean up') {
            steps {
                sh 'mvn -B docker:remove'
            }
        }
    }
}