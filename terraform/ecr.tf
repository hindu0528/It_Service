# Define all microservice ECR repositories in a clean loop
resource "aws_ecr_repository" "repos" {
  for_each             = toset([
    "discovery-server",
    "config-server",
    "api-gateway",
    "auth-service",
    "ticket-service",
    "attachment-service",
    "notification-service",
    "ticketdesk-ui"
  ])
  name                 = each.value
  image_tag_mutability = "MUTABLE"
  force_delete         = true

  image_scanning_configuration {
    scan_on_push = true
  }

  tags = {
    Environment = "Dev"
  }
}