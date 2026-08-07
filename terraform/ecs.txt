# Fetch current AWS account ID and region
data "aws_caller_identity" "current" {}
data "aws_region" "current" {}

locals {
  # Construct ECR image URL using the Git SHA tag passed by GitHub Actions
  ecr_registry = "${data.aws_caller_identity.current.account_id}.dkr.ecr.${data.aws_region.current.name}.amazonaws.com"
  app_image    = "${local.ecr_registry}/ticketdesk-ui:${var.image_tag}"
}

# ECS Cluster
resource "aws_ecs_cluster" "main" {
  name = "ecs-fargate-cluster"

  tags = {
    Name = "ecs-fargate-cluster"
  }
}

# IAM Role: ECS Task Execution Role (used by Fargate agent to pull images and write logs)
resource "aws_iam_role" "ecs_task_execution_role" {
  name = "ecs-task-execution-role"

  assume_role_policy = jsonencode({
    Version = "2012-10-17"
    Statement = [
      {
        Action = "sts:AssumeRole"
        Effect = "Allow"
        Principal = {
          Service = "ecs-tasks.amazonaws.com"
        }
      }
    ]
  })
}

# Attach standard ECS task execution policy to IAM role
resource "aws_iam_role_policy_attachment" "ecs_task_execution_role_policy" {
  role       = aws_iam_role.ecs_task_execution_role.name
  policy_arn = "arn:aws:iam::aws:policy/service-role/AmazonECSTaskExecutionRolePolicy"
}

# CloudWatch Log Group for container logs
resource "aws_cloudwatch_log_group" "ecs_log_group" {
  name              = "/ecs/ecs-fargate-app"
  retention_in_days = 7

  tags = {
    Name = "ecs-fargate-log-group"
  }
}

# ECS Task Definition
resource "aws_ecs_task_definition" "app" {
  family                   = "ecs-fargate-task"
  execution_role_arn       = aws_iam_role.ecs_task_execution_role.arn
  network_mode             = "awsvpc" # Required for Fargate
  requires_compatibilities = ["FARGATE"]
  cpu                      = "256"    # 0.25 vCPU
  memory                   = "512"    # 0.5 GB RAM

  container_definitions = jsonencode([
    {
      name      = "app"
      image     = local.app_image
      essential = true
      portMappings = [
        {
          containerPort = 80
          hostPort      = 80
        }
      ]
      logConfiguration = {
        logDriver = "awslogs"
        options = {
          "awslogs-group"         = aws_cloudwatch_log_group.ecs_log_group.name
          "awslogs-region"        = data.aws_region.current.name
          "awslogs-stream-prefix" = "ecs"
        }
      }
    }
  ])
}

# ECS Service
resource "aws_ecs_service" "main" {
  name            = "ecs-fargate-service"
  cluster         = aws_ecs_cluster.main.id
  task_definition = aws_ecs_task_definition.app.arn
  desired_count   = 1
  launch_type     = "FARGATE"

  network_configuration {
    # CRITICAL: Containers run inside the PRIVATE subnets
    subnets          = [aws_subnet.private_1.id, aws_subnet.private_2.id]
    security_groups  = [aws_security_group.ecs_tasks.id]
    assign_public_ip = false # Private subnet tasks do not get public IPs
  }

  load_balancer {
    target_group_arn = aws_alb_target_group.app.arn
    container_name   = "app"
    container_port   = 80
  }

  # Ensure the ALB listener is active before creating the service
  depends_on = [aws_alb_listener.front_end]

  tags = {
    Name = "ecs-fargate-service"
  }
}