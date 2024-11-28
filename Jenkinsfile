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
                sh 'chmod +x mvnw' // Çalıştırma izni veriliyor
                sh './mvnw clean package' // Maven Wrapper ile build
            }
        }
        stage('Deploy') {
            steps {
                sh '''
                pid=$(pgrep -f "zara-0.0.1-SNAPSHOT.jar")
                if [ -n "$pid" ]; then
                    kill -9 $pid
                fi
                nohup java -jar target/zara-0.0.1-SNAPSHOT.jar > app.log 2>&1 &
                '''
            }
        }
    }
}