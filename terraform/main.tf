terraform {
  required_version = ">= 1.0.0"
  required_providers {
    aws = {
      source  = "hashicorp/aws"
      version = "~> 5.0"
    }
    random = {
      source  = "hashicorp/random"
      version = "~> 3.0"
    }
    archive = {
      source  = "hashicorp/archive"
      version = "~> 2.0"
    }
  }
}

provider "aws" {
  region = var.aws_region
  
  # Automated Tagging (Milestone 8)
  default_tags {
    tags = {
      Project     = "It_Support_System"
      Owner       = "Sravan"
      Environment = "Dev"
      CostCenter  = "10001"
    }
  }
}

variable "aws_region" {
  type    = string
  default = "ap-south-2" # Set to ap-south-2 (Hyderabad) based on user environment
}

variable "image_tag" {
  type    = string
  default = "latest"
}

data "aws_caller_identity" "current" {}
data "aws_region" "current" {}

locals {
  ecr_registry = "${data.aws_caller_identity.current.account_id}.dkr.ecr.${data.aws_region.current.name}.amazonaws.com"
}

output "alb_hostname" {
  value       = aws_alb.main.dns_name
  description = "The public DNS name of the ALB"
}

output "cloudfront_domain_name" {
  value       = aws_s3_bucket_website_configuration.frontend.website_endpoint
  description = "The website hosting endpoint of the S3 bucket"
}

output "s3_bucket_name" {
  value       = aws_s3_bucket.frontend_bucket.id
  description = "Name of S3 bucket hosting frontend static assets"
}