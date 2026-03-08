variable "hcloud_token" {
  description = "Hetzner Cloud API token (read+write). Generate at console.hetzner.cloud → Project → Security → API Tokens."
  type        = string
  sensitive   = true
}

variable "server_name" {
  description = "Display name for the Hetzner server."
  type        = string
  default     = "tour-de-jug"
}

variable "server_type" {
  description = "Hetzner server type. cx22 (2 vCPU / 4 GB) handles the app comfortably. Scale to cx32 if needed."
  type        = string
  default     = "cx22"
  # Reference: https://www.hetzner.com/cloud/
  # cx22  — 2 vCPU / 4 GB  / 40 GB SSD  / ~5 €/mo
  # cx32  — 4 vCPU / 8 GB  / 80 GB SSD  / ~9 €/mo
  # cx42  — 8 vCPU / 16 GB / 160 GB SSD / ~17 €/mo
}

variable "server_image" {
  description = "OS image slug. ubuntu-24.04 is the LTS base used by the setup script."
  type        = string
  default     = "ubuntu-24.04"
}

variable "datacenter" {
  description = "Hetzner datacenter. nbg1 = Nuremberg, fsn1 = Falkenstein, hel1 = Helsinki, ash = Ashburn (US)."
  type        = string
  default     = "nbg1-dc3"
}

variable "ssh_public_key" {
  description = "SSH public key content (paste the one-liner starting with ssh-ed25519 or ssh-rsa). Added to the server at provisioning time."
  type        = string
}

variable "domain" {
  description = "Fully-qualified domain name that will point to the floating IP (e.g. tourdegjug.example.com). Used in cloud-init and as a label."
  type        = string
  default     = "tourdegjug.example.com"
}

variable "alert_email" {
  description = "Email address for Let's Encrypt certificate expiry alerts."
  type        = string
  default     = "ops@example.com"
}

variable "db_password" {
  description = "Postgres password written into /opt/tour-de-jug/.env on the server."
  type        = string
  sensitive   = true
}

variable "github_client_id" {
  description = "GitHub OAuth App client ID written into /opt/tour-de-jug/.env."
  type        = string
  sensitive   = true
}

variable "github_client_secret" {
  description = "GitHub OAuth App client secret written into /opt/tour-de-jug/.env."
  type        = string
  sensitive   = true
}
