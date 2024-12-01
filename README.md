# CS441_Fall2024_Assignment_3 (Server)
### Author : Akhil S Nair
### Email : anair56@uic.edu

## Project Description
This project implements a microservice architecture for handling LLM (Large Language Model) interactions using AWS Bedrock and gRPC. The server component handles requests through gRPC and interacts with AWS Lambda to process queries using AWS Bedrock.

## Demo Video
[Project Demo and Deployment Walkthrough Video](https://youtu.be/PKA09SNOq60?si=z3r6hO1_Hq6M23sv)

### Key Components
1. **Routes**: HTTP endpoint handling using Akka HTTP
2. **gRPC Server**: Manages gRPC communication
3. **Bedrock Service**: Implements LLM query processing
4. **AWS Lambda Integration**: Connects to AWS Bedrock

## Prerequisites
- Java 11 or higher
- SBT
- Docker (for containerized deployment)
- AWS Account with configured credentials
- AWS CLI

### Installation

#### Local Setup

1. Clone the git repository
```bash
git clone git@github.com:akhilmw2/CS441_Fall2024_Assignment_3.git
cd CS441_Fall2024_Assignment_3
```

#### Build and Run using SBT
```
sbt clean compile
sbt run

Akka server port : 8080
gRPC port: 50051
```
### Docker Usage

#### Local
1. Create a deployment directory:
```bash
cd ~/Desktop
mkdir assignment3-deployment
cd assignment3-deployment
cd assignment3-deployment
mkdir conversations  # For storing conversation logs
```
2. Copy this file src/main/resources/docker-compose.yml into the created directory
3. Create a .env file in the same directory:
```
AWS_ACCESS_KEY_ID=your_access_key
AWS_SECRET_ACCESS_KEY=your_secret_key
AWS_REGION=us-east-1
```
4. Build and run:
``` 
# Build and start containers
docker-compose up --build

# To stop containers
docker-compose down
```
5. Your directory structure should look like:
```
Desktop/
├── assignment3-deployment/
│   ├── docker-compose.yml
│   ├── .env
│   └── conversations/       # For storing conversation logs
├── CS441_Fall2024_Assignment_3/        # Server Project
│   └── Dockerfile
└── CS441_Fall2024_Assignment_3_Client/ # Client Project
└── Dockerfile

```

### Conclusion
This project successfully implements a distributed conversational system leveraging AWS Bedrock and Ollama, demonstrating effective integration of cloud and local LLM services through microservices architecture. The implementation showcases robust communication using gRPC, containerized deployment capabilities, and scalable design suitable for both local development and cloud deployment.

## Acknowledgments
I would like to thank Professor Mark Grechanik for his guidance and instruction in the CS 441 course, which has been invaluable in developing and refining this project. Special thanks to TA Vasu Garg for support and insights throughout the process.


