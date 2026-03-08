#!/usr/bin/env bash
# One-time VPS setup script for a fresh Hetzner Ubuntu 24.04 server.
# Run as root: bash setup-vps.sh
set -euo pipefail

DEPLOY_USER="deploy"
APP_DIR="/opt/tour-de-jug"

echo "==> Updating system packages"
apt-get update -qq && apt-get upgrade -y -qq

echo "==> Installing Docker"
curl -fsSL https://get.docker.com | sh
systemctl enable --now docker

echo "==> Creating deploy user"
id "$DEPLOY_USER" &>/dev/null || useradd -m -s /bin/bash "$DEPLOY_USER"
usermod -aG docker "$DEPLOY_USER"

echo "==> Setting up SSH key for deploy user"
mkdir -p /home/$DEPLOY_USER/.ssh
# Paste the public key that matches your VPS_SSH_KEY secret here, or add it manually:
# echo "ssh-ed25519 AAAA... github-actions" >> /home/$DEPLOY_USER/.ssh/authorized_keys
chmod 700 /home/$DEPLOY_USER/.ssh
chmod 600 /home/$DEPLOY_USER/.ssh/authorized_keys 2>/dev/null || true
chown -R $DEPLOY_USER:$DEPLOY_USER /home/$DEPLOY_USER/.ssh

echo "==> Creating app directory"
mkdir -p "$APP_DIR/nginx"
chown -R $DEPLOY_USER:$DEPLOY_USER "$APP_DIR"

echo "==> Installing UFW firewall rules"
apt-get install -y -qq ufw
ufw default deny incoming
ufw default allow outgoing
ufw allow ssh
ufw allow http
ufw allow https
ufw --force enable

echo "==> Creating .env file (fill in secrets before first deploy)"
cat > "$APP_DIR/.env" <<'EOF'
DB_PASSWORD=change_me_now
GITHUB_CLIENT_ID=change_me_now
GITHUB_CLIENT_SECRET=change_me_now
EOF
chmod 600 "$APP_DIR/.env"
chown $DEPLOY_USER:$DEPLOY_USER "$APP_DIR/.env"

echo ""
echo "==> Setup complete!"
echo ""
echo "Next steps:"
echo "  1. Add your deploy SSH public key to /home/$DEPLOY_USER/.ssh/authorized_keys"
echo "  2. Fill in secrets in $APP_DIR/.env"
echo "  3. Copy nginx.conf to $APP_DIR/nginx/nginx.conf and update the domain name"
echo "  4. Obtain a TLS certificate:"
echo "     docker run --rm -v /etc/letsencrypt:/etc/letsencrypt \\"
echo "       -v /var/www/certbot:/var/www/certbot certbot/certbot certonly \\"
echo "       --webroot -w /var/www/certbot -d tourdegjug.example.com --email you@example.com --agree-tos"
echo "  5. Add these secrets to your GitHub repo:"
echo "     VPS_HOST, VPS_USER (=$DEPLOY_USER), VPS_SSH_KEY (private key)"
