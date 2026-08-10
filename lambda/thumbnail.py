import json
import os
import boto3
from io import BytesIO
from PIL import Image

s3_client = boto3.client('s3')

def handler(event, context):
    # Parse the S3 Event
    record = event['Records'][0]
    bucket = record['s3']['bucket']['name']
    key = record['s3']['object']['key']
    
    # Define prefixes
    UPLOAD_PREFIX = "uploads/"
    THUMBNAIL_PREFIX = "thumbnails/"
    
    # Avoid recursion: only process files in uploads/
    if not key.startswith(UPLOAD_PREFIX):
        print(f"Skipping object {key} outside of {UPLOAD_PREFIX}")
        return {
            'statusCode': 200,
            'body': json.dumps('Skipped non-upload prefix')
        }
        
    print(f"Processing object: {key} in bucket: {bucket}")
    
    # Get the image file from S3
    response = s3_client.get_object(Bucket=bucket, Key=key)
    image_data = response['Body'].read()
    
    # Open image with Pillow and generate thumbnail
    image = Image.open(BytesIO(image_data))
    image.thumbnail((128, 128))
    
    # Write thumbnail image to memory buffer
    buffer = BytesIO()
    # Determine format
    img_format = image.format if image.format else 'JPEG'
    image.save(buffer, format=img_format)
    buffer.seek(0)
    
    # Generate the destination key under thumbnails/
    dest_key = key.replace(UPLOAD_PREFIX, THUMBNAIL_PREFIX, 1)
    
    # Upload thumbnail back to S3
    s3_client.put_object(
        Bucket=bucket,
        Key=dest_key,
        Body=buffer,
        ContentType=response.get('ContentType', 'image/jpeg')
    )
    
    print(f"Successfully uploaded thumbnail to {dest_key}")
    return {
        'statusCode': 200,
        'body': json.dumps('Thumbnail generated successfully!')
    }