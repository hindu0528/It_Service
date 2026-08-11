resource "aws_alb" "main" {
  name_prefix     = "ecs-"
  subnets         = [aws_subnet.public_1.id, aws_subnet.public_2.id]
  security_groups = [aws_security_group.alb.id]
}

# Target Groups
resource "aws_alb_target_group" "gateway" {
  name_prefix = "tg-gw"
  port        = 8080
  protocol    = "HTTP"
  vpc_id      = aws_vpc.main.id
  target_type = "ip"

  health_check {
    path                = "/actuator/health"
    matcher             = "200,404"
    interval            = 45
    timeout             = 10
  }
}

resource "aws_alb_target_group" "discovery" {
  name_prefix = "tg-dsc"
  port        = 8761
  protocol    = "HTTP"
  vpc_id      = aws_vpc.main.id
  target_type = "ip"

  health_check {
    path                = "/"
    matcher             = "200"
    interval            = 60
    timeout             = 30
    healthy_threshold   = 2
    unhealthy_threshold = 5
  }
}

resource "aws_alb_target_group" "config" {
  name_prefix = "tg-cfg"
  port        = 8888
  protocol    = "HTTP"
  vpc_id      = aws_vpc.main.id
  target_type = "ip"

  health_check {
    path    = "/config-server/dev"
    matcher = "200,404"
  }
}

# Listeners
resource "aws_alb_listener" "front_end" {
  load_balancer_arn = aws_alb.main.arn
  port              = 80
  protocol          = "HTTP"

  default_action {
    target_group_arn = aws_alb_target_group.gateway.arn # Default path routes to API Gateway
    type             = "forward"
  }
}

resource "aws_alb_listener" "discovery" {
  load_balancer_arn = aws_alb.main.arn
  port              = 8761
  protocol          = "HTTP"

  default_action {
    target_group_arn = aws_alb_target_group.discovery.arn
    type             = "forward"
  }
}

resource "aws_alb_listener" "config" {
  load_balancer_arn = aws_alb.main.arn
  port              = 8888
  protocol          = "HTTP"

  default_action {
    target_group_arn = aws_alb_target_group.config.arn
    type             = "forward"
  }
}