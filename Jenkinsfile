pipeline {
    agent {
        docker {
            image 'maven:3.8.1-openjdk-17' 
            args '-v maven_repo:/root/.m2' 
        }
    }
    stages {
        stage('Build') {
            steps {
                sh 'mvn -B -DskipTests clean package docker:build'
            }
        }
        stage('Test') { 
            steps {
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