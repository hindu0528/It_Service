# Application Load Balancer
resource "aws_alb" "main" {
  name_prefix     = "ecs-"
  subnets         = [aws_subnet.public_1.id, aws_subnet.public_2.id]
  security_groups = [aws_security_group.alb.id]

  tags = {
    Name = "ecs-fargate-alb"
  }
}

# ----------------- TARGET GROUPS -----------------

# Target Group: Frontend / Default
# Target Group: Frontend / Default
resource "aws_alb_target_group" "app" {
  name_prefix = "tg-ecs" # <-- Changed back to keep the existing target group
  port        = 80
  protocol    = "HTTP"
  vpc_id      = aws_vpc.main.id
  target_type = "ip"

  health_check {
    path = "/"
  }
}

# Target Group: Discovery Server
resource "aws_alb_target_group" "discovery" {
  name_prefix = "tg-dsc"
  port        = 8761
  protocol    = "HTTP"
  vpc_id      = aws_vpc.main.id
  target_type = "ip"

  health_check {
    path                = "/"
    matcher             = "200"
    interval            = 30
    timeout             = 5
    unhealthy_threshold = 3
  }
}

# Target Group: Config Server
resource "aws_alb_target_group" "config" {
  name_prefix = "tg-cfg"
  port        = 8888
  protocol    = "HTTP"
  vpc_id      = aws_vpc.main.id
  target_type = "ip"

  health_check {
    path                = "/config-server/dev" # Endpoint that config server responds to
    matcher             = "200,404"            # We accept 404 since it's just querying configs
    interval            = 30
    timeout             = 5
    unhealthy_threshold = 3
  }
}

# ----------------- LISTENERS -----------------

# HTTP Listener - Port 80 (Forward to UI/Gateway)
resource "aws_alb_listener" "front_end" {
  load_balancer_arn = aws_alb.main.arn
  port              = 80
  protocol          = "HTTP"

  default_action {
    target_group_arn = aws_alb_target_group.app.arn
    type             = "forward"
  }
}

# HTTP Listener - Port 8761 (Forward to Discovery Server)
resource "aws_alb_listener" "discovery" {
  load_balancer_arn = aws_alb.main.arn
  port              = 8761
  protocol          = "HTTP"

  default_action {
    target_group_arn = aws_alb_target_group.discovery.arn
    type             = "forward"
  }
}

# HTTP Listener - Port 8888 (Forward to Config Server)
resource "aws_alb_listener" "config" {
  load_balancer_arn = aws_alb.main.arn
  port              = 8888
  protocol          = "HTTP"

  default_action {
    target_group_arn = aws_alb_target_group.config.arn
    type             = "forward"
  }
}

# Outputs
output "alb_hostname" {
  value       = aws_alb.main.dns_name
  description = "The public DNS name of the ALB"
}