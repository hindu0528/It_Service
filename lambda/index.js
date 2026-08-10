const Jimp = require('jimp');
const { S3Client, GetObjectCommand, PutObjectCommand } = require('@aws-sdk/client-s3');

const s3 = new S3Client({});

exports.handler = async (event) => {
    const record = event.Records[0];
    const bucket = record.s3.bucket.name;
    const key = decodeURIComponent(record.s3.object.key.replace(/\+/g, ' '));

    const UPLOAD_PREFIX = "uploads/";
    const THUMBNAIL_PREFIX = "thumbnails/";

    if (!key.startsWith(UPLOAD_PREFIX)) {
        console.log(`Skipping object ${key} outside of ${UPLOAD_PREFIX}`);
        return { statusCode: 200, body: 'Skipped' };
    }

    console.log(`Processing object: ${key} in bucket: ${bucket}`);

    try {
        // Get image from S3
        const getObjCmd = new GetObjectCommand({ Bucket: bucket, Key: key });
        const s3Response = await s3.send(getObjCmd);
        
        // Convert stream to Buffer
        const streamToBuffer = async (stream) => {
            return new Promise((resolve, reject) => {
                const chunks = [];
                stream.on('data', (chunk) => chunks.push(chunk));
                stream.on('error', reject);
                stream.on('end', () => resolve(Buffer.concat(chunks)));
            });
        };
        const imageBuffer = await streamToBuffer(s3Response.Body);

        // Read image and resize with Jimp
        const image = await Jimp.read(imageBuffer);
        image.resize(128, Jimp.AUTO); // Resize to width 128, height auto
        const thumbnailBuffer = await image.getBufferAsync(image.getMIME());

        // Target key
        const destKey = key.replace(UPLOAD_PREFIX, THUMBNAIL_PREFIX);

        // Upload back to S3
        const putObjCmd = new PutObjectCommand({
            Bucket: bucket,
            Key: destKey,
            Body: thumbnailBuffer,
            ContentType: image.getMIME()
        });
        await s3.send(putObjCmd);

        console.log(`Successfully uploaded thumbnail to ${destKey}`);
        return { statusCode: 200, body: 'Success' };
    } catch (err) {
        console.error(err);
        throw err;
    }
};
