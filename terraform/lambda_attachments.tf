# S3 Bucket for Attachments (Milestone 5)
resource "aws_s3_bucket" "attachments_bucket" {
  bucket_prefix = "ticketdesk-attachments-"
  force_destroy = true
}

resource "aws_s3_bucket_public_access_block" "attachments" {
  bucket                  = aws_s3_bucket.attachments_bucket.id
  block_public_acls       = true
  block_public_policy     = true
  ignore_public_acls      = true
  restrict_public_buckets = true
}

# Zip package for Lambda function
data "archive_file" "lambda_zip" {
  type        = "zip"
  source_file = "${path.module}/../lambda/thumbnail.py"
  output_path = "${path.module}/../lambda/thumbnail.zip"
}

# IAM Role for Lambda
resource "aws_iam_role" "lambda_role" {
  name = "thumbnail-lambda-role"
  assume_role_policy = jsonencode({
    Version = "2012-10-17"
    Statement = [{
      Action    = "sts:AssumeRole"
      Effect    = "Allow"
      Principal = { Service = "lambda.amazonaws.com" }
    }]
  })
}

resource "aws_iam_role_policy_attachment" "lambda_basic" {
  role       = aws_iam_role.lambda_role.name
  policy_arn = "arn:aws:iam::aws:policy/service-role/AWSLambdaBasicExecutionRole"
}

resource "aws_iam_role_policy_attachment" "lambda_s3" {
  role       = aws_iam_role.lambda_role.name
  policy_arn = "arn:aws:iam::aws:policy/AmazonS3FullAccess"
}

# Lambda Function (Python 3.11 with Pillow Layer)
resource "aws_lambda_function" "thumbnail" {
  filename         = data.archive_file.lambda_zip.output_path
  source_code_hash = data.archive_file.lambda_zip.output_base64sha256
  function_name    = "ticketdesk-thumbnail-generator"
  role             = aws_iam_role.lambda_role.arn
  handler          = "thumbnail.handler"
  runtime          = "python3.11"
  timeout          = 15

  # Uses a public Klayers Pillow dependency Layer for Python 3.11 in us-east-1
  layers = ["arn:aws:lambda:us-east-1:770693421928:layer:Klayers-p311-Pillow:1"]
}

# Give S3 permission to invoke the Lambda
resource "aws_lambda_permission" "allow_s3" {
  statement_id  = "AllowExecutionFromS3"
  action        = "lambda:InvokeFunction"
  function_name = aws_lambda_function.thumbnail.function_name
  principal     = "s3.amazonaws.com"
  source_arn    = aws_s3_bucket.attachments_bucket.arn
}

# Configure S3 Event notification trigger on uploads/
resource "aws_s3_bucket_notification" "bucket_notification" {
  bucket = aws_s3_bucket.attachments_bucket.id

  lambda_function {
    lambda_function_arn = aws_lambda_function.thumbnail.arn
    events              = ["s3:ObjectCreated:*"]
    filter_prefix       = "uploads/"
  }

  depends_on = [aws_lambda_permission.allow_s3]
}