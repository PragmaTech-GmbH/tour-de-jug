#cloud-config
# Runs once on first boot. Idempotent — safe to re-run manually if needed.

package_update: true
package_upgrade: true

packages:
  - curl
  - ufw
  - git

runcmd:
  # ── Docker ──────────────────────────────────────────────────────────────────
  - curl -fsSL https://get.docker.com | sh
  - systemctl enable --now docker

  # ── deploy user ─────────────────────────────────────────────────────────────
  # The SSH key is already injected by Hetzner via the hcloud_ssh_key resource.
  # We only need to create the user and grant Docker access.
  - useradd -m -s /bin/bash deploy || true
  - usermod -aG docker deploy

  # Copy root's authorized_keys so the deploy user can SSH in with the same key.
  - mkdir -p /home/deploy/.ssh
  - cp /root/.ssh/authorized_keys /home/deploy/.ssh/authorized_keys
  - chown -R deploy:deploy /home/deploy/.ssh
  - chmod 700 /home/deploy/.ssh
  - chmod 600 /home/deploy/.ssh/authorized_keys

  # ── App directory ────────────────────────────────────────────────────────────
  - mkdir -p /opt/tour-de-jug/nginx
  - chown -R deploy:deploy /opt/tour-de-jug

  # ── .env file ────────────────────────────────────────────────────────────────
  - |
    cat > /opt/tour-de-jug/.env <<'ENVEOF'
    DB_PASSWORD=${db_password}
    GITHUB_CLIENT_ID=${github_client_id}
    GITHUB_CLIENT_SECRET=${github_client_secret}
    ENVEOF
  - chmod 600 /opt/tour-de-jug/.env
  - chown deploy:deploy /opt/tour-de-jug/.env

  # ── UFW firewall ─────────────────────────────────────────────────────────────
  - ufw default deny incoming
  - ufw default allow outgoing
  - ufw allow ssh
  - ufw allow http
  - ufw allow https
  - ufw --force enable

final_message: |
  cloud-init finished in $UPTIME seconds.
  Server is ready for tour-de-jug deployment.
  Domain: ${domain}
