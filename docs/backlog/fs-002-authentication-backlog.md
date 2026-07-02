# Backlog FS-002 - Utilisateurs et authentification

Ce backlog couvre le demarrage du backend FlowMova a partir de FS-002 et du DAT.

## Decisions MVP retenues

- JWT access token uniquement pour le MVP.
- Duree du token: 12 heures.
- Pas de refresh token en MVP.
- Logout gere cote client par suppression du token.
- Contexte entreprise transmis par le header `X-Company-Id`.
- Roles V1: `ADMIN`, `EMPLOYEE`.
- Ajout du champ `status` sur `company_users`.
- Mot de passe: minimum 8 caracteres.
- Hash des mots de passe avec BCrypt.
- Compte utilisateur actif immediatement apres inscription.
- Reset password via token en base, expiration 30 minutes.
- Envoi email reel reporte a une iteration ulterieure.

## Milestone 1 - Socle backend

### BE-001 - Initialiser le projet Spring Boot backend

**En tant que** developpeur,
**je veux** disposer d'un projet Spring Boot 3 configure en Java 21,
**afin de** demarrer le backend FlowMova sur une base conforme au DAT.

Critères d'acceptation:

- Le projet Spring Boot est present dans le depot.
- Java 21 est configure.
- L'application demarre localement.
- Un test de contexte Spring minimal passe.
- La structure initiale respecte l'approche monolithe modulaire.

### BE-002 - Configurer PostgreSQL et Flyway

**En tant que** developpeur,
**je veux** connecter le backend a PostgreSQL avec Flyway,
**afin de** versionner le schema de donnees des le debut du projet.

Critères d'acceptation:

- Une configuration PostgreSQL locale/dev existe.
- Flyway est configure.
- Les migrations sont executees au demarrage.
- Les parametres sensibles passent par variables d'environnement ou configuration locale non versionnee.

### BE-003 - Mettre en place la structure modulaire

**En tant que** developpeur,
**je veux** organiser le code par modules fonctionnels,
**afin de** respecter l'architecture monolithe modulaire du DAT.

Critères d'acceptation:

- Les packages `auth`, `user`, `companyaccess` et `shared` existent.
- Les responsabilites des modules sont documentees dans le code ou le README.
- Les classes communes sont placees dans `shared`.

### BE-004 - Standardiser les erreurs API

**En tant que** client API,
**je veux** recevoir des erreurs coherentes,
**afin de** faciliter l'integration Flutter et les tests.

Critères d'acceptation:

- Un format d'erreur API standard est defini.
- Les erreurs de validation retournent un statut HTTP coherent.
- Les erreurs techniques internes ne sont pas exposees aux clients.
- Un gestionnaire global d'exceptions est present.

## Milestone 2 - Modele de donnees FS-002

### AUTH-001 - Creer la table `users`

**En tant que** backend,
**je veux** persister les comptes utilisateurs,
**afin de** permettre l'inscription, l'authentification et la gestion du profil.

Critères d'acceptation:

- Une migration Flyway cree la table `users`.
- Les champs FS-002 sont representes: `id`, `email`, `password_hash`, `first_name`, `last_name`, `phone`, `profile_picture`, `preferred_language`, `status`, `created_at`, `updated_at`, `version`.
- `email` est unique.
- `status` est indexe.
- Le champ `version` est prevu pour l'optimistic locking.

### AUTH-002 - Creer la table `company_users`

**En tant que** backend,
**je veux** persister l'association entre utilisateur et entreprise,
**afin de** gerer les roles par entreprise.

Critères d'acceptation:

- Une migration Flyway cree la table `company_users`.
- Les champs sont presents: `id`, `company_id`, `user_id`, `role`, `status`, `created_at`, `updated_at`.
- Le couple `(company_id, user_id)` est unique.
- Les index recommandes par FS-002 sont presents.
- Les roles supportes sont `ADMIN` et `EMPLOYEE`.
- Les statuts supportes sont `ACTIVE` et `INACTIVE`.

### AUTH-003 - Creer les entites et repositories FS-002

**En tant que** developpeur,
**je veux** disposer des entites JPA et repositories,
**afin de** manipuler les utilisateurs et associations entreprise-utilisateur.

Critères d'acceptation:

