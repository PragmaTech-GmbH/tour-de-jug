# Manual Deployment Guide — Tour de JUG on Hetzner VPS

This guide walks through provisioning the server with Terraform and then deploying
the application stack by hand. The CD pipeline in `.github/workflows/cd.yml`
automates steps 4–6 on every push to `main`.

---

## Prerequisites

| Tool | Install |
|---|---|
| Terraform ≥ 1.7 | `brew install terraform` |
| Hetzner account | console.hetzner.cloud |
| Docker (local) | docker.com — only needed to build/test images locally |
| An SSH key pair | `ssh-keygen -t ed25519 -C "hetzner-deploy"` |
| A domain name | Point the A record to the floating IP after step 2 |

---

## Step 1 — Create a Hetzner API token

1. Go to **console.hetzner.cloud** → your project → **Security → API Tokens**
2. Click **Generate API Token**, give it **Read & Write** permissions
3. Copy the token — it's shown only once

---

## Step 2 — Provision infra with Terraform

```bash
cd terraform

# Copy the example vars file and fill in your values
cp terraform.tfvars.example terraform.tfvars
$EDITOR terraform.tfvars

# Initialise providers
terraform init

# Preview what will be created (server, firewall, SSH key, floating IP)
terraform plan

# Apply — takes about 60 s
terraform apply
```

Terraform prints a `next_steps` output at the end with the exact commands to run,
including the floating IP to use for your DNS record.

> **Tip:** Pass sensitive values via environment variables instead of writing them
> in `terraform.tfvars`:
> ```bash
> export TF_VAR_hcloud_token="..."
> export TF_VAR_db_password="..."
> export TF_VAR_github_client_id="..."
> export TF_VAR_github_client_secret="..."
> terraform apply
> ```

---

## Step 3 — DNS

Create an **A record** pointing your domain to the floating IP:

```
tourdegjug.example.com  →  <floating IP from terraform output>
```

Propagation takes 1–60 minutes depending on your registrar's TTL.

---

## Step 4 — Verify cloud-init finished

cloud-init installs Docker, creates the `deploy` user, and writes `/opt/tour-de-jug/.env`.

```bash
ssh deploy@<floating-ip>

# Wait for cloud-init to complete (usually < 3 min after server boots)
sudo cloud-init status --wait

# Confirm Docker is running
docker ps
```

---

## Step 5 — Copy config files to the server

From your local machine (run from the repo root):

```bash
DEPLOY_HOST=<floating-ip>

# docker-compose.prod.yml
scp docker-compose.prod.yml deploy@$DEPLOY_HOST:/opt/tour-de-jug/

# nginx reverse-proxy config
scp deploy/nginx/nginx.conf deploy@$DEPLOY_HOST:/opt/tour-de-jug/nginx/nginx.conf
```

Then open the nginx config on the server and replace the placeholder domain:

```bash
ssh deploy@$DEPLOY_HOST
sed -i 's/tourdegjug.example.com/YOUR_ACTUAL_DOMAIN/g' /opt/tour-de-jug/nginx/nginx.conf
```

---

## Step 6 — Obtain a TLS certificate

nginx must be running (HTTP only) so certbot can complete the ACME challenge.
Start nginx first, then run certbot:

```bash
ssh deploy@$DEPLOY_HOST
cd /opt/tour-de-jug

# Start nginx in HTTP-only mode (it will 301 everything except /.well-known/)
docker compose -f docker-compose.prod.yml up -d nginx certbot

# Issue the certificate (runs inside the certbot container)
docker compose -f docker-compose.prod.yml run --rm certbot \
  certonly --webroot -w /var/www/certbot \
  -d YOUR_ACTUAL_DOMAIN \
  --email you@example.com \
  --agree-tos --no-eff-email
```

The certificate is written to `/etc/letsencrypt/` on the host and mounted into
the nginx container. Certbot's `sleep 12h` loop in the compose file handles
automatic renewal.

---

## Step 7 — Pull the Docker image and start the stack

The app image lives in GitHub Container Registry (GHCR). The CD pipeline pushes
it automatically, but you can also pull it manually:

```bash
ssh deploy@$DEPLOY_HOST
cd /opt/tour-de-jug

# Log in to GHCR (use a GitHub personal access token with read:packages scope)
echo $GITHUB_PAT | docker login ghcr.io -u YOUR_GITHUB_USERNAME --password-stdin

# Start the full stack (app + postgres + nginx + certbot)
docker compose -f docker-compose.prod.yml up -d

# Tail logs
docker compose -f docker-compose.prod.yml logs -f app
```

---

## Step 8 — Smoke test

```bash
# Health endpoint
curl https://YOUR_ACTUAL_DOMAIN/actuator/health
# → {"status":"UP"}

# Home page
curl -I https://YOUR_ACTUAL_DOMAIN/
# → HTTP/2 200
```

---

## Updating the app manually (between CD runs)

```bash
ssh deploy@<floating-ip>
cd /opt/tour-de-jug

docker compose -f docker-compose.prod.yml pull app
docker compose -f docker-compose.prod.yml up -d --no-deps app
docker image prune -f
```

---

## Useful commands on the server

```bash
# View all container status
docker compose -f /opt/tour-de-jug/docker-compose.prod.yml ps

# Follow app logs
docker compose -f /opt/tour-de-jug/docker-compose.prod.yml logs -f app

# Open a psql shell inside the DB container
docker compose -f /opt/tour-de-jug/docker-compose.prod.yml \
  exec db psql -U tourdegjug -d tourdegjug

# Reload nginx config without downtime
docker compose -f /opt/tour-de-jug/docker-compose.prod.yml \
  exec nginx nginx -s reload

# Force-renew TLS certificate (certbot renews automatically, but you can trigger manually)
docker compose -f /opt/tour-de-jug/docker-compose.prod.yml \
  run --rm certbot renew --force-renewal
```

---

## Tearing down

```bash
cd terraform

# Remove all Hetzner resources (server, floating IP, firewall, SSH key)
# Postgres data volume (if enabled) is also destroyed — back up first!
terraform destroy
```

---

## GitHub Secrets for the CD pipeline

After the server is running, add these to
**GitHub repo → Settings → Secrets and variables → Actions**:

| Secret | Value |
|---|---|
| `VPS_HOST` | Floating IP from `terraform output floating_ip` |
| `VPS_USER` | `deploy` |
| `VPS_SSH_KEY` | Contents of your private key (`cat ~/.ssh/id_ed25519`) |

The `GITHUB_TOKEN` secret is injected automatically — no action needed.
