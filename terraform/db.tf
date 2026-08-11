# DB Subnet Group
resource "aws_db_subnet_group" "db_subnets" {
  name       = "ecs-fargate-db-subnet-group"
  subnet_ids = [aws_subnet.private_1.id, aws_subnet.private_2.id]
}

# Generate password
resource "random_password" "db_password" {
  length  = 16
  special = false
}

# Secrets Manager Secret
resource "aws_secretsmanager_secret" "db_password" {
  name_prefix             = "ecs-fargate-db-password-"
  recovery_window_in_days = 0
}

resource "aws_secretsmanager_secret_version" "db_password_val" {
  secret_id     = aws_secretsmanager_secret.db_password.id
  secret_string = random_password.db_password.result
}

# Parameter Store: DB Configurations
resource "aws_ssm_parameter" "db_username" {
  name  = "/config/app/db_username"
  type  = "String"
  value = "dbadmin"
}

resource "aws_ssm_parameter" "db_url_ticket" {
  name  = "/config/ticket-service/db_url"
  type  = "String"
  value = "jdbc:mysql://${aws_db_instance.main.endpoint}/ticketdesk_ticket?createDatabaseIfNotExist=true&useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=UTC"
}

resource "aws_ssm_parameter" "db_url_auth" {
  name  = "/config/auth-service/db_url"
  type  = "String"
  value = "jdbc:mysql://${aws_db_instance.main.endpoint}/ticketdesk_auth?createDatabaseIfNotExist=true&useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=UTC"
}

resource "aws_ssm_parameter" "db_url_attachment" {
  name  = "/config/attachment-service/db_url"
  type  = "String"
  value = "jdbc:mysql://${aws_db_instance.main.endpoint}/ticketdesk_attachment?createDatabaseIfNotExist=true&useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=UTC"
}

# RDS DB Instance (Free Tier MySQL)
resource "aws_db_instance" "main" {
  identifier             = "ticketdesk-db"
  engine                 = "mysql"
  engine_version         = "8.0"
  instance_class         = "db.t3.micro"
  allocated_storage      = 20
  db_name                = "ticketdesk_ticket"
  username               = "dbadmin"
  password               = random_password.db_password.result
  db_subnet_group_name   = aws_db_subnet_group.db_subnets.name
  vpc_security_group_ids = [aws_security_group.db_sg.id]
  skip_final_snapshot    = true
  deletion_protection    = false
}
