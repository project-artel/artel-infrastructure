# Jenkins controller image

EC2 인스턴스에서 CI/CD용 Jenkins 컨트롤러를 컨테이너로 실행하기 위한 이미지입니다.
현재 배포 대상은 `stage -> operation` 순서의 서버 구성을 전제로 합니다.

## Files

- `Dockerfile`: Jenkins LTS 기반 이미지. Docker CLI, AWS CLI, Jenkins plugins, JCasC 설정을 포함합니다.
- `docker-compose.yml`: EC2에서 Jenkins 컨테이너를 실행하는 기본 Compose 파일입니다.
- `plugins.txt`: 이미지 빌드 시 미리 설치할 Jenkins 플러그인 목록입니다. Jenkins for Jira 연동용 `atlassian-jira-software-cloud` 플러그인을 포함합니다.
- `casc.yaml`: Configuration as Code 기본 설정입니다.
- `seed-job.groovy`: `stage`와 `operation` 폴더를 만드는 초기 Job DSL입니다.
- `Jenkinsfile.example`: 애플리케이션 저장소에 둘 `stage -> operation` 파이프라인 예시입니다.

## Image build

Jenkins 이미지는 EC2에서 빌드하지 않습니다.
GitHub Actions가 `jenkins/` 디렉터리를 Docker build context로 사용해서 GHCR에 push합니다.

기본 이미지 이름은 다음 형식입니다.

```text
ghcr.io/<github-owner>/<infra-repository>/jenkins:latest
```

브랜치/PR/SHA 태그도 함께 생성됩니다.

## Run

EC2에서는 저장소를 `git clone`하지 않고 GHCR 이미지만 pull해서 실행합니다.

```bash
docker pull ghcr.io/<github-owner>/<infra-repository>/jenkins:latest
```

Docker socket을 Jenkins 컨테이너에 연결할 경우 먼저 group id를 확인합니다.

```bash
export DOCKER_GID=$(stat -c '%g' /var/run/docker.sock)
export JENKINS_ADMIN_PASSWORD='change-me'
docker run -d \
  --name soma-jenkins \
  --restart unless-stopped \
  -p 8080:8080 \
  -p 50000:50000 \
  -v jenkins_home:/var/jenkins_home \
  -v /var/run/docker.sock:/var/run/docker.sock \
  --group-add "${DOCKER_GID}" \
  -e JENKINS_ADMIN_ID=admin \
  -e JENKINS_ADMIN_PASSWORD="${JENKINS_ADMIN_PASSWORD}" \
  -e JENKINS_URL=http://<jenkins-host>:8080/ \
  -e STAGE_HOST=<stage-host> \
  -e OPERATION_HOST=<operation-host> \
  ghcr.io/<github-owner>/<infra-repository>/jenkins:latest
```

EC2에서 사용할 때는 `JENKINS_ADMIN_PASSWORD`를 하드코딩하지 말고 SSM Parameter Store,
Secrets Manager, 또는 인스턴스의 환경 변수 주입 방식으로 관리하세요.

## Next steps

1. Jenkins EC2 인스턴스에 Docker를 설치합니다.
2. Jenkins 컨테이너에는 `/var/jenkins_home`을 반드시 영속 볼륨으로 마운트합니다.
3. private GHCR 이미지를 쓸 경우 EC2에서 GitHub PAT로 `docker login ghcr.io`를 먼저 수행합니다.
4. 배포 대상 서버 접근은 SSH key 또는 AWS IAM Role 기반으로 구성합니다.
5. 애플리케이션 저장소에는 `Jenkinsfile`을 두고 `stage` 배포 후 승인 단계를 거쳐 `operation` 배포가 실행되도록 만듭니다.

## Jira integration

Jira Cloud에서 Jenkins for Jira 앱을 설치한 뒤 Jenkins 연결을 시작하면 webhook URL과 secret token이 발급됩니다.
Jenkins에서는 Manage Jenkins > Atlassian Jira Software Cloud 메뉴에서 이 값을 등록합니다.

Jira에서 빌드와 배포 상태를 이슈에 연결하려면 애플리케이션 저장소의 branch name, PR title,
또는 commit message에 Jira issue key가 포함되어야 합니다.

`Jenkinsfile.example`은 다음 이벤트를 Jira로 보냅니다.

- build stage: `jiraSendBuildInfo()`
- stage deployment: `jiraSendDeploymentInfo environmentType: 'staging'`
- operation deployment: `jiraSendDeploymentInfo environmentType: 'production'`
