pipeline {
    agent any

    environment {
        // Jenkins中配置的Docker Hub凭证ID
        DOCKER_HUB_CREDENTIALS = credentials('1')
        // Docker Hub镜像名（用户名/仓库名）
        DOCKER_IMAGE = 'ssssstrid/teedy-app'
        // 自动使用构建编号作为tag
        DOCKER_TAG = "${env.BUILD_NUMBER}"
    }

    stages {
        stage('Build') {
            steps {
                checkout scmGit(
                    branches: [[name: '*/master']],
                    extensions: [],
                    userRemoteConfigs: [[url: 'https://github.com/Zhang-Astrid/Teedy.git']]
                )
            }
        }

        stage('Build Docker Image') {
            steps {
                script {
                    docker.build("${env.DOCKER_IMAGE}:${env.DOCKER_TAG}")
                }
            }
        }

        stage('Upload Image') {
            steps {
                script {
                    docker.withRegistry('https://registry.hub.docker.com', 'DOCKER_HUB_CREDENTIALS') {
                        docker.image("${env.DOCKER_IMAGE}:${env.DOCKER_TAG}").push()
                        docker.image("${env.DOCKER_IMAGE}:${env.DOCKER_TAG}").push('latest')
                    }
                }
            }
        }

        stage('Run Docker Container') {
            steps {
                script {
                    sh 'docker stop teedy-container-8080 || true'
                    sh 'docker rm teedy-container-8080 || true'
                    docker.image("${env.DOCKER_IMAGE}:${env.DOCKER_TAG}").run(
                        '--name teedy-container-8080 -d -p 8080:8080'
                    )
                    sh 'docker ps --filter "name=teedy-container"'
                }
            }
        }
    }
}
