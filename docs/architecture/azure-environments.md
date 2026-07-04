# Azure Environments

This document records the current deployment architecture decision for FlowMova backend environments.

## Decision

FlowMova will use separate Azure environments for development and production.

Development keeps costs low by running PostgreSQL as a container. Production uses Azure Database for PostgreSQL Flexible Server for managed backups, maintenance, monitoring, and safer operations.

Container images are stored in GitHub Container Registry (`ghcr.io`) instead of Azure Container Registry at the start.

## Target Layout

```text
DEV
Region: Canada Central
rg-flowmova-dev
  log-flowmova-dev
  cae-flowmova-dev
    ca-flowmova-api-dev
    ca-flowmova-postgres-dev
  stflowmovadev001
    Azure Files share: flowmova-postgres-dev-data

PROD
Region: South Africa North, or closest available Africa-region alternative
rg-flowmova-prod
  log-flowmova-prod
  cae-flowmova-prod
    ca-flowmova-api-prod
  psql-flowmova-prod
  stflowmovaprod001
    Blob container: flowmova-prod
```

Storage account names are globally unique in Azure, lowercase-only, and cannot contain hyphens. If a proposed storage account name is unavailable, append a short numeric suffix while preserving the environment name.

## Resource Naming

### Development

| Resource | Name | Region |
| --- | --- | --- |
| Resource group | `rg-flowmova-dev` | `Canada Central` |
| Log Analytics workspace | `log-flowmova-dev` | `Canada Central` |
| Container Apps environment | `cae-flowmova-dev` | `Canada Central` |
| Backend Container App | `ca-flowmova-api-dev` | `Canada Central` |
| PostgreSQL Container App | `ca-flowmova-postgres-dev` | `Canada Central` |
| Storage account for dev PostgreSQL volume | `stflowmovadev001` | `Canada Central` |
| Azure Files share for dev PostgreSQL data | `flowmova-postgres-dev-data` | `Canada Central` |

### Production

| Resource | Name | Region |
| --- | --- | --- |
| Resource group | `rg-flowmova-prod` | `South Africa North` preferred |
| Log Analytics workspace | `log-flowmova-prod` | same as prod region |
| Container Apps environment | `cae-flowmova-prod` | same as prod region |
| Backend Container App | `ca-flowmova-api-prod` | same as prod region |
| Azure Database for PostgreSQL Flexible Server | `psql-flowmova-prod` | same as prod region |
| Storage account for Blob Storage | `stflowmovaprod001` | same as prod region |
| Blob container | `flowmova-prod` | same as prod region |

Production region priority:

1. `South Africa North`
2. Closest Azure region with all required services available, for example `UAE North`, `Qatar Central`, `North Europe`, or `West Europe`.

All production resources should stay in the same region unless a specific service is unavailable and the exception is documented.

## Development Environment

The development environment is used to validate cloud deployment, pipeline behavior, and integration with Azure resources.

Components:

- Resource group: `rg-flowmova-dev`
- Region: `Canada Central`
- Log Analytics workspace: `log-flowmova-dev`
- Azure Container Apps environment: `cae-flowmova-dev`
- Backend container app: `ca-flowmova-api-dev`
- PostgreSQL container app: `ca-flowmova-postgres-dev`
- Storage account for PostgreSQL dev data: `stflowmovadev001`
- Azure Files share for PostgreSQL dev data: `flowmova-postgres-dev-data`
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

- Resource group: `rg-flowmova-prod`
- Preferred region: `South Africa North`
- Log Analytics workspace: `log-flowmova-prod`
- Azure Container Apps environment: `cae-flowmova-prod`
- Backend container app: `ca-flowmova-api-prod`
- Azure Database for PostgreSQL Flexible Server: `psql-flowmova-prod`
- Storage account for Blob Storage: `stflowmovaprod001`
- Blob container: `flowmova-prod`
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
