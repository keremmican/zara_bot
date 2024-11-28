pipeline {
    agent any
    environment {
        DOCKER_IMAGE = "zara-bot:latest" // Docker image adı
    }
    stages {
        stage('Checkout') {
            steps {
                // GitHub'dan kodu çek
                checkout scm
            }
        }
        stage('Build') {
            steps {
                // Maven ile projeyi build et
                sh 'chmod +x mvnw'
                sh './mvnw clean package'
            }
        }
        stage('Docker Build') {
            steps {
                // Docker image oluştur
                sh 'docker build -t $DOCKER_IMAGE .'
            }
        }
        stage('Docker Run') {
            steps {
                // Önceki container'ı durdur ve sil
                sh '''
                if [ $(docker ps -q -f name=zara-bot-container) ]; then
                    docker stop zara-bot-container
                    docker rm zara-bot-container
                fi
                '''
                // Yeni container'ı başlat
                sh '''
                docker run -d --name zara-bot-container -p 8088:8088 $DOCKER_IMAGE
                '''
            }
        }
    }
}
