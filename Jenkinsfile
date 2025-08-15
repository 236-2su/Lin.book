pipeline {
  agent any
  options { timestamps(); disableConcurrentBuilds() }
  environment {
    DEPLOY_DIR = "/opt/linbook"
    DOCKER_HOST = "unix:///var/run/docker.sock"
  }
  stages {
    stage('Checkout'){ steps { checkout scm } }
    stage('Sanity'){ steps { sh 'docker --version && docker compose version || true' } }
    stage('Sync'){
      steps { sh 'rsync -av --delete --exclude compose.yml ./ ${DEPLOY_DIR}/' }
    }
    stage('Build'){
      steps { sh 'cd ${DEPLOY_DIR} && docker compose build --pull be fe' }
    }
    stage('Deploy'){
      steps { sh 'cd ${DEPLOY_DIR} && docker compose up -d --no-deps be fe && docker image prune -f' }
    }
  }
  post { always { sh 'cd ${DEPLOY_DIR} && docker compose ps || true' } }
}
