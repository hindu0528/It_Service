# Observability Dashboard (Milestone 7)
resource "aws_cloudwatch_dashboard" "main" {
  dashboard_name = "ECS-Fargate-Microservices-Dashboard"

  dashboard_body = jsonencode({
    widgets = [
      {
        type   = "metric"
        width  = 12
        height = 6
        properties = {
          metrics = [
            ["AWS/ApplicationELB", "RequestCount", "LoadBalancer", aws_alb.main.arn_suffix]
          ]
          period = 60
          stat   = "Sum"
          region = var.aws_region
          title  = "Load Balancer Request Count"
        }
      },
      {
        type   = "metric"
        width  = 12
        height = 6
        properties = {
          metrics = [
            ["AWS/ApplicationELB", "HTTPCode_Target_5XX_Count", "LoadBalancer", aws_alb.main.arn_suffix]
          ]
          period = 60
          stat   = "Sum"
          region = var.aws_region
          title  = "Target 5XX Response Count"
        }
      },
      {
        type   = "metric"
        width  = 12
        height = 6
        properties = {
          metrics = [
            ["AWS/ApplicationELB", "TargetResponseTime", "LoadBalancer", aws_alb.main.arn_suffix]
          ]
          period = 60
          stat   = "Average"
          region = var.aws_region
          title  = "Target Response Time"
        }
      },
      {
        type   = "metric"
        width  = 12
        height = 6
        properties = {
          metrics = [
            ["AWS/RDS", "DatabaseConnections", "DBInstanceIdentifier", aws_db_instance.main.identifier]
          ]
          period = 60
          stat   = "Average"
          region = var.aws_region
          title  = "Database Connections"
        }
      }
    ]
  })
}

# SNS Topic for Alarms
resource "aws_sns_topic" "alerts" {
  name = "cloudwatch-alerts-topic"
}

# Alarms (Milestone 7 Alerting)

# 1. Unhealthy Target Alarm
resource "aws_cloudwatch_metric_alarm" "unhealthy_targets" {
  alarm_name          = "unhealthy-target-hosts"
  comparison_operator = "GreaterThanOrEqualToThreshold"
  evaluation_periods  = "1"
  metric_name         = "UnHealthyHostCount"
  namespace           = "AWS/ApplicationELB"
  period              = "60"
  statistic           = "Average"
  threshold           = "1"
  alarm_actions       = [aws_sns_topic.alerts.arn]
  
  dimensions = {
    TargetGroup  = aws_alb_target_group.gateway.arn_suffix
    LoadBalancer = aws_alb.main.arn_suffix
  }
}

# 2. Database High CPU Alarm
resource "aws_cloudwatch_metric_alarm" "db_cpu" {
  alarm_name          = "high-database-cpu"
  comparison_operator = "GreaterThanThreshold"
  evaluation_periods  = "2"
  metric_name         = "CPUUtilization"
  namespace           = "AWS/RDS"
  period              = "300"
  statistic           = "Average"
  threshold           = "80"
  alarm_actions       = [aws_sns_topic.alerts.arn]

  dimensions = {
    DBInstanceIdentifier = aws_db_instance.main.identifier
  }
}

# 3. HTTP 5XX Target Error Alarm
resource "aws_cloudwatch_metric_alarm" "http_5xx" {
  alarm_name          = "alb-target-5xx-errors"
  comparison_operator = "GreaterThanThreshold"
  evaluation_periods  = "1"
  metric_name         = "HTTPCode_Target_5XX_Count"
  namespace           = "AWS/ApplicationELB"
  period              = "60"
  statistic           = "Sum"
  threshold           = "5"
  alarm_actions       = [aws_sns_topic.alerts.arn]

  dimensions = {
    LoadBalancer = aws_alb.main.arn_suffix
  }
}