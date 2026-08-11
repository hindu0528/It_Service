# ECS Cluster
resource "aws_ecs_cluster" "main" {
  name = "ecs-fargate-cluster"
}

# Private DNS Namespace for service discovery
resource "aws_service_discovery_private_dns_namespace" "main" {
  name        = "it-support.local"
  description = "Internal service discovery namespace"
  vpc         = aws_vpc.main.id
}

# ----------------- IAM ROLES -----------------

# Execution Role (for pulling images, logs, injected secrets)
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

# IAM Policy for secrets retrieval
resource "aws_iam_role_policy" "ecs_execution_secrets" {
  name = "ecs-execution-secrets-policy"
  role = aws_iam_role.ecs_task_execution_role.id

  policy = jsonencode({
    Version = "2012-10-17"
    Statement = [
      {
        Effect = "Allow"
        Action = [
          "secretsmanager:GetSecretValue",
          "ssm:GetParameters",
          "ssm:GetParameter"
        ]
        Resource = [
          aws_secretsmanager_secret.db_password.arn,
          aws_ssm_parameter.db_username.arn,
          aws_ssm_parameter.db_url_ticket.arn,
          aws_ssm_parameter.db_url_auth.arn,
          aws_ssm_parameter.db_url_attachment.arn
        ]
      }
    ]
  })
}

# Task Role (for runtime AWS access like S3 uploads)
resource "aws_iam_role" "ecs_task_role" {
  name = "ecs-task-role"
  assume_role_policy = jsonencode({
    Version = "2012-10-17"
    Statement = [{
      Action    = "sts:AssumeRole"
      Effect    = "Allow"
      Principal = { Service = "ecs-tasks.amazonaws.com" }
    }]
  })
}

resource "aws_iam_role_policy_attachment" "ecs_task_s3_policy" {
  role       = aws_iam_role.ecs_task_role.name
  policy_arn = "arn:aws:iam::aws:policy/AmazonS3FullAccess"
}

# Logs
resource "aws_cloudwatch_log_group" "ecs_log_group" {
  name              = "/ecs/ecs-fargate-app"
  retention_in_days = 7 # Complies with Observability retention requirement (M7)
}

# ----------------- SERVICE DISCOVERY & ECS SERVICES -----------------

# 1. Discovery Server (Eureka)
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
  cpu                      = "512"
  memory                   = "1024"

  container_definitions = jsonencode([{
    name      = "discovery-server"
    image     = "${local.ecr_registry}/discovery-server:${var.image_tag}"
    essential = true
    portMappings = [{ containerPort = 8761, hostPort = 8761 }]
    environment = [
      { name = "EUREKA_INSTANCE_HOSTNAME", value = "discovery-server.it-support.local" },
      { name = "EUREKA_CLIENT_REGISTER_WITH_EUREKA", value = "false" },
      { name = "EUREKA_CLIENT_FETCH_REGISTRY", value = "false" }
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
    subnets         = [aws_subnet.private_1.id, aws_subnet.private_2.id]
    security_groups = [aws_security_group.ecs_tasks.id]
  }

  load_balancer {
    target_group_arn = aws_alb_target_group.discovery.arn
    container_name   = "discovery-server"
    container_port   = 8761
  }

  service_registries { registry_arn = aws_service_discovery_service.discovery_server.arn }
  depends_on         = [aws_alb_listener.discovery]
}

# 2. Config Server
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
    portMappings = [{ containerPort = 8888, hostPort = 8888 }]
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
    subnets         = [aws_subnet.private_1.id, aws_subnet.private_2.id]
    security_groups = [aws_security_group.ecs_tasks.id]
  }

  load_balancer {
    target_group_arn = aws_alb_target_group.config.arn
    container_name   = "config-server"
    container_port   = 8888
  }

  service_registries { registry_arn = aws_service_discovery_service.config_server.arn }
  depends_on         = [aws_alb_listener.config]
}

# 3. API Gateway
resource "aws_service_discovery_service" "api_gateway" {
  name = "api-gateway"
  dns_config {
    namespace_id = aws_service_discovery_private_dns_namespace.main.id
    dns_records {
      ttl  = 10
      type = "A"
    }
    routing_policy = "MULTIVALUE"
  }
}

