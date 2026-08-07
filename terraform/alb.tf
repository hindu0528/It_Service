# Application Load Balancer
resource "aws_alb" "main" {
  name            = "ecs-fargate-alb"
  subnets         = [aws_subnet.public_1.id, aws_subnet.public_2.id]
  security_groups = [aws_security_group.alb.id]

  tags = {
    Name = "ecs-fargate-alb"
  }
}

# Target Group for ECS Fargate Tasks
resource "aws_alb_target_group" "app" {
  name        = "ecs-fargate-tg"
  port        = 80
  protocol    = "HTTP"
  vpc_id      = aws_vpc.main.id
  target_type = "ip" # Required for Fargate (awsvpc network mode)

  health_check {
    healthy_threshold   = "3"
    interval            = "30"
    protocol            = "HTTP"
    matcher             = "200"
    timeout             = "3"
    path                = "/"
    unhealthy_threshold = "2"
  }

  tags = {
    Name = "ecs-fargate-tg"
  }
}

# HTTP Listener routing traffic from ALB to Target Group
resource "aws_alb_listener" "front_end" {
  load_balancer_arn = aws_alb.main.arn
  port              = 80
  protocol          = "HTTP"

  default_action {
    target_group_arn = aws_alb_target_group.app.arn
    type             = "forward"
  }
}

# Output the Load Balancer DNS Name (This will be your working URL)
output "alb_hostname" {
  value       = aws_alb.main.dns_name
  description = "The public DNS name of the ALB"
}