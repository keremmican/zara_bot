pipeline {
    agent any
    stages {
        stage('Checkout') {
            steps {
                checkout scm
            }
        }
        stage('Build') {
            steps {
                sh './mvnw clean package' //
            }
        }
        stage('Deploy') {
            steps {
                sh 'java -jar target/zara-0.0.1-SNAPSHOT.jar' //
            }
        }
    }
}
