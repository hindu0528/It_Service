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

# Import blocks to automatically import existing repositories into state
import {
  to = aws_ecr_repository.repos["discovery-server"]
  id = "discovery-server"
}
import {
  to = aws_ecr_repository.repos["config-server"]
  id = "config-server"
}
import {
  to = aws_ecr_repository.repos["api-gateway"]
  id = "api-gateway"
}
import {
  to = aws_ecr_repository.repos["auth-service"]
  id = "auth-service"
}
import {
  to = aws_ecr_repository.repos["ticket-service"]
  id = "ticket-service"
}
import {
  to = aws_ecr_repository.repos["attachment-service"]
  id = "attachment-service"
}
import {
  to = aws_ecr_repository.repos["notification-service"]
  id = "notification-service"
}
import {
  to = aws_ecr_repository.repos["ticketdesk-ui"]
  id = "ticketdesk-ui"
}