pipeline {
  agent any
  options { timestamps(); disableConcurrentBuilds() }

  environment {
    DEPLOY_DIR = "/opt/linbook"
    DOCKER_HOST = "unix:///var/run/docker.sock"

    // compose 호출 통일 (프로젝트 분리 + 오버라이드 포함)
    COMPOSE = "docker compose -p linbookapp -f compose.yml -f compose.ci.yml -f compose.ports.yml"

    // 헬스체크 포트
    BE_PORT = "8080"
    FE_PORT = "3001"

    // (경고 억제용) compose.yml에 남아있는 변수 경고를 잠재움
    BE_IMAGE = "linbook/be:dev"
    FE_IMAGE = "linbook/fe:dev"
  }

  stages {
    stage('Checkout') {
      steps { checkout scm }
    }

    stage('Prepare ports override (ensure 3001:3000)') {
      steps {
        sh '''
          # 워크스페이스에 compose.ports.yml 없으면 생성
          if [ ! -f compose.ports.yml ]; then
            cat > compose.ports.yml <<'YAML'
services:
  be:
    ports:
      - "8080:8080"
  fe:
    ports:
      - "3001:3000"
YAML
          fi

          # 안전을 위해 유효성만 확인
          grep -q '3001:3000' compose.ports.yml || { echo "compose.ports.yml FE 포트 미설정"; exit 1; }
        '''
      }
    }

    stage('Sync to server') {
      steps {
        sh '''
          mkdir -p "${DEPLOY_DIR}"

          # 서버의 compose.yml은 운영 값 보호 위해 유지(레포 값으로 덮지 않음)
          rsync -av --delete \
            --exclude compose.yml \
            --exclude .git/ \
            ./ "${DEPLOY_DIR}/"
        '''
      }
    }

    stage('Build images') {
      steps {
        sh '''
          set -e
          cd "${DEPLOY_DIR}"
          ${COMPOSE} config >/tmp/effective.yml || true
          echo "----- fe effective config -----"
          sed -n '/^  fe:/,/^  [a-z]/p' /tmp/effective.yml || true

          ${COMPOSE} build --pull be fe
        '''
      }
    }

    stage('Deploy') {
      steps {
        sh '''
          set -e
          cd "${DEPLOY_DIR}"

          # 기존 컨테이너 강제 제거(이름 충돌 방지)
          docker rm -f linbook-fe linbook-be || true

          # 분리된 프로젝트명으로 올림 (Jenkins 스택과 간섭 없음)
          ${COMPOSE} up -d --no-deps be fe

          docker image prune -f || true
        '''
      }
    }

    stage('Health Check') {
      steps {
        sh '''
          set +e

          echo "== BE health =="
          for i in $(seq 1 20); do
            if curl -sf "http://localhost:${BE_PORT}" >/dev/null; then
              echo "BE OK"; break
            fi
            sleep 1
          done

          echo "== FE health =="
          for i in $(seq 1 20); do
            if curl -sf "http://localhost:${FE_PORT}" >/dev/null; then
              echo "FE OK"; break
            fi
            sleep 1
          done
        '''
      }
    }
  }

  post {
    always {
      sh 'cd ${DEPLOY_DIR} && ${COMPOSE} ps || true'
    }
  }
}
