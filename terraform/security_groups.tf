# ALB Security Group (Public-facing)
resource "aws_security_group" "alb" {
  name_prefix = "ecs-fargate-alb-sg-"
  description = "Access to the load balancer from the internet"
  vpc_id      = aws_vpc.main.id

  # Port 80 (Frontend UI / Gateway)
  ingress {
    from_port   = 80
    to_port     = 80
    protocol    = "tcp"
    cidr_blocks = ["0.0.0.0/0"]
  }

  # Port 8761 (Discovery Server UI)
  ingress {
    from_port   = 8761
    to_port     = 8761
    protocol    = "tcp"
    cidr_blocks = ["0.0.0.0/0"]
  }

  # Port 8888 (Config Server API)
  ingress {
    from_port   = 8888
    to_port     = 8888
    protocol    = "tcp"
    cidr_blocks = ["0.0.0.0/0"]
  }

  egress {
    from_port   = 0
    to_port     = 0
    protocol    = "-1"
    cidr_blocks = ["0.0.0.0/0"]
  }

  tags = {
    Name = "ecs-fargate-alb-sg"
  }
}

# ECS Tasks Security Group (Private)
resource "aws_security_group" "ecs_tasks" {
  name_prefix = "ecs-fargate-tasks-sg-"
  description = "Access to the ECS tasks from the ALB only"
  vpc_id      = aws_vpc.main.id

  # Allow inbound traffic on any port from the ALB Security Group
  ingress {
    from_port       = 0
    to_port         = 65535
    protocol        = "tcp"
    security_groups = [aws_security_group.alb.id]
  }

  # Allow all internal microservice communication
  ingress {
    from_port = 0
    to_port   = 0
    protocol  = "-1"
    self      = true
  }

  egress {
    from_port   = 0
    to_port     = 0
    protocol    = "-1"
    cidr_blocks = ["0.0.0.0/0"]
  }

  tags = {
    Name = "ecs-fargate-tasks-sg"
  }
}

# Database Security Group (RDS - Port 3306)
resource "aws_security_group" "db_sg" {
  name_prefix = "ecs-fargate-db-sg-"
  vpc_id      = aws_vpc.main.id

  ingress {
    from_port       = 3306
    to_port         = 3306
    protocol        = "tcp"
    security_groups = [aws_security_group.ecs_tasks.id]
  }

  egress {
    from_port   = 0
    to_port     = 0
    protocol    = "-1"
    cidr_blocks = ["0.0.0.0/0"]
  }

  tags = {
    Name = "ecs-fargate-db-sg"
  }
}

output "alb_security_group_id" {
  value       = aws_security_group.alb.id
}

output "ecs_tasks_security_group_id" {
  value       = aws_security_group.ecs_tasks.id
}