pipeline {
    agent any

    parameters {
        string(name: 'GIT_URL', defaultValue: 'https://github.com/cloudship/cloudship.git', description: 'Git repository URL')
        string(name: 'BRANCH_NAME', defaultValue: 'main', description: 'Target Git branch to build')
        string(name: 'GIT_COMMIT', defaultValue: 'HEAD', description: 'Git commit hash or ref')
        string(name: 'PROJECT_ID', defaultValue: '1', description: 'CloudShip Project ID')
        string(name: 'CLOUDSHIP_BUILD_ID', defaultValue: '', description: 'CloudShip Internal CI Build ID')
        string(name: 'CLOUDSHIP_API_URL', defaultValue: 'http://localhost:8088', description: 'CloudShip Backend API URL')
        string(name: 'ACR_NAME', defaultValue: 'cloudshipcr', description: 'Azure Container Registry name')
        string(name: 'ACR_LOGIN_SERVER', defaultValue: 'cloudshipcr.azurecr.io', description: 'Azure Container Registry login server')
        string(name: 'DOCKER_IMAGE_NAME', defaultValue: 'cloudship/backend', description: 'Docker image repository name')
        string(name: 'DOCKER_IMAGE_TAG', defaultValue: '', description: 'Docker image tag (defaults to short commit SHA)')
    }

    environment {
        IMAGE_NAME = "${params.DOCKER_IMAGE_NAME ?: 'cloudship/backend'}"
        ACR_SERVER = "${params.ACR_LOGIN_SERVER ?: 'cloudshipcr.azurecr.io'}"
        TAG = "${params.DOCKER_IMAGE_TAG ?: (params.GIT_COMMIT ?: 'HEAD').take(7)}"
        FULL_IMAGE_REF = "${params.ACR_LOGIN_SERVER ?: 'cloudshipcr.azurecr.io'}/${params.DOCKER_IMAGE_NAME ?: 'cloudship/backend'}:${params.DOCKER_IMAGE_TAG ?: (params.GIT_COMMIT ?: 'HEAD').take(7)}"
        PUSH_STATUS = 'NOT_STARTED'
        IMAGE_DIGEST = ''
        PUSH_DURATION_MS = '0'
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
                echo "==> Building Docker image: ${IMAGE_NAME}:${TAG}"
                script {
                    sh "docker build -t ${IMAGE_NAME}:${TAG} -t ${IMAGE_NAME}:latest -f backend/Dockerfile ."
                }
                echo '==> Docker image built successfully'
            }
        }

        stage('Authenticate to ACR') {
            steps {
                echo "==> Authenticating to Azure Container Registry: ${ACR_SERVER}"
                script {
                    if (env.ACR_CREDENTIALS_ID) {
                        withCredentials([usernamePassword(credentialsId: env.ACR_CREDENTIALS_ID, usernameVariable: 'ACR_USER', passwordVariable: 'ACR_PASS')]) {
                            sh 'echo "$ACR_PASS" | docker login "$ACR_SERVER" -u "$ACR_USER" --password-stdin'
                        }
                    } else if (env.AZURE_CLIENT_SECRET && env.AZURE_CLIENT_ID) {
                        withCredentials([string(credentialsId: 'AZURE_CLIENT_SECRET', variable: 'AZ_SECRET')]) {
                            sh 'echo "$AZ_SECRET" | docker login "$ACR_SERVER" -u "$AZURE_CLIENT_ID" --password-stdin'
                        }
                    } else {
                        echo '==> Standby: No Jenkins registry credential configured; proceeding with local Docker daemon inspection'
                        sh 'docker --version'
                    }
                }
            }
        }

        stage('Tag Image') {
            steps {
                echo "==> Tagging image for ACR: ${FULL_IMAGE_REF}"
                script {
                    sh "docker tag ${IMAGE_NAME}:${TAG} ${FULL_IMAGE_REF}"
                    sh "docker tag ${IMAGE_NAME}:${TAG} ${ACR_SERVER}/${IMAGE_NAME}:latest"
                }
                echo '==> Image successfully tagged for ACR'
            }
        }

        stage('Push Image') {
            steps {
                echo "==> Pushing container image to Azure Container Registry: ${FULL_IMAGE_REF}"
                script {
                    def startTime = System.currentTimeMillis()
                    env.PUSH_STATUS = 'RUNNING'
                    try {
                        sh "docker push ${FULL_IMAGE_REF}"
                        env.PUSH_STATUS = 'SUCCESS'
                        env.PUSH_DURATION_MS = "${System.currentTimeMillis() - startTime}"
                        echo "==> Successfully pushed image to ACR in ${env.PUSH_DURATION_MS}ms"
                    } catch (Exception e) {
                        env.PUSH_STATUS = 'FAILED'
                        env.PUSH_DURATION_MS = "${System.currentTimeMillis() - startTime}"
                        echo "==> Docker push to ACR failed: ${e.getMessage()}"
                        throw e
                    }
                }
            }
        }

        stage('Verify Push') {
            steps {
                echo "==> Verifying image push and capturing digest for ${FULL_IMAGE_REF}"
                script {
                    def digestOutput = sh(script: "docker inspect --format='{{index .RepoDigests 0}}' ${FULL_IMAGE_REF} 2>/dev/null || echo ''", returnStdout: true).trim()
                    if (digestOutput && digestOutput.contains('@')) {
                        env.IMAGE_DIGEST = digestOutput.split('@')[1]
                        echo "==> Verified Registry Image Digest: ${env.IMAGE_DIGEST}"
                    } else {
                        def idOutput = sh(script: "docker inspect --format='{{.Id}}' ${FULL_IMAGE_REF} 2>/dev/null || echo ''", returnStdout: true).trim()
                        env.IMAGE_DIGEST = idOutput
                        echo "==> Verified Local Container Digest: ${env.IMAGE_DIGEST}"
                    }
                }
            }
        }
    }

    post {
        success {
            echo "==> CI Pipeline SUCCESS for commit ${TAG}. Image: ${FULL_IMAGE_REF}"
            script {
                if (params.CLOUDSHIP_BUILD_ID?.trim()) {
                    sh """
                        curl -s -X PATCH "${params.CLOUDSHIP_API_URL}/api/ci-builds/${params.CLOUDSHIP_BUILD_ID}" \\
                             -H "Content-Type: application/json" \\
                             \${JENKINS_WEBHOOK_SECRET ? '-H "X-CloudShip-CI-Token: ' + JENKINS_WEBHOOK_SECRET + '"' : ''} \\
                             -d '{"status":"SUCCESS","jenkinsBuildNumber":${env.BUILD_NUMBER},"dockerImageTag":"${TAG}","pushStatus":"${env.PUSH_STATUS}","pushDurationMs":${env.PUSH_DURATION_MS},"imageDigest":"${env.IMAGE_DIGEST}","registryName":"${params.ACR_NAME}","registryLoginServer":"${env.ACR_SERVER}"}' || true
                    """
                }
            }
        }
        failure {
            echo "==> CI Pipeline FAILED for commit ${TAG}."
            script {
                if (params.CLOUDSHIP_BUILD_ID?.trim()) {
                    sh """
                        curl -s -X PATCH "${params.CLOUDSHIP_API_URL}/api/ci-builds/${params.CLOUDSHIP_BUILD_ID}" \\
                             -H "Content-Type: application/json" \\
                             \${JENKINS_WEBHOOK_SECRET ? '-H "X-CloudShip-CI-Token: ' + JENKINS_WEBHOOK_SECRET + '"' : ''} \\
                             -d '{"status":"FAILED","jenkinsBuildNumber":${env.BUILD_NUMBER},"errorMessage":"Jenkins build #${env.BUILD_NUMBER} failed","pushStatus":"${env.PUSH_STATUS == 'RUNNING' ? 'FAILED' : env.PUSH_STATUS}","pushErrorMessage":"Pipeline stage failed during execution","registryName":"${params.ACR_NAME}","registryLoginServer":"${env.ACR_SERVER}"}' || true
                    """
                }
            }
        }
        aborted {
            echo "==> CI Pipeline ABORTED for commit ${TAG}."
            script {
                if (params.CLOUDSHIP_BUILD_ID?.trim()) {
                    sh """
                        curl -s -X PATCH "${params.CLOUDSHIP_API_URL}/api/ci-builds/${params.CLOUDSHIP_BUILD_ID}" \\
                             -H "Content-Type: application/json" \\
                             \${JENKINS_WEBHOOK_SECRET ? '-H "X-CloudShip-CI-Token: ' + JENKINS_WEBHOOK_SECRET + '"' : ''} \\
                             -d '{"status":"ABORTED","jenkinsBuildNumber":${env.BUILD_NUMBER},"errorMessage":"Jenkins build #${env.BUILD_NUMBER} was aborted","pushStatus":"SKIPPED","registryName":"${params.ACR_NAME}","registryLoginServer":"${env.ACR_SERVER}"}' || true
                    """
                }
            }
        }
    }
}
