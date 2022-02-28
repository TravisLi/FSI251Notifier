pipeline {
    agents{
    	agent{
    		docker {
            	image 'maven:3.8.1-openjdk-17' 
            	args '-v maven_repo:/root/.m2 -v /certs/client:/certs/client'
            	label 'maven'
        	}
    	}
    	agent {
        	docker {
            	image 'mongo:latest' 
            	args '-e MONGO_INITDB_ROOT_USERNAME=admin'
            	args '-e MONGO_INITDB_ROOT_PASSWORD=password'
            	args '-p 27017:27017'
            	args '--network-alias mongodb'
        		}
    		}
    	}
    }
    stages {
        stage('Build') {
            agent {
        		label 'maven'
    		}
            steps {
                sh 'mvn -B -DskipTests clean package'
            }
        }
        stage('Test') {
        	failFast true
        	parallel{
        		stage('Start Test'){
        			agent {
        				label 'maven'
    				}
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
    }
}