pipeline {
  agent any

  environment {
    REGISTRY = "ghcr.io"
    ORG      = "236-2su"             // TODO: set
    EC2_IP   = "54.252.41.133" // TODO: set

    BE_IMAGE = "${REGISTRY}/${ORG}/linbook-be"
    FE_IMAGE = "${REGISTRY}/${ORG}/linbook-fe"

    COMMIT_TAG = "${env.BRANCH_NAME}-${env.GIT_COMMIT.take(7)}"
    LIVE_TAG   = "live"
    DOCKER_BUILDKIT = '1'
  }

  options { timestamps(); ansiColor('xterm') }

  stages {
    stage('Checkout') { steps { checkout scm } }

    stage('Docker Login (GHCR)') {
      steps {
        withCredentials([string(credentialsId: 'GHCR_PAT', variable: 'REGISTRY_TOKEN')]) {
          sh '''
            set -e
            echo "$REGISTRY_TOKEN" | docker login ${REGISTRY} -u ${ORG} --password-stdin
          '''
        }
      }
    }

    stage('Build & Push Images') {
      steps {
        sh '''
          set -e
          docker buildx create --use || true

          docker buildx build -f ./linbook_be/Dockerfile \
            -t ${BE_IMAGE}:${COMMIT_TAG} -t ${BE_IMAGE}:${LIVE_TAG} \
            --push ./linbook_be

          docker buildx build -f ./linbook_fe/Dockerfile \
            -t ${FE_IMAGE}:${COMMIT_TAG} -t ${FE_IMAGE}:${LIVE_TAG} \
            --push ./linbook_fe
        '''
      }
    }

    stage('Deploy to EC2') {
      steps {
        withCredentials([string(credentialsId: 'GHCR_PAT', variable: 'REGISTRY_TOKEN')]) {
          sshagent(credentials: ['EC2_SSH']) {
            sh '''
              set -e
              ssh -o StrictHostKeyChecking=no ubuntu@${EC2_IP} "
                set -e
                cd /opt/linbook
                echo '${REGISTRY_TOKEN}' | docker login ${REGISTRY} -u ${ORG} --password-stdin
                docker compose pull
                docker compose up -d
                docker image prune -f
              "
            '''
          }
        }
      }
    }
  }

  post {
    success { echo "Deployed ${BRANCH_NAME}@${GIT_COMMIT.take(7)} to ${EC2_IP}" }
    failure { echo "❌ Pipeline failed. Check logs." }
    always  { sh 'docker logout ${REGISTRY} || true' }
  }
}