- L'entite `User` existe.
- L'entite `CompanyUser` existe.
- Les enums `UserStatus`, `CompanyUserStatus` et `CompanyRole` existent.
- Les repositories exposent les recherches necessaires: par email, par utilisateur, par entreprise, par couple entreprise/utilisateur.

## Milestone 3 - Inscription

### AUTH-010 - Inscrire un utilisateur

**En tant que** visiteur,
**je veux** creer un compte FlowMova,
**afin de** pouvoir acceder aux fonctionnalites authentifiees.

Critères d'acceptation:

- L'endpoint `POST /api/auth/register` existe.
- L'email est obligatoire et unique.
- Le mot de passe est obligatoire et contient au moins 8 caracteres.
- Le mot de passe est hashe avec BCrypt.
- Le compte est cree avec le statut `ACTIVE`.
- La reponse ne contient jamais `passwordHash`.

### AUTH-011 - Tester l'inscription

**En tant que** developpeur,
**je veux** couvrir les cas d'inscription,
**afin de** proteger le comportement FS-002 contre les regressions.

Critères d'acceptation:

- Un test couvre l'inscription reussie.
- Un test couvre l'email deja utilise.
- Un test couvre un mot de passe invalide.
- Un test verifie que le hash n'est pas expose.

## Milestone 4 - Connexion JWT

### AUTH-020 - Connecter un utilisateur

**En tant que** utilisateur,
**je veux** me connecter avec mon email et mon mot de passe,
**afin de** recevoir un token d'acces a la plateforme.

Critères d'acceptation:

- L'endpoint `POST /api/auth/login` existe.
- Les identifiants sont verifies cote serveur.
- Un utilisateur `DISABLED` ne peut pas se connecter.
- La reponse contient `accessToken`, `tokenType` et `expiresIn`.
- Le token expire apres 12 heures.

### AUTH-021 - Generer et valider les JWT

**En tant que** backend,
**je veux** generer et valider des JWT,
**afin de** securiser les endpoints proteges.

Critères d'acceptation:

- Le JWT contient au minimum `userId` et `email`.
- La signature du token est verifiee a chaque requete protegee.
- Les endpoints publics restent accessibles sans token.
- Les endpoints proteges refusent les requetes sans JWT valide.

### AUTH-022 - Configurer Spring Security

**En tant que** developpeur,
**je veux** configurer Spring Security,
**afin de** centraliser l'authentification et la protection des API.

Critères d'acceptation:

- Spring Security est configure.
- Les endpoints `/api/auth/register` et `/api/auth/login` sont publics.
- Un filtre JWT authentifie les requetes protegees.
- Les tests verifient acces public, acces refuse sans token et acces autorise avec token valide.

## Milestone 5 - Profil utilisateur

### USER-001 - Consulter son profil

**En tant que** utilisateur authentifie,
**je veux** consulter mon profil,
**afin de** voir mes informations personnelles.

Critères d'acceptation:

- L'endpoint `GET /api/users/me` existe.
- L'endpoint necessite un JWT valide.
- La reponse contient les informations de profil autorisees.
- La reponse ne contient jamais `passwordHash`.

### USER-002 - Modifier son profil

**En tant que** utilisateur authentifie,
**je veux** modifier mes informations de profil,
**afin de** tenir mes informations a jour.

Critères d'acceptation:

- L'endpoint `PATCH /api/users/me` existe.
- Les champs modifiables sont `firstName`, `lastName`, `phone`, `preferredLanguage`, `profilePicture`.
- Les champs sensibles `email`, `status` et `passwordHash` ne sont pas modifiables via cet endpoint.
- `updated_at` est mis a jour.

### USER-003 - Changer son mot de passe

**En tant que** utilisateur authentifie,
**je veux** changer mon mot de passe,
**afin de** securiser mon compte.

Critères d'acceptation:

- L'endpoint `POST /api/users/me/change-password` existe.
- L'ancien mot de passe est obligatoire.
- L'ancien mot de passe doit etre valide.
- Le nouveau mot de passe respecte la politique MVP.
- Le nouveau mot de passe est hashe avec BCrypt.

## Milestone 6 - Appartenance entreprise et roles

### ACCESS-001 - Lister ses entreprises

**En tant que** utilisateur authentifie,
**je veux** consulter les entreprises auxquelles j'appartiens,
**afin de** choisir mon contexte de travail.