resource "aws_ecs_task_definition" "gateway" {
  family                   = "api-gateway"
  execution_role_arn       = aws_iam_role.ecs_task_execution_role.arn
  network_mode             = "awsvpc"
  requires_compatibilities = ["FARGATE"]
  cpu                      = "256"
  memory                   = "512"

  container_definitions = jsonencode([{
    name      = "api-gateway"
    image     = "${local.ecr_registry}/api-gateway:${var.image_tag}"
    essential = true
    portMappings = [{ containerPort = 8080, hostPort = 8080 }]
    environment = [
      { name = "SPRING_CLOUD_CONFIG_URI", value = "http://config-server.it-support.local:8888" },
      { name = "EUREKA_CLIENT_SERVICEURL_DEFAULTZONE", value = "http://discovery-server.it-support.local:8761/eureka/" },
      { name = "SPRING_CLOUD_INETUTILS_PREFERRED_NETWORKS", value = "10.0." }
    ]
    logConfiguration = {
      logDriver = "awslogs"
      options = {
        "awslogs-group"         = aws_cloudwatch_log_group.ecs_log_group.name
        "awslogs-region"        = data.aws_region.current.name
        "awslogs-stream-prefix" = "gateway"
      }
    }
  }])
}

resource "aws_ecs_service" "gateway" {
  name            = "api-gateway"
  cluster         = aws_ecs_cluster.main.id
  task_definition = aws_ecs_task_definition.gateway.arn
  desired_count   = 1
  launch_type     = "FARGATE"

  network_configuration {
    subnets         = [aws_subnet.private_1.id, aws_subnet.private_2.id]
    security_groups = [aws_security_group.ecs_tasks.id]
  }

  load_balancer {
    target_group_arn = aws_alb_target_group.gateway.arn
    container_name   = "api-gateway"
    container_port   = 8080
  }

  service_registries { registry_arn = aws_service_discovery_service.api_gateway.arn }
  depends_on         = [aws_alb_listener.front_end, aws_ecs_service.config]
}

# 4. Auth Service
resource "aws_service_discovery_service" "auth_service" {
  name = "auth-service"
  dns_config {
    namespace_id = aws_service_discovery_private_dns_namespace.main.id
    dns_records {
      ttl  = 10
      type = "A"
    }
    routing_policy = "MULTIVALUE"
  }
}

resource "aws_ecs_task_definition" "auth" {
  family                   = "auth-service"
  execution_role_arn       = aws_iam_role.ecs_task_execution_role.arn
  network_mode             = "awsvpc"
  requires_compatibilities = ["FARGATE"]
  cpu                      = "256"
  memory                   = "512"

  container_definitions = jsonencode([{
    name      = "auth-service"
    image     = "${local.ecr_registry}/auth-service:${var.image_tag}"
    essential = true
    portMappings = [{ containerPort = 8085, hostPort = 8085 }]
    environment = [
      { name = "SPRING_PROFILES_ACTIVE", value = "dev" },
      { name = "SPRING_CLOUD_CONFIG_URI", value = "http://config-server.it-support.local:8888" },
      { name = "EUREKA_CLIENT_SERVICEURL_DEFAULTZONE", value = "http://discovery-server.it-support.local:8761/eureka/" },
      { name = "SPRING_JPA_PROPERTIES_HIBERNATE_DIALECT", value = "org.hibernate.dialect.MySQLDialect" },
      { name = "SPRING_JPA_DATABASE_PLATFORM", value = "org.hibernate.dialect.MySQLDialect" },
      { name = "SPRING_CLOUD_INETUTILS_PREFERRED_NETWORKS", value = "10.0." }
    ]
    secrets = [
      { name = "DB_PASSWORD", valueFrom = aws_secretsmanager_secret.db_password.arn },
      { name = "DB_USERNAME", valueFrom = aws_ssm_parameter.db_username.arn },
      { name = "SPRING_DATASOURCE_URL", valueFrom = aws_ssm_parameter.db_url_auth.arn }
    ]
    logConfiguration = {
      logDriver = "awslogs"
      options = {
        "awslogs-group"         = aws_cloudwatch_log_group.ecs_log_group.name
        "awslogs-region"        = data.aws_region.current.name
        "awslogs-stream-prefix" = "auth"
      }
    }
  }])
}

