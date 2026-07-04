# Azure Environments

This document records the current deployment architecture decision for FlowMova backend environments.

## Decision

FlowMova will use separate Azure environments for development and production.

Development keeps costs low by running PostgreSQL as a container. Production uses Azure Database for PostgreSQL Flexible Server for managed backups, maintenance, monitoring, and safer operations.

Container images are stored in GitHub Container Registry (`ghcr.io`) instead of Azure Container Registry at the start.

## Target Layout

```text
DEV
rg-flowmova-dev
  cae-flowmova-dev
    ca-flowmova-api-dev
    ca-flowmova-postgres-dev
  Azure Files volume for dev PostgreSQL data

PROD
rg-flowmova-prod
  cae-flowmova-prod
    ca-flowmova-api-prod
  Azure Database for PostgreSQL Flexible Server
  Azure Blob Storage
```

## Development Environment

The development environment is used to validate cloud deployment, pipeline behavior, and integration with Azure resources.

Components:

- Azure Container Apps environment: `cae-flowmova-dev`
- Backend container app: `ca-flowmova-api-dev`
- PostgreSQL container app: `ca-flowmova-postgres-dev`
- Persistent storage for PostgreSQL via Azure Files
- Images pulled from GitHub Container Registry

Rules:

- Dev data is not critical production data.
- PostgreSQL dev can be restarted, reset, or recreated when needed.
- A persistent volume is required if dev data must survive container restarts.
- Backend dev should use `minReplicas=0` when possible.
- PostgreSQL dev should use minimal CPU and memory.

Example dev datasource:

```text
FLOWMOVA_DATASOURCE_URL=jdbc:postgresql://<dev-postgres-host>:5432/flowmova
```

## Production Environment

The production environment is used for real customers and real business data.

Components:

- Azure Container Apps environment: `cae-flowmova-prod`
- Backend container app: `ca-flowmova-api-prod`
- Azure Database for PostgreSQL Flexible Server
- Azure Blob Storage for future file/object storage needs
- Images pulled from GitHub Container Registry

Rules:

- PostgreSQL must not run as an unmanaged container in production.
- Production uses Azure Database for PostgreSQL Flexible Server.
- Production database backups and restore strategy must be enabled before handling real customer data.
- Production secrets must be separated from development secrets.
- Production deployment should require manual approval in GitHub Actions.

Example prod datasource:

```text
FLOWMOVA_DATASOURCE_URL=jdbc:postgresql://<prod-server>.postgres.database.azure.com:5432/flowmova
```

## GitHub Container Registry

The first container registry is GitHub Container Registry.

Image examples:

```text
ghcr.io/darkstorm97/flowmova-backend:dev
ghcr.io/darkstorm97/flowmova-backend:prod
ghcr.io/darkstorm97/flowmova-backend:<commit-sha>
```

Deployment flow:

```text
GitHub Actions
  -> build Docker image
  -> push image to ghcr.io
  -> deploy Azure Container App revision using that image
```

If images are private, Azure Container Apps must be configured with credentials that can read GitHub packages.

## Cost Expectation For Early Tests

For an early test with around two customer companies, a few users, and low traffic, the expected monthly cost is approximately:

```text
45 to 90 CAD / month
```

Approximate breakdown:

| Component | Estimated monthly cost |
| --- | ---: |
| Backend dev Container Apps, scale-to-zero | 0 to 3 CAD |
| PostgreSQL dev container plus small Azure Files volume | 5 to 20 CAD |
| Backend prod Container Apps, scale-to-zero or very small footprint | 0 to 10 CAD |
| Azure PostgreSQL prod burstable minimal tier | 25 to 55 CAD |
| Blob Storage low volume | 1 to 5 CAD |
| Logs and monitoring | 5 to 15 CAD |
| GitHub Container Registry | 0 CAD in Azure |

Recommended budget controls:

- Azure budget: 75 CAD / month.
- Alert at 50 CAD.
- Alert at 70 CAD.
- Keep log retention short at the beginning.
- Use LRS storage unless a stronger redundancy requirement is validated.

## Environment Separation Rules

- Dev and prod use separate resource groups.
- Dev and prod use separate Container Apps environments.
- Dev and prod use separate databases.
- Dev and prod use separate secrets.
- Dev can be recreated without impacting prod.
- Prod deployment requires more control than dev deployment.

## Future Migration Notes

If development data becomes important, the dev PostgreSQL container can be migrated to Azure Database for PostgreSQL Flexible Server later.

If production traffic grows, production can be adjusted by:

- increasing PostgreSQL SKU,
- increasing backend `minReplicas`,
- adding autoscaling rules,
- extending log retention,
- adding stricter backup and monitoring policies.
