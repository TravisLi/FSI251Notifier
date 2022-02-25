pipeline {
    agent {
        docker {
            image 'maven:3.8.1-openjdk-17' 
            args '-v maven_repo:/root/.m2 --network jenkins' 
        }
    }
    stages {
        stage('Build') {
            steps {
                sh 'mvn -B -DskipTests clean package docker:build'
            }
        }
        stage('Test') {
        	environment {
        		azure_endpoint = credentials('azure_endpoint')
        		azure_key = credentials('azure_key')
        		azure_storage = credentials('azure_storage')
        		db_host = credentials('db_host')
        		db_user = credentials('db_user')
        		db_password = credentials('db_password')
        		web_user = credentials('web_user')
        		web_password = credentials('web_password')
        	} 
            steps {
            	echo 'Test start'
                sh 'mvn docker:start test docker:stop' 
            }
            post {
                always {
                    junit 'target/surefire-reports/*.xml' 
                }
            }
        }
    }
}