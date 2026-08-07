# ALB Security Group (Public-facing)
resource "aws_security_group" "alb" {
  name_prefix = "ecs-fargate-alb-sg-" # Generates unique name
  description = "Access to the load balancer from the internet"
  vpc_id      = aws_vpc.main.id

  ingress {
    from_port   = 80
    to_port     = 80
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
  name_prefix = "ecs-fargate-tasks-sg-" # Generates unique name
  description = "Access to the ECS tasks from the ALB only"
  vpc_id      = aws_vpc.main.id

  ingress {
    from_port       = 80
    to_port         = 80
    protocol        = "tcp"
    security_groups = [aws_security_group.alb.id]
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

# Outputs for Security Groups
output "alb_security_group_id" {
  value       = aws_security_group.alb.id
  description = "ID of the ALB security group"
}

output "ecs_tasks_security_group_id" {
  value       = aws_security_group.ecs_tasks.id
  description = "ID of the ECS tasks security group"
}