# Imixs-Micro-Engine

Imixs-Micro-Engine is a lightweight BPMN Workflow engine built on top of the Quarkus framework. This project aims to provide a high-performance, low-footprint workflow solution that can be easily deployed in containerized environments.

At its core, Imixs-Micro-Engine leverages the power of WebSocket technology to enable real-time, bidirectional communication between the workflow engine and its clients. This allows for immediate updates and responsive workflow management, making it ideal for modern, distributed applications.

Built with Java 17 and the latest Quarkus framework, Imixs-Micro takes advantage of cutting-edge features for optimal performance. The project structure follows Maven conventions, making it familiar and easy to navigate for Java developers.

Key features of Imixs-Micro-Engine include:

- Real-time workflow updates using WebSocket technology
- Lightweight and fast Quarkus-based runtime
- Containerized deployment with Docker support
- GraalVM integration for potential native compilation
- Comprehensive JUnit tests ensuring reliability

The engine is designed with cloud-native principles in mind. It can be easily deployed as a standalone JAR or as a Docker container, making it versatile for various deployment scenarios - from development environments to production Kubernetes clusters.

The Imixs-Micro Docker integration uses a Red Hat Universal Base Image (UBI) with OpenJDK 17, ensuring a stable and secure foundation. The inclusion of GraalVM in our Docker image opens up possibilities for native compilation, potentially further reducing startup time and memory footprint.

Imixs-Micro-Engine represents a new approach to workflow management - one that embraces modern, cloud-native technologies while providing the robust BPMN support that businesses rely on. Whether you're building a small-scale workflow application or a large, distributed system, Imixs-Micro-Engine offers the flexibility, performance, and ease-of-use to meet your needs.

Get started with Imixs-Micro-Engine today and experience the future of lightweight, high-performance workflow management!

For development read the [Developer Guide](./DEVELOPMENT.md)