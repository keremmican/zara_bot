pipeline {
    agent any
    environment {
        DOCKER_IMAGE = "zara-bot:latest"
    }
    stages {
        stage('Checkout') {
            steps {
                checkout scm
            }
        }
        stage('Build') {
            steps {
                sh 'chmod +x mvnw'
                sh './mvnw clean package'
            }
        }
        stage('Docker Build') {
            steps {
                sh 'docker build -t $DOCKER_IMAGE .'
            }
        }
        stage('Docker Run') {
            steps {
                sh '''
                if [ $(docker ps -aq -f name=zara-bot-container) ]; then
                    echo "Stopping and removing existing container"
                    docker stop zara-bot-container
                    docker rm zara-bot-container
                fi

                docker run --network="host" -d --name zara-bot-container -p 8088:8088 $DOCKER_IMAGE
                '''
            }
        }
    }
}
