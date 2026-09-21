pipeline {
    agent any

    parameters {
        string(name: 'GIT_URL', defaultValue: 'https://github.com/cloudship/cloudship.git', description: 'Git repository URL')
        string(name: 'BRANCH_NAME', defaultValue: 'main', description: 'Target Git branch to build')
        string(name: 'GIT_COMMIT', defaultValue: 'HEAD', description: 'Git commit hash or ref')
        string(name: 'PROJECT_ID', defaultValue: '1', description: 'CloudShip Project ID')
    }

    environment {
        IMAGE_NAME = 'cloudship/backend'
        SHORT_COMMIT = "${params.GIT_COMMIT}".take(7)
    }

    stages {
        stage('Checkout') {
            steps {
                echo "==> Checking out repository: ${params.GIT_URL} (branch: ${params.BRANCH_NAME}, commit: ${params.GIT_COMMIT})"
                checkout([
                    $class: 'GitSCM',
                    branches: [[name: "${params.BRANCH_NAME}"]],
                    userRemoteConfigs: [[
                        url: "${params.GIT_URL}",
                        credentialsId: env.GIT_CREDENTIALS_ID ?: ''
                    ]]
                ])
            }
        }

        stage('Validate') {
            steps {
                echo '==> Validating project environment and workspace integrity'
                sh 'java -version'
                script {
                    if (!fileExists('backend/pom.xml')) {
                        error('Validation failed: backend/pom.xml does not exist')
                    }
                    if (!fileExists('backend/Dockerfile')) {
                        error('Validation failed: backend/Dockerfile does not exist')
                    }
                    if (!fileExists('frontend/index.html')) {
                        error('Validation failed: frontend/index.html does not exist')
                    }
                }
                echo '==> Project structure and required configurations verified'
            }
        }

        stage('Build') {
            steps {
                echo '==> Building CloudShip backend application with Maven'
                dir('backend') {
                    sh 'mvn clean package -DskipTests -B'
                }
            }
        }

        stage('Test') {
            steps {
                echo '==> Running automated test suite'
                dir('backend') {
                    sh 'mvn test -B'
                }
            }
            post {
                always {
                    junit testResults: 'backend/target/surefire-reports/*.xml', allowEmptyResults: true
                }
            }
        }

        stage('Docker Build') {
            steps {
                echo "==> Building Docker image: ${IMAGE_NAME}:${SHORT_COMMIT}"
                script {
                    sh "docker build -t ${IMAGE_NAME}:${SHORT_COMMIT} -t ${IMAGE_NAME}:latest -f backend/Dockerfile ."
                }
                echo '==> Docker image built successfully (registry push withheld per Phase 3 boundary)'
            }
        }
    }

    post {
        success {
            echo "==> CI Pipeline SUCCESS for commit ${SHORT_COMMIT}. Image: ${IMAGE_NAME}:${SHORT_COMMIT}"
        }
        failure {
            echo "==> CI Pipeline FAILED for commit ${SHORT_COMMIT}."
        }
        aborted {
            echo "==> CI Pipeline ABORTED for commit ${SHORT_COMMIT}."
        }
    }
}
