import boto3
import os
from botocore.exceptions import ClientError

class S3Service:
    def __init__(self):
        self.s3 = boto3.client(
            "s3",
            endpoint_url=os.getenv("S3_ENDPOINT", "http://minio:9000"),
            aws_access_key_id=os.getenv("S3_ACCESS_KEY"),
            aws_secret_access_key=os.getenv("S3_SECRET_KEY"),
            config=boto3.session.Config(signature_version="s3v4")
        )

    def download_file(self, bucket_name: str, object_key: str, local_path: str):
        try:
            self.s3.download_file(bucket_name, object_key, local_path)
            print(f"File downloaded: s3://{bucket_name}/{object_key} -> {local_path}")
        except ClientError as e:
            print(f"Error downloading from S3: {e}")
            raise Exception(f"Failed to download {object_key} from {bucket_name}")