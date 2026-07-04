# FlowMova Backend

Backend de la plateforme FlowMova.

FlowMova est une plateforme SaaS permettant a des entreprises d'organiser leurs activites de service: entreprises, utilisateurs, catalogues, unites de service, articles et tickets. Ce depot contient le backend applicatif charge d'exposer les API REST, d'appliquer les regles metier, de gerer la securite et de persister les donnees.

## Sources de verite

Les specifications fonctionnelles et techniques prioritaires sont:

1. `FS-002 - Gestion des utilisateurs et de l'authentification`
2. `FS-001 - Gestion des unites de service`
3. `DAT - Document d'Architecture Technique`

Le Product Blueprint est conserve comme document de contexte, mais il n'est plus la source prioritaire en cas d'ambiguite.

## Stack cible

- Java 21
- Spring Boot 3
- Spring Security
- JWT
- Spring Data JPA / Hibernate
- PostgreSQL
- Flyway
- OpenAPI / Swagger
- GitHub Actions
- GitHub Container Registry
- Azure Container Apps

## Architecture

Le backend suit une approche de monolithe modulaire. La premiere version doit rester simple tout en conservant des frontieres claires entre les domaines fonctionnels.

La structure des modules backend est documentee dans [docs/architecture/backend-modules.md](docs/architecture/backend-modules.md).

La strategie d'environnements Azure est documentee dans [docs/architecture/azure-environments.md](docs/architecture/azure-environments.md).

Modules cibles de demarrage:

- `auth`: inscription, connexion, JWT, reset password
- `user`: profil utilisateur et changement de mot de passe
- `company-access`: appartenance utilisateur-entreprise, roles et controles d'autorisation
- `shared`: erreurs API, validation, audit, configuration commune

## Perimetre de demarrage FS-002

Le premier objectif est d'implementer la gestion des utilisateurs et de l'authentification:

- creation de compte utilisateur
- authentification email / mot de passe
- generation d'un access token JWT
- securisation des endpoints proteges
- consultation et modification du profil utilisateur
- changement de mot de passe
- association d'un utilisateur a une entreprise
- roles `ADMIN` et `EMPLOYEE`
- verification des permissions par entreprise
- reset password avec token a usage unique
- journalisation minimale des operations sensibles

## Decisions MVP

- JWT access token uniquement pour le MVP
- duree du token: 12 heures
- pas de refresh token en MVP
- logout gere cote client par suppression du token
- contexte entreprise transmis par le header `X-Company-Id`
- mot de passe: minimum 8 caracteres
- hash des mots de passe avec BCrypt
- compte actif immediatement apres inscription
- ajout du champ `status` sur l'association utilisateur-entreprise
- reset password via token en base, expiration 30 minutes
- email reel de reset password reporte a une iteration ulterieure

## Etat du projet

Le depot est en phase d'initialisation. Le premier jalon consiste a mettre en place le socle Spring Boot, la base PostgreSQL, les migrations Flyway et la verticale minimale inscription -> connexion -> JWT -> endpoint protege.

## Demarrage local

Prerequis:

- JDK 21
- Maven 3.9+

Commandes utiles:

```bash
docker compose up -d
mvn test
mvn spring-boot:run
```

L'application demarre par defaut sur le port `8080`.

Variables d'environnement utiles:

- `FLOWMOVA_PUBLIC_BASE_URL`: URL publique de base utilisee pour construire les liens publics des emplacements. Valeur locale par defaut: `http://localhost:3000`.

PostgreSQL local est fourni par Docker Compose:

- host: `localhost`
- port: `5433`
- database: `flowmova`
- user: `flowmova`
- password: `flowmova_dev`

Pour arreter PostgreSQL local:

```bash
docker compose down
```
