# Spring Boot App Deployment Setup

This document provides the setup instructions needed on your DigitalOcean droplet to use the automated deployment workflow.

## Prerequisites on DigitalOcean Droplet

### 1. Create the systemd service file

Create `/etc/systemd/system/llm-orchestrator.service`:

```bash
sudo nano /etc/systemd/system/llm-orchestrator.service
```

Add the following content:

```ini
[Unit]
Description=LLM Orchestrator Spring Boot Application
After=network.target

[Service]
Type=simple
User=deploy
WorkingDirectory=/applications/llm-orchestrator
ExecStart=/usr/bin/java -jar /applications/llm-orchestrator/current/app.jar
Restart=on-failure
RestartSec=10
StandardOutput=journal
StandardError=journal
SyslogIdentifier=llm-orchestrator

[Install]
WantedBy=multi-user.target
```

### 2. Create deployment user

```bash
sudo useradd -m -s /bin/bash deploy
sudo mkdir -p /applications/llm-orchestrator
sudo chown -R deploy:deploy /applications/llm-orchestrator
```

### 3. Enable the service

```bash
sudo systemctl daemon-reload
sudo systemctl enable llm-orchestrator
```

### 4. Configure sudo permissions (optional but recommended)

Add the following to `/etc/sudoers` so deploy user can control the service without password:

```bash
sudo visudo
```

Add this line:
```
deploy ALL=(ALL) NOPASSWD: /bin/systemctl start llm-orchestrator, /bin/systemctl stop llm-orchestrator, /bin/systemctl status llm-orchestrator, /bin/journalctl
```

## GitHub Secrets to Configure

Add these secrets to your GitHub repository settings:

- `DROPLET_SSH_KEY` - Your private SSH key for the deployment user
- `DROPLET_IP` - Your DigitalOcean droplet IP address
- `DROPLET_USER` - The deployment user (default: `deploy`)

## Deployment Process

The workflow will automatically:

1. Build the Spring Boot JAR using Gradle
2. Create a versioned directory with timestamp and commit hash
3. Deploy the JAR file to `/applications/llm-orchestrator/versions/v_YYYYMMDD_HHMMSS_HASH/app.jar`
4. Create a symlink `/applications/llm-orchestrator/current` pointing to the new version
5. Stop the old application
6. Start the new application via systemd
7. Verify the deployment by checking the health endpoint
8. On failure: Automatically rollback to the previous version

## Directory Structure

```
/applications/llm-orchestrator/
├── current -> versions/v_20260225_120530_abc1234/  (symlink to latest)
├── versions/
│   ├── v_20260225_120530_abc1234/
│   │   └── app.jar
│   └── v_20260225_100000_xyz7890/
│       └── app.jar
└── .backups/
    ├── current_version
    └── previous_version
```

## Logs

View application logs using:

```bash
sudo journalctl -u llm-orchestrator -f
```

## Manual Rollback

If needed, you can manually rollback:

```bash
# Check available versions
ls -la /applications/llm-orchestrator/versions/

# Rollback to previous version
PREVIOUS_VERSION=$(cat /applications/llm-orchestrator/.backups/previous_version)
sudo rm -f /applications/llm-orchestrator/current
sudo ln -s /applications/llm-orchestrator/versions/$PREVIOUS_VERSION /applications/llm-orchestrator/current
sudo systemctl restart llm-orchestrator
```

## Environment Variables

If you need to pass environment variables to the application, edit the systemd service file:

```bash
sudo nano /etc/systemd/system/llm-orchestrator.service
```

Add an `Environment` variable or use `EnvironmentFile`:

```ini
Environment="SERVER_PORT=8080"
Environment="JAVA_OPTS=-Xmx512m -Xms256m"
EnvironmentFile=/etc/default/llm-orchestrator
```

Then reload and restart:

```bash
sudo systemctl daemon-reload
sudo systemctl restart llm-orchestrator
```

