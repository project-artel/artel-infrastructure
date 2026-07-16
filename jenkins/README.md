# Jenkins controller

This directory is the Docker build context for the ARTEL Jenkins controller.

## Files

- `Dockerfile`: Jenkins LTS JDK 21 image with Docker CLI, Buildx, Compose, AWS CLI, plugins, and JCasC.
- `plugins.txt`: Jenkins plugins installed at image build time.
- `casc.yaml`: Jenkins Configuration as Code bootstrap.
- `seed-job.groovy`: Creates the service Multibranch Pipeline jobs.
- `docker-compose.yml`: Minimal EC2 runtime compose file for the Jenkins container.

## Image

GitHub Actions builds and pushes:

```text
ghcr.io/project-artel/infra/jenkins:latest
```

The EC2 instance should pull and run this image. It should not build the Jenkins
image locally.

## Runtime

Required environment values:

```text
JENKINS_ADMIN_ID
JENKINS_ADMIN_PASSWORD
JENKINS_URL
DOCKER_GID
```

The container mounts `/var/run/docker.sock` so Jenkins jobs can build Docker
images later.

## GitHub App

After Jenkins starts, register the GitHub App private key as:

```text
Credential ID: github-project-artel
Kind: GitHub App
```

The webhook endpoint is:

```text
http://15.164.69.110:8080/github-webhook/
```

Use `https://jenkins.artel.kr/github-webhook/` after DNS and TLS are ready.
