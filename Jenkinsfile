pipeline {
  agent any
  options { timestamps(); disableConcurrentBuilds() }
  environment {
    DEPLOY_DIR = "/opt/linbook"
    DOCKER_HOST = "unix:///var/run/docker.sock"
    COMPOSE = "docker compose -p linbookapp -f compose.yml -f compose.ci.yml"
  }
  stages {
    stage('Checkout'){ steps { checkout scm } }
    stage('Sanity'){ steps { sh 'docker --version && docker compose version || true' } }
    stage('Sync'){
      steps { sh 'rsync -av --delete --exclude compose.yml ./ ${DEPLOY_DIR}/' }
    }
    stage('Build'){
      steps { sh "cd ${DEPLOY_DIR} && ${COMPOSE} build --pull be fe" }
    }
    stage('Deploy'){
      steps { 
        sh '''
          cd ${DEPLOY_DIR}
          
          # 기존 컨테이너 강제 중지 및 삭제
          docker rm -f linbook-be linbook-fe || true
          
          ${COMPOSE} up -d --no-deps be fe
          docker image prune -f
        '''
      }
    }
    stage('Health Check'){
      steps { 
        sh '''
          sleep 30
          curl -f http://localhost:8080 || exit 1
          curl -f http://localhost:3001 || exit 1
        '''
      }
    }
  }
  post { 
    always { 
      sh "cd ${DEPLOY_DIR} && ${COMPOSE} ps || true"
      sh 'docker system prune -f'
    } 
  }
}