Critères d'acceptation:

- L'endpoint `GET /api/users/me/companies` existe.
- Seules les associations actives sont retournees par defaut.
- Le role de l'utilisateur dans chaque entreprise est visible.

### ACCESS-002 - Verifier les permissions par entreprise

**En tant que** backend,
**je veux** verifier les permissions avec le header `X-Company-Id`,
**afin de** garantir l'isolation des donnees entre entreprises.

Critères d'acceptation:

- Les operations entreprise utilisent `X-Company-Id` lorsque necessaire.
- Le backend verifie l'appartenance de l'utilisateur a l'entreprise.
- Une association absente ou inactive refuse l'acces.
- Le role `ADMIN` est requis pour les operations d'administration.

### ACCESS-003 - Gerer les utilisateurs d'une entreprise

**En tant que** administrateur d'entreprise,
**je veux** associer, lister, modifier le role et retirer des utilisateurs,
**afin de** administrer l'acces a mon entreprise.

Critères d'acceptation:

- Un administrateur peut lister les utilisateurs de son entreprise.
- Un administrateur peut associer un utilisateur existant par email.
- Un administrateur peut changer le role d'un utilisateur.
- Un administrateur peut retirer ou desactiver une association.
- Un employe ne peut pas administrer les utilisateurs.

### ACCESS-004 - Proteger le dernier administrateur

**En tant que** plateforme,
**je veux** empecher la suppression du dernier administrateur,
**afin de** garantir qu'une entreprise reste administrable.

Critères d'acceptation:

- Le dernier administrateur ne peut pas etre retire.
- Le dernier administrateur ne peut pas etre retrograde en employe.
- Les tentatives refusees ne modifient pas les donnees.
- Les tests couvrent retrait et changement de role du dernier administrateur.

## Milestone 7 - Reset password

### RESET-001 - Demander une reinitialisation de mot de passe

**En tant que** utilisateur,
**je veux** demander une reinitialisation de mot de passe,
**afin de** recuperer l'acces a mon compte.

Critères d'acceptation:

- L'endpoint `POST /api/auth/password-reset/request` existe.
- Un token a usage unique est cree si l'email correspond a un compte.
- Le token expire apres 30 minutes.
- La reponse ne revele jamais si l'email existe.
- Une interface `EmailService` est prevue, meme si l'envoi reel est reporte.

### RESET-002 - Confirmer une reinitialisation de mot de passe

**En tant que** utilisateur,
**je veux** confirmer mon nouveau mot de passe avec un token valide,
**afin de** retrouver l'acces a mon compte.

Critères d'acceptation:

- L'endpoint `POST /api/auth/password-reset/confirm` existe.
- Le token doit exister, ne pas etre expire et ne pas avoir ete utilise.
- Le nouveau mot de passe respecte la politique MVP.
- Le mot de passe est hashe avec BCrypt.
- Le token est invalide apres utilisation.

## Milestone 8 - Audit et documentation

### SEC-001 - Journaliser les operations sensibles

**En tant que** plateforme,
**je veux** journaliser les operations sensibles,
**afin de** assurer la tracabilite demandee par FS-002.

Critères d'acceptation:

- Un service d'audit minimal existe.
- Les evenements suivants sont journalises: creation de compte, modification profil, changement mot de passe, reset password, association utilisateur-entreprise, changement de role, retrait d'utilisateur.
- Les journaux n'exposent pas de mot de passe, token ou secret.

### DOC-001 - Publier la documentation OpenAPI

**En tant que** developpeur frontend,
**je veux** consulter la documentation OpenAPI,
**afin de** integrer les endpoints backend plus facilement.

Critères d'acceptation:

- OpenAPI / Swagger est configure.
- Les endpoints auth sont documentes.
- Les endpoints profil utilisateur sont documentes.
- Les endpoints entreprise-utilisateur sont documentes.
- Les DTO principaux possedent des descriptions lisibles.

## Premiere verticale recommandee

Pour obtenir rapidement une base utilisable, traiter dans cet ordre:

1. BE-001
2. BE-002
3. BE-003
4. AUTH-001
5. AUTH-003
6. AUTH-010
7. AUTH-020
8. AUTH-021
9. AUTH-022

Cette verticale permet d'obtenir: inscription -> connexion -> JWT -> endpoint protege.
