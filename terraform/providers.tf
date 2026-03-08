terraform {
  required_version = ">= 1.7"

  required_providers {
    hcloud = {
      source  = "hetznercloud/hcloud"
      version = "~> 1.49"
    }
  }

  # Optional: store state remotely so the team can share it.
  # Comment this block out to use local state instead.
  # backend "s3" {
  #   bucket                      = "your-bucket-name"
  #   key                         = "tour-de-jug/terraform.tfstate"
  #   region                      = "eu-central-1"
  #   endpoint                    = "https://fsn1.your-objectstorage.com"  # Hetzner Object Storage
  #   skip_credentials_validation = true
  #   skip_metadata_api_check     = true
  #   skip_region_validation      = true
  #   force_path_style            = true
  # }
}

provider "hcloud" {
  token = var.hcloud_token
}
