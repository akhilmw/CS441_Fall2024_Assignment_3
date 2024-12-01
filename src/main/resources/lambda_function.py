import boto3
import json
import os
import logging

# Set up logging
logger = logging.getLogger()
logger.setLevel(logging.INFO)

def lambda_handler(event, context):
    # Log the incoming event for debugging
    logger.info(f"Received event: {json.dumps(event)}")

    # More flexible query extraction
    query = event.get('query', event.get('body', {}).get('query'))
    
    if not query:
        logger.error("No query found in event")
        return {
            "statusCode": 400,
            "body": json.dumps({"error": "Missing 'query' in the request body."})
        }

    # Specific configuration for Titan Text Lite
    model_id = 'amazon.titan-text-lite-v1'
    max_tokens = int(os.environ.get('MAX_TOKENS', 50))
    temperature = float(os.environ.get('TEMPERATURE', 0.3))

    # Initialize Bedrock client - let boto3 use default region
    bedrock_client = boto3.client('bedrock-runtime')

    # Titan Text Lite specific request body
    bedrock_request = {
        "inputText": query,
        "textGenerationConfig": {
            "maxTokenCount": max_tokens,
            "temperature": temperature,
            "stopSequences": [],
            "topP": 1
        }
    }

    try:
        # Call Bedrock model
        response = bedrock_client.invoke_model(
            modelId=model_id,
            contentType="application/json",
            accept="application/json",
            body=json.dumps(bedrock_request)
        )

        # Parse Bedrock response
        response_body = json.loads(response['body'].read().decode('utf-8'))
        
        # Extract output text for Titan model
        output_text = response_body['results'][0]['outputText']

        logger.info(f"Model response: {output_text}")

        # Return the generated text
        return {
            "statusCode": 200,
            "body": json.dumps({
                "response": output_text,
                "model": model_id
            })
        }

    except Exception as e:
        logger.error(f"Error invoking Bedrock: {str(e)}")
        return {
            "statusCode": 500,
            "body": json.dumps({
                "error": str(e),
                "model": model_id
            })
        }
