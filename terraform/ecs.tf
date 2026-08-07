data "aws_caller_identity" "current" {}
data "aws_region" "current" {}

locals {
  ecr_registry = "${data.aws_caller_identity.current.account_id}.dkr.ecr.${data.aws_region.current.name}.amazonaws.com"
}

# ECS Cluster
resource "aws_ecs_cluster" "main" {
  name = "ecs-fargate-cluster"
}

# Private DNS Namespace for Microservice Service Discovery
resource "aws_service_discovery_private_dns_namespace" "main" {
  name        = "it-support.local"
  description = "Internal service discovery namespace"
  vpc         = aws_vpc.main.id  # Changed from vpc_id to vpc
}

# ----------------- IAM ROLES -----------------

resource "aws_iam_role" "ecs_task_execution_role" {
  name = "ecs-task-execution-role"
  assume_role_policy = jsonencode({
    Version = "2012-10-17"
    Statement = [{
      Action    = "sts:AssumeRole"
      Effect    = "Allow"
      Principal = { Service = "ecs-tasks.amazonaws.com" }
    }]
  })
}

resource "aws_iam_role_policy_attachment" "ecs_task_execution_role_policy" {
  role       = aws_iam_role.ecs_task_execution_role.name
  policy_arn = "arn:aws:iam::aws:policy/service-role/AmazonECSTaskExecutionRolePolicy"
}

resource "aws_cloudwatch_log_group" "ecs_log_group" {
  name              = "/ecs/ecs-fargate-app"
  retention_in_days = 7
}

# ----------------- DISCOVERY SERVER (EUREKA) -----------------

resource "aws_service_discovery_service" "discovery_server" {
  name = "discovery-server"
  dns_config {
    namespace_id = aws_service_discovery_private_dns_namespace.main.id
    dns_records {
      ttl  = 10
      type = "A"
    }
    routing_policy = "MULTIVALUE"
  }
}

resource "aws_ecs_task_definition" "discovery" {
  family                   = "discovery-server"
  execution_role_arn       = aws_iam_role.ecs_task_execution_role.arn
  network_mode             = "awsvpc"
  requires_compatibilities = ["FARGATE"]
  cpu                      = "256"
  memory                   = "512"

  container_definitions = jsonencode([{
    name      = "discovery-server"
    image     = "${local.ecr_registry}/discovery-server:${var.image_tag}"
    essential = true
    portMappings = [{
      containerPort = 8761
      hostPort      = 8761
    }]
    environment = [
      { name = "EUREKA_INSTANCE_HOSTNAME", value = "discovery-server.it-support.local" },
      { name = "EUREKA_CLIENT_REGISTER_WITH_EUREKA", value = "false" },
      { name = "EUREKA_CLIENT_FETCH_REGISTRY", value = "false" },
      { name = "SPRING_CLOUD_CONFIG_URI", value = "http://config-server.it-support.local:8888" }
    ]
    logConfiguration = {
      logDriver = "awslogs"
      options = {
        "awslogs-group"         = aws_cloudwatch_log_group.ecs_log_group.name
        "awslogs-region"        = data.aws_region.current.name
        "awslogs-stream-prefix" = "discovery"
      }
    }
  }])
}

resource "aws_ecs_service" "discovery" {
  name            = "discovery-server"
  cluster         = aws_ecs_cluster.main.id
  task_definition = aws_ecs_task_definition.discovery.arn
  desired_count   = 1
  launch_type     = "FARGATE"

  network_configuration {
    subnets          = [aws_subnet.private_1.id, aws_subnet.private_2.id]
    security_groups  = [aws_security_group.ecs_tasks.id]
    assign_public_ip = false
  }

  load_balancer {
    target_group_arn = aws_alb_target_group.discovery.arn
    container_name   = "discovery-server"
    container_port   = 8761
  }

  service_registries {
    registry_arn = aws_service_discovery_service.discovery_server.arn
  }

  depends_on = [aws_alb_listener.discovery]
}

# ----------------- CONFIG SERVER -----------------

resource "aws_service_discovery_service" "config_server" {
  name = "config-server"
  dns_config {
    namespace_id = aws_service_discovery_private_dns_namespace.main.id
    dns_records {
      ttl  = 10
      type = "A"
    }
    routing_policy = "MULTIVALUE"
  }
}

resource "aws_ecs_task_definition" "config" {
  family                   = "config-server"
  execution_role_arn       = aws_iam_role.ecs_task_execution_role.arn
  network_mode             = "awsvpc"
  requires_compatibilities = ["FARGATE"]
  cpu                      = "256"
  memory                   = "512"

  container_definitions = jsonencode([{
    name      = "config-server"
    image     = "${local.ecr_registry}/config-server:${var.image_tag}"
    essential = true
    portMappings = [{
      containerPort = 8888
      hostPort      = 8888
    }]
    environment = [
      { name = "SPRING_PROFILES_ACTIVE", value = "native" },
      { name = "EUREKA_CLIENT_SERVICEURL_DEFAULTZONE", value = "http://discovery-server.it-support.local:8761/eureka/" }
    ]
    logConfiguration = {
      logDriver = "awslogs"
      options = {
        "awslogs-group"         = aws_cloudwatch_log_group.ecs_log_group.name
        "awslogs-region"        = data.aws_region.current.name
        "awslogs-stream-prefix" = "config"
      }
    }
  }])
}

resource "aws_ecs_service" "config" {
  name            = "config-server"
  cluster         = aws_ecs_cluster.main.id
  task_definition = aws_ecs_task_definition.config.arn
  desired_count   = 1
  launch_type     = "FARGATE"

  network_configuration {
    subnets          = [aws_subnet.private_1.id, aws_subnet.private_2.id]
    security_groups  = [aws_security_group.ecs_tasks.id]
    assign_public_ip = false
  }

  load_balancer {
    target_group_arn = aws_alb_target_group.config.arn
    container_name   = "config-server"
    container_port   = 8888
  }

  service_registries {
    registry_arn = aws_service_discovery_service.config_server.arn
  }

  depends_on = [aws_alb_listener.config]
}