resource "aws_ecs_service" "auth" {
  name            = "auth-service"
  cluster         = aws_ecs_cluster.main.id
  task_definition = aws_ecs_task_definition.auth.arn
  desired_count   = 1
  launch_type     = "FARGATE"

  network_configuration {
    subnets         = [aws_subnet.private_1.id, aws_subnet.private_2.id]
    security_groups = [aws_security_group.ecs_tasks.id]
  }

  service_registries { registry_arn = aws_service_discovery_service.auth_service.arn }
  depends_on         = [aws_db_instance.main, aws_ecs_service.config]
}

# 5. Ticket Service (Milestone 3 Core)
resource "aws_service_discovery_service" "ticket" {
  name = "ticket-service"
  dns_config {
    namespace_id = aws_service_discovery_private_dns_namespace.main.id
    dns_records {
      ttl  = 10
      type = "A"
    }
    routing_policy = "MULTIVALUE"
  }
}

resource "aws_ecs_task_definition" "ticket" {
  family                   = "ticket-service"
  execution_role_arn       = aws_iam_role.ecs_task_execution_role.arn
  task_role_arn            = aws_iam_role.ecs_task_role.arn
  network_mode             = "awsvpc"
  requires_compatibilities = ["FARGATE"]
  cpu                      = "256"
  memory                   = "512"

  container_definitions = jsonencode([{
    name      = "ticket-service"
    image     = "${local.ecr_registry}/ticket-service:${var.image_tag}"
    essential = true
    portMappings = [{ containerPort = 8082, hostPort = 8082 }]
    environment = [
      { name = "SPRING_PROFILES_ACTIVE", value = "dev" },
      { name = "SPRING_CLOUD_CONFIG_URI", value = "http://config-server.it-support.local:8888" },
      { name = "EUREKA_CLIENT_SERVICEURL_DEFAULTZONE", value = "http://discovery-server.it-support.local:8761/eureka/" },
      { name = "SPRING_JPA_PROPERTIES_HIBERNATE_DIALECT", value = "org.hibernate.dialect.MySQLDialect" },
      { name = "SPRING_JPA_DATABASE_PLATFORM", value = "org.hibernate.dialect.MySQLDialect" },
      { name = "SPRING_CLOUD_INETUTILS_PREFERRED_NETWORKS", value = "10.0." }
    ]
    secrets = [
      { name = "DB_PASSWORD", valueFrom = aws_secretsmanager_secret.db_password.arn },
      { name = "DB_USERNAME", valueFrom = aws_ssm_parameter.db_username.arn },
      { name = "SPRING_DATASOURCE_URL", valueFrom = aws_ssm_parameter.db_url_ticket.arn }
    ]
    logConfiguration = {
      logDriver = "awslogs"
      options = {
        "awslogs-group"         = aws_cloudwatch_log_group.ecs_log_group.name
        "awslogs-region"        = data.aws_region.current.name
        "awslogs-stream-prefix" = "ticket"
      }
    }
  }])
}

resource "aws_ecs_service" "ticket" {
  name            = "ticket-service"
  cluster         = aws_ecs_cluster.main.id
  task_definition = aws_ecs_task_definition.ticket.arn
  desired_count   = 1
  launch_type     = "FARGATE"

  network_configuration {
    subnets         = [aws_subnet.private_1.id, aws_subnet.private_2.id]
    security_groups = [aws_security_group.ecs_tasks.id]
  }

  service_registries { registry_arn = aws_service_discovery_service.ticket.arn }
  depends_on         = [aws_db_instance.main, aws_ecs_service.config]
}

# 6. Attachment Service
resource "aws_service_discovery_service" "attachment" {
  name = "attachment-service"
  dns_config {
    namespace_id = aws_service_discovery_private_dns_namespace.main.id
    dns_records {
      ttl  = 10
      type = "A"
    }
    routing_policy = "MULTIVALUE"
  }
}

resource "aws_ecs_task_definition" "attachment" {
  family                   = "attachment-service"
  execution_role_arn       = aws_iam_role.ecs_task_execution_role.arn
  task_role_arn            = aws_iam_role.ecs_task_role.arn
  network_mode             = "awsvpc"
  requires_compatibilities = ["FARGATE"]
  cpu                      = "256"
  memory                   = "512"

  container_definitions = jsonencode([{
    name      = "attachment-service"
    image     = "${local.ecr_registry}/attachment-service:${var.image_tag}"
    essential = true
    portMappings = [{ containerPort = 8084, hostPort = 8084 }]
    environment = [
      { name = "SPRING_PROFILES_ACTIVE", value = "dev" },
      { name = "SPRING_CLOUD_CONFIG_URI", value = "http://config-server.it-support.local:8888" },
      { name = "EUREKA_CLIENT_SERVICEURL_DEFAULTZONE", value = "http://discovery-server.it-support.local:8761/eureka/" },
      { name = "SPRING_JPA_PROPERTIES_HIBERNATE_DIALECT", value = "org.hibernate.dialect.MySQLDialect" },
      { name = "SPRING_JPA_DATABASE_PLATFORM", value = "org.hibernate.dialect.MySQLDialect" },
      { name = "SPRING_CLOUD_INETUTILS_PREFERRED_NETWORKS", value = "10.0." }
    ]
    secrets = [
      { name = "DB_PASSWORD", valueFrom = aws_secretsmanager_secret.db_password.arn },
      { name = "DB_USERNAME", valueFrom = aws_ssm_parameter.db_username.arn },
      { name = "SPRING_DATASOURCE_URL", valueFrom = aws_ssm_parameter.db_url_attachment.arn }
    ]
    logConfiguration = {
      logDriver = "awslogs"
      options = {
        "awslogs-group"         = aws_cloudwatch_log_group.ecs_log_group.name
        "awslogs-region"        = data.aws_region.current.name
        "awslogs-stream-prefix" = "attachment"
      }
    }
  }])
}

resource "aws_ecs_service" "attachment" {
  name            = "attachment-service"
  cluster         = aws_ecs_cluster.main.id
  task_definition = aws_ecs_task_definition.attachment.arn
  desired_count   = 1
  launch_type     = "FARGATE"

  network_configuration {
    subnets         = [aws_subnet.private_1.id, aws_subnet.private_2.id]
    security_groups = [aws_security_group.ecs_tasks.id]
  }

  service_registries { registry_arn = aws_service_discovery_service.attachment.arn }
  depends_on         = [aws_db_instance.main, aws_ecs_service.config]
}

# 7. Notification Service
resource "aws_service_discovery_service" "notification" {
  name = "notification-service"
  dns_config {
    namespace_id = aws_service_discovery_private_dns_namespace.main.id
    dns_records {
      ttl  = 10
      type = "A"
    }
    routing_policy = "MULTIVALUE"
  }
}

resource "aws_ecs_task_definition" "notification" {
  family                   = "notification-service"
  execution_role_arn       = aws_iam_role.ecs_task_execution_role.arn
  network_mode             = "awsvpc"
  requires_compatibilities = ["FARGATE"]
  cpu                      = "256"
  memory                   = "512"

  container_definitions = jsonencode([{
    name      = "notification-service"
    image     = "${local.ecr_registry}/notification-service:${var.image_tag}"
    essential = true
    portMappings = [{ containerPort = 8083, hostPort = 8083 }]
    environment = [
      { name = "SPRING_CLOUD_CONFIG_URI", value = "http://config-server.it-support.local:8888" },
      { name = "EUREKA_CLIENT_SERVICEURL_DEFAULTZONE", value = "http://discovery-server.it-support.local:8761/eureka/" },
      { name = "SPRING_CLOUD_INETUTILS_PREFERRED_NETWORKS", value = "10.0." }
    ]
    logConfiguration = {
      logDriver = "awslogs"
      options = {
        "awslogs-group"         = aws_cloudwatch_log_group.ecs_log_group.name
        "awslogs-region"        = data.aws_region.current.name
        "awslogs-stream-prefix" = "notification"
      }
    }
  }])
}

resource "aws_ecs_service" "notification" {
  name            = "notification-service"
  cluster         = aws_ecs_cluster.main.id
  task_definition = aws_ecs_task_definition.notification.arn
  desired_count   = 1
  launch_type     = "FARGATE"

  network_configuration {
    subnets         = [aws_subnet.private_1.id, aws_subnet.private_2.id]
    security_groups = [aws_security_group.ecs_tasks.id]
  }

  service_registries { registry_arn = aws_service_discovery_service.notification.arn }
  depends_on         = [aws_ecs_service.config]
}