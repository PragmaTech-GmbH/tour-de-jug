# ── SSH key ───────────────────────────────────────────────────────────────────
resource "hcloud_ssh_key" "deploy" {
  name       = "${var.server_name}-deploy"
  public_key = var.ssh_public_key
}

# ── Firewall ──────────────────────────────────────────────────────────────────
resource "hcloud_firewall" "app" {
  name = "${var.server_name}-firewall"

  # Inbound
  rule {
    direction = "in"
    protocol  = "tcp"
    port      = "22"
    source_ips = ["0.0.0.0/0", "::/0"]
    description = "SSH"
  }

  rule {
    direction = "in"
    protocol  = "tcp"
    port      = "80"
    source_ips = ["0.0.0.0/0", "::/0"]
    description = "HTTP (nginx → redirect to HTTPS)"
  }

  rule {
    direction = "in"
    protocol  = "tcp"
    port      = "443"
    source_ips = ["0.0.0.0/0", "::/0"]
    description = "HTTPS"
  }

  # ICMP — useful for ping/traceroute debugging
  rule {
    direction = "in"
    protocol  = "icmp"
    source_ips = ["0.0.0.0/0", "::/0"]
  }

  # All outbound is allowed by default; no explicit egress rules needed.
}

# ── Cloud-init user-data ──────────────────────────────────────────────────────
# Runs once on first boot. Installs Docker, creates deploy user,
# writes the .env file, and sets up the directory layout.
locals {
  cloud_init = templatefile("${path.module}/cloud-init.yaml.tpl", {
    db_password          = var.db_password
    github_client_id     = var.github_client_id
    github_client_secret = var.github_client_secret
    domain               = var.domain
    alert_email          = var.alert_email
  })
}

# ── Server ────────────────────────────────────────────────────────────────────
resource "hcloud_server" "app" {
  name        = var.server_name
  server_type = var.server_type
  image       = var.server_image
  datacenter  = var.datacenter

  ssh_keys    = [hcloud_ssh_key.deploy.id]
  user_data   = local.cloud_init

  firewall_ids = [hcloud_firewall.app.id]

  # Delete protection prevents accidental destruction via `terraform destroy`.
  # Set to false when you intentionally want to tear down the server.
  delete_protection  = false
  rebuild_protection = false

  labels = {
    app    = "tour-de-jug"
    env    = "production"
    domain = var.domain
  }

  lifecycle {
    # Prevent Terraform from recreating the server if only the user_data changes
    # after the initial provision (cloud-init only runs once anyway).
    ignore_changes = [user_data]
  }
}

# ── Floating IP ───────────────────────────────────────────────────────────────
# A floating IP survives server rebuilds/replacements. Point your DNS A record
# here once and never update it again.
resource "hcloud_floating_ip" "app" {
  name          = "${var.server_name}-ip"
  type          = "ipv4"
  home_location = split("-", var.datacenter)[0]   # e.g. "nbg1" from "nbg1-dc3"
  description   = "Stable public IP for tour-de-jug"

  labels = {
    app = "tour-de-jug"
  }
}

resource "hcloud_floating_ip_assignment" "app" {
  floating_ip_id = hcloud_floating_ip.app.id
  server_id      = hcloud_server.app.id
}

# ── Volume (optional persistent data disk) ───────────────────────────────────
# Useful if you want Postgres data to outlive the server. Uncomment to enable.
# resource "hcloud_volume" "postgres_data" {
#   name      = "${var.server_name}-postgres"
#   size      = 20   # GB
#   location  = split("-", var.datacenter)[0]
#   format    = "ext4"
#   automount = false
#
#   labels = { app = "tour-de-jug" }
# }
#
# resource "hcloud_volume_attachment" "postgres_data" {
#   volume_id = hcloud_volume.postgres_data.id
#   server_id = hcloud_server.app.id
#   automount = true
# }
