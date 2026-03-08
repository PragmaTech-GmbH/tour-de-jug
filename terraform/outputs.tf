output "server_ipv4" {
  description = "Primary IPv4 of the server (changes if server is rebuilt — prefer floating_ip)."
  value       = hcloud_server.app.ipv4_address
}

output "floating_ip" {
  description = "Stable floating IPv4. Point your DNS A record here."
  value       = hcloud_floating_ip.app.ip_address
}

output "server_id" {
  description = "Hetzner internal server ID."
  value       = hcloud_server.app.id
}

output "next_steps" {
  description = "Human-readable checklist after apply."
  value = <<-EOT
    ✅ Infrastructure ready.

    1. Create a DNS A record:
         ${var.domain}  →  ${hcloud_floating_ip.app.ip_address}

    2. SSH in and verify cloud-init finished:
         ssh deploy@${hcloud_floating_ip.app.ip_address}
         sudo cloud-init status --wait

    3. Obtain a TLS certificate (run once, auto-renews via certbot container):
         ssh deploy@${hcloud_floating_ip.app.ip_address}
         cd /opt/tour-de-jug
         docker compose -f docker-compose.prod.yml run --rm certbot \
           certonly --webroot -w /var/www/certbot \
           -d ${var.domain} \
           --email ${var.alert_email} --agree-tos --no-eff-email

    4. Start the full stack:
         docker compose -f docker-compose.prod.yml up -d

    5. Add these GitHub secrets for the CD pipeline:
         VPS_HOST  = ${hcloud_floating_ip.app.ip_address}
         VPS_USER  = deploy
         VPS_SSH_KEY = <paste the matching private key>
  EOT
}
