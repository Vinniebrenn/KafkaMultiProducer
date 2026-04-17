

Spring Boot Migration – Detailed Technical Architecture & Transformation Document

---

1. Executive Summary

The current application landscape is built on a vendor-provided SWIFT processing system deployed on WebSphere 8.x running on RHEL, both of which have reached end-of-life (EOL) and are no longer aligned with enterprise support, security, and operational standards.

In parallel, the vendor has transitioned their product roadmap to a Spring Boot–based runtime, with future releases no longer supporting WebSphere. This makes the migration both mandatory and time-bound, rather than optional.

To address these constraints and align with enterprise strategy, the application will be migrated to a Spring Boot architecture deployed on OpenShift, the organization’s standard container platform.

This transformation is not just a platform migration, but a fundamental architectural evolution, covering:

- runtime model (WebSphere → Spring Boot)
- deployment (VM/server → containers/OpenShift)
- integration (filesystem → MQ-based messaging)
- delivery model (manual → DevSecOps-driven)

The immediate focus is to deliver v17 within the current year, ensuring functional parity while enabling a modern, scalable, and enterprise-aligned foundation for v18 and beyond.

---

2. Current Architecture (WebSphere-Based Model)

2.1 Runtime Environment

The application is currently deployed as a WAR file on WebSphere Application Server 8.x, hosted on RHEL servers.

Key characteristics:

- Application lifecycle managed by WebSphere
- Dependency on WebSphere services (JMS, connection pooling, threading)
- Tight coupling between application and runtime

This creates a non-portable, server-dependent architecture.

---

2.2 Integration Model (Filesystem-Based SWIFT Processing)

The current system relies on a shared NAS-based filesystem integration model.

Inbound Flow:

- SWIFT files are placed in an input directory (NAS shared path)
- Application polls and reads files from this directory

Processing:

- Business logic processes SWIFT files

Post-processing:

- Successfully processed files → moved to processed folder
- Failed files → moved to error/failed folder

Outbound Flow:

- Application generates SWIFT output files
- Files are written back to the NAS location
- Downstream systems consume from shared folder

Limitations:

- Tight coupling via filesystem
- No real-time processing (batch/polling driven)
- Scalability constraints
- File locking / concurrency issues
- Not container-friendly

---

2.3 Messaging Layer

- IBM MQ (v9.3) exists in the environment
- Application interacts with MQ via WebSphere MQ JMS provider
- MQ client libraries are provided by WebSphere resource adapter ("wmq.jmsra.rar")
- No direct MQ dependency inside application

This hides MQ complexity but introduces:

- lack of control over MQ client version
- dependency on WebSphere runtime

---

2.4 Database Layer

- Oracle 19c used for persistence
- Accessed via WebSphere-managed data sources

---

2.5 Scheduling Model

- Batch jobs controlled via cron jobs on RHEL
- Scripts trigger application processes

Limitations:

- Distributed scheduling
- Lack of central control
- Not aligned with enterprise scheduling standards

---

2.6 Key Limitations

- End-of-life platform (WebSphere + RHEL)
- Filesystem-based integration limits scalability
- Tight coupling to infrastructure
- Lack of cloud/container readiness
- Hidden MQ dependency
- Manual deployment and operational overhead

---

3. Target Architecture (Spring Boot on OpenShift)

3.1 Runtime Model

The application will run as a Spring Boot service deployed on OpenShift.

Key characteristics:

- Stateless containerized application
- Multiple instances (pods) running in parallel
- Load-balanced and horizontally scalable
- No dependency on application server

This enables:

- portability
- scalability
- platform standardization

---

3.2 Integration Model (MQ-Based)

The target architecture replaces filesystem integration with event-driven MQ-based integration.

Inbound Flow:

- Upstream systems publish messages to MQ Input Queue
- Application consumes messages asynchronously

Processing:

- Business logic executed within Spring Boot service

Outbound Flow:

- Processed messages published to MQ Output Queue
- Downstream systems consume from MQ

Failure Handling:

- Failed messages routed to Error Queue / Dead Letter Queue (DLQ)

Mapping from Current Model:

Filesystem Model| MQ Model
Input Folder| Input Queue
Processed Folder| Message consumed / logged
Error Folder| Error Queue / DLQ
Output Folder| Output Queue

Benefits:

- decoupled systems
- real-time processing
- better scalability
- resilience

---

3.3 Database Layer

- Oracle 19c retained
- Direct connection via application configuration
- No WebSphere dependency

---

3.4 Scheduling Model

- Scheduling handled via Spring Boot Scheduler
- Jobs triggered internally within application

Benefits:

- centralized logic
- container-aware execution

---

3.5 OpenShift Platform Capabilities

- Container orchestration
- Auto-scaling
- Load balancing
- Health monitoring (readiness/liveness probes)
- Self-healing (pod restart)
- Integrated logging and monitoring

---

4. DevOps and Deployment Model

4.1 Build and Packaging

- Vendor provides application artifacts
- Application packaged into Docker container
- Includes:
  - Java runtime
  - MQ client libraries
  - application configs

---

4.2 CI/CD Pipeline

Deployment follows enterprise DevSecOps pipeline:

- Source onboarding
- Container build
- Security scanning
- Compliance validation
- Promotion across environments:
  - Dev → QA → UAT → Prod

---

4.3 Deployment Model

- Deployed as OpenShift pods
- Stateless design
- Config managed via ConfigMaps and Secrets
- Version-controlled deployments

---

4.4 Operational Benefits

- Immutable deployments
- Easy rollback
- Automated recovery
- Consistent environments

---

5. Key Architectural Evolution

Area| Current| Target
Runtime| WebSphere| Spring Boot
Platform| RHEL Servers| OpenShift
Integration| Filesystem| MQ
MQ Handling| WebSphere-managed| Application-managed
Scheduling| Cron| Spring Scheduler
Deployment| Manual| CI/CD
Scalability| Limited| Horizontal
Resilience| Manual| Automated

---

6. Strategic Benefits

- Alignment with enterprise OpenShift platform
- Removal of legacy dependencies
- Improved scalability and resilience
- Event-driven integration model
- Standardized DevOps delivery
- Future-ready architecture

---

7. Migration Approach

Phase 1 – v17 (Current Scope)

- Move to Spring Boot
- Deploy on OpenShift
- Shift integration to MQ
- Maintain functional parity

Phase 2 – v18 (Future)

- Optimization
- Platform enhancements
- Further architectural improvements

---

8. Conclusion

This migration represents a critical modernization initiative, driven by both platform lifecycle constraints and vendor direction.

The transition from WebSphere to Spring Boot on OpenShift introduces:

- a scalable architecture
- a decoupled integration model
- a standardized deployment pipeline

While v17 ensures continuity, it establishes a strong foundation for long-term transformation and enterprise alignment.

---
