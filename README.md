graph TD
    %% ZONE 1: LEGACY (Greyed out to show decommissioning)
    subgraph Legacy_Zone [LEGACY: ON-PREM RHEL]
        direction TB
        WS[IBM WebSphere <br/> EAR-Based Monolith] --- OS[Outdated RHEL 6/7]
        NAS[NAS File System]
    end

    %% ZONE 2: DEVOPS (The 'How' it gets there)
    subgraph DevOps_Zone [ENTERPRISE DEVOPS PIPELINE]
        Git[Git / Source] --> Build[Maven/Gradle Build]
        Build --> Scan[Security Scan <br/> Aqua/Sonar]
        Scan --> Registry[(Image Registry <br/> Quay/Artifactory)]
    end

    %% ZONE 3: OPENSHIFT RUNTIME (The 'Where' it lives)
    subgraph OCP_Cluster [OPENSHIFT CONTAINER PLATFORM]
        direction TB
        Route([OCP Route]) --> SVC[OCP Service]
        
        subgraph Pod_Scaling [Scalable Pod Group]
            APP[Spring Boot Application <br/> Stateless Runtime]
            JDBC[Oracle JDBC]
            JMS[MQ Client]
        end

        SVC --> APP
        Config[(ConfigMaps / Secrets)] -.-> APP
        Health{Liveness / <br/> Readiness} --- APP
    end

    %% ZONE 4: INTEGRATION & DATA
    subgraph MQ_Backbone [INTEGRATION LAYER: IBM MQ]
        direction TB
        IQ[INPUT_QUEUE <br/> Inbound SWIFT]
        OQ[OUTPUT_QUEUE <br/> Outbound SWIFT]
        EQ[ERROR_DLQ <br/> Failed Processing]
    end

    DB[(Oracle Database)]

    %% CONNECTORS
    Upstream[Upstream Systems] --> IQ
    IQ --> Route
    APP --> OQ
    OQ --> Downstream[Downstream Systems]
    APP -- "Failure/Retry" --> EQ
    APP <--> DB
    Scheduler[External Scheduler] -- "REST Trigger" --> Route
    Registry -- "Automated Image Push" --> Pod_Scaling

    %% STYLING (Lucidchart Compatible)
    style Legacy_Zone fill:#f5f5f5,stroke:#999,stroke-dasharray: 5 5
    style DevOps_Zone fill:#e3f2fd,stroke:#1565c0
    style OCP_Cluster fill:#fff,stroke:#e65100,stroke-width:2px
    style MQ_Backbone fill:#e8f5e9,stroke:#2e7d32
    style APP fill:#c8e6c9,stroke:#2e7d32,stroke-width:2px
    style EQ fill:#ffebee,stroke:#c62828
    style DB fill:#fff3e0,stroke:#ef6c00
