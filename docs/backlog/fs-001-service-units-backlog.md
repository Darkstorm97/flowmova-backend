# Backlog FS-001 - Gestion des unites de service

Ce backlog couvre la fonctionnalite FS-001 "Gestion des unites de service".

Il est construit a partir de FS-001, FS-002 et du DAT. En cas d'ambiguite, les Feature Specifications et le DAT sont prioritaires sur le Product Blueprint.

## Decisions MVP retenues

- Une entreprise represente une organisation utilisant FlowMova.
- Il n'y a pas de concept d'entreprise publique/privee dans le MVP.
- Une entreprise `ACTIVE` est visible par les utilisateurs, authentifies ou non.
- Une entreprise non active n'est pas visible publiquement.
- Les utilisateurs peuvent rechercher et consulter les entreprises actives.
- La fiche entreprise affiche les informations de l'entreprise, ses catalogues actifs classes par categories et ses unites de service disponibles.
- Une entreprise peut posseder plusieurs categories de catalogues.
- Une entreprise peut posseder plusieurs catalogues.
- Les categories servent uniquement a organiser les catalogues.
- Les catalogues representent les offres de reference de l'entreprise.
- Les articles representent les offres effectivement disponibles dans une unite de service.
- Les articles appartiennent a une unite de service et sont issus d'un catalogue.
- Dans le MVP, les catalogues n'ont pas de visibilite publique independante.
- Un catalogue actif est consultable si son entreprise est active.
- Le MVP privilegie l'archivage/desactivation logique plutot que la suppression physique.
- Une unite de service represente une file de prise en charge par tickets.
- Le seul type d'unite de service supporte dans le MVP est `TICKET_QUEUE`.
- Les autres modes de fonctionnement comme reservation, commande, livraison ou drive sont hors perimetre MVP.
- Une unite de service possede un lien public stable permettant d'y acceder directement.
- Le backend fournit l'identifiant ou l'URL d'acces public de l'unite, mais ne genere pas d'image QR code.
- La generation et l'affichage du QR code sont hors backend et pourront etre geres cote frontend ou par un outil externe.
- Une unite de service est creee avec le statut `CLOSED`.
- Une unite de service peut etre creee sans article.
- Une unite de service peut etre ouverte sans article.
- Les statuts MVP d'une unite de service sont `CLOSED`, `OPEN`, `ARCHIVED`.
- Seules les unites `OPEN` sont visibles publiquement.
- Seules les unites `OPEN` acceptent la creation de tickets.
- Les administrateurs voient les unites `OPEN`, `CLOSED` et `ARCHIVED` de leur entreprise.
- Une unite de service peut etre ouverte uniquement si elle est correctement configuree.
- La configuration minimale d'ouverture est: entreprise active, nom renseigne, type `TICKET_QUEUE`, statut actuel `CLOSED`.
- Les articles d'une unite sont optionnels. S'ils existent et sont disponibles, ils sont affiches; sinon l'utilisateur peut creer un ticket general.
- Un ticket peut contenir zero, une ou plusieurs lignes de ticket.
- Un ticket avec zero ligne permet une prise en charge generale.
- Le numero de ticket est genere automatiquement.
- Le numero de ticket est unique par unite de service.
- Les utilisateurs autorises de l'entreprise peuvent gerer les tickets.
- Dans le MVP, les roles `ADMIN` et `EMPLOYEE` peuvent gerer les tickets.
- Dans le MVP, seul le role `ADMIN` gere l'entreprise, les categories, les catalogues, les unites de service et les articles.
- Le createur authentifie peut annuler son propre ticket si le ticket est associe a son compte et si la transition est valide.
- Un visiteur non authentifie ne peut pas annuler lui-meme son ticket dans le MVP.
- Une annulation autonome par visiteur non authentifie necessitera un mecanisme dedie et une validation FS avant developpement.

## Ambiguites resolues

- Le Product Blueprint evoque un catalogue unique, mais FS-001 detaille plusieurs categories et plusieurs catalogues. Decision retenue: plusieurs catalogues classes par categories.
- FS-001 evoque la visibilite publique/privee des entreprises. Decision MVP retenue: pas de visibilite publique/privee, seules les entreprises `ACTIVE` sont visibles.
- FS-001 evoque plusieurs modes de fonctionnement possibles. Decision MVP retenue: uniquement `TICKET_QUEUE`.
- FS-001 evoque l'annulation de ticket sans preciser tous les acteurs. Decision retenue: entreprise autorisee et createur authentifie seulement.
- La regle d'ouverture initiale exigeait au moins un article disponible. Decision corrigee: les articles sont optionnels et une unite peut etre ouverte sans article pour permettre les tickets generaux.
- Le besoin de QR code est retenu sous forme de lien public stable. Decision retenue: le backend ne genere pas d'image QR code.

## Milestone 1 - Entreprises

### COMPANY-001 - Creer la table `companies`

**En tant que** backend,
**je veux** persister les entreprises,
**afin de** disposer de la racine fonctionnelle FS-001.

Criteres d'acceptation:

- Une migration Flyway cree la table `companies`.
- Les champs MVP sont presents: `id`, `name`, `description`, `status`, `created_at`, `updated_at`, `created_by`, `updated_by`, `version`.
- Le statut supporte au MVP permet au minimum `ACTIVE` et `DISABLED`.
- Le champ `visibility` n'est pas ajoute dans le MVP.
- Les index necessaires aux recherches par statut et nom sont presents.

### COMPANY-002 - Creer entite et repository Company

**En tant que** developpeur,
**je veux** disposer d'une entite JPA et d'un repository Company,
**afin de** manipuler les entreprises dans le backend.

Criteres d'acceptation:

- L'entite `Company` existe.
- L'enum `CompanyStatus` existe.
- Le repository permet de rechercher par id, statut et nom.
- Le champ `version` est mappe pour l'optimistic locking.
- Les dates de creation et modification sont gerees.

### COMPANY-010 - Creer une entreprise

**En tant que** utilisateur authentifie,
**je veux** creer une entreprise,
**afin de** administrer mes activites de service dans FlowMova.

Criteres d'acceptation:

- L'endpoint de creation d'entreprise existe.
- L'utilisateur doit etre authentifie.
- Le nom de l'entreprise est obligatoire.
- L'entreprise est creee avec le statut `ACTIVE`.
- Le createur est associe automatiquement a l'entreprise avec le role `ADMIN`.
- La creation est transactionnelle: entreprise et association administrateur sont creees ensemble.

### COMPANY-011 - Associer le createur comme administrateur

**En tant que** plateforme,
**je veux** associer automatiquement le createur d'une entreprise comme administrateur,
**afin de** garantir que l'entreprise reste administrable.

Criteres d'acceptation:

- Une ligne `company_users` est creee pour le createur.
- Le role du createur est `ADMIN`.
- Le statut de l'association est `ACTIVE`.
- Une entreprise creee possede toujours au moins un administrateur.
- Les tests couvrent l'association automatique.

### COMPANY-020 - Consulter mes entreprises

**En tant que** utilisateur authentifie,
**je veux** consulter les entreprises auxquelles j'appartiens,
**afin de** choisir mon contexte de travail.

Criteres d'acceptation:

- L'endpoint retourne les entreprises liees a l'utilisateur authentifie.
- Le role de l'utilisateur dans chaque entreprise est retourne.
- Les associations inactives ne sont pas retournees par defaut.
- Les donnees d'autres utilisateurs ne sont pas exposees.

### COMPANY-030 - Rechercher les entreprises actives

**En tant que** utilisateur,
**je veux** rechercher les entreprises actives,
**afin de** decouvrir les organisations disponibles sur FlowMova.

Criteres d'acceptation:

- L'endpoint de recherche est public.
- Seules les entreprises `ACTIVE` sont retournees.
- Les entreprises non actives sont exclues.
- La recherche supporte au minimum un filtre texte sur le nom.
- Les resultats sont pagines.

### COMPANY-031 - Consulter une fiche entreprise active

**En tant que** utilisateur,
**je veux** consulter la fiche d'une entreprise active,
**afin de** voir ses informations, ses catalogues et ses unites disponibles.

Criteres d'acceptation:

- L'endpoint de consultation est public.
- Une entreprise `ACTIVE` peut etre consultee.
- Une entreprise non active retourne une erreur ou n'est pas exposee.
- La reponse contient les informations publiques de l'entreprise.
- La reponse prepare l'affichage des catalogues actifs classes par categories et des unites ouvertes.

## Milestone 2 - Categories de catalogues

### CATCAT-001 - Creer la table `catalog_categories`

**En tant que** backend,
**je veux** persister les categories de catalogues,
**afin de** organiser les catalogues d'une entreprise.

Criteres d'acceptation:

- Une migration Flyway cree la table `catalog_categories`.
- Les champs sont presents: `id`, `company_id`, `name`, `description`, `display_order`, `status`, `created_at`, `updated_at`, `created_by`, `updated_by`, `version`.
- Une categorie appartient obligatoirement a une entreprise.
- Le nom est unique au sein d'une meme entreprise.
- Les index recommandes sont presents: `company_id`, `(company_id, name)`.

### CATCAT-002 - Creer entite et repository CatalogCategory

**En tant que** developpeur,
**je veux** disposer d'une entite et d'un repository pour les categories,
**afin de** gerer l'organisation des catalogues.

Criteres d'acceptation:

- L'entite `CatalogCategory` existe.
- L'enum de statut de categorie existe.
- Le repository permet de lister les categories par entreprise.
- Le repository permet de verifier l'unicite du nom par entreprise.

### CATCAT-010 - Creer une categorie de catalogue

**En tant que** administrateur d'entreprise,
**je veux** creer une categorie de catalogue,
**afin de** classer les catalogues de mon entreprise.

Criteres d'acceptation:

- L'endpoint de creation existe.
- Le role `ADMIN` est requis pour l'entreprise.
- Le nom est obligatoire.
- Le nom est unique dans l'entreprise.
- La categorie est creee avec le statut `ACTIVE`.

### CATCAT-020 - Lister les categories d'une entreprise

**En tant que** administrateur d'entreprise,
**je veux** lister les categories de catalogue,
**afin de** administrer l'organisation de mes catalogues.

Criteres d'acceptation:

- L'endpoint de liste existe.
- Les categories sont filtrees par entreprise.
- Les categories sont triees par `display_order`, puis par nom.
- Les utilisateurs non autorises ne peuvent pas lister les categories internes d'une entreprise.

### CATCAT-030 - Archiver une categorie de catalogue

**En tant que** administrateur d'entreprise,
**je veux** archiver une categorie,
**afin de** la retirer de l'usage courant sans supprimer l'historique.

Criteres d'acceptation:

- Le role `ADMIN` est requis.
- Le statut de la categorie passe a `ARCHIVED`.
- Les catalogues rattaches ne sont pas supprimes.
- Une categorie archivee n'est pas visible dans les listes publiques.

## Milestone 3 - Catalogues

### CATALOG-001 - Creer la table `catalogs`

**En tant que** backend,
**je veux** persister les catalogues,
**afin de** representer les offres de reference des entreprises.

Criteres d'acceptation:

- Une migration Flyway cree la table `catalogs`.
- Les champs sont presents: `id`, `company_id`, `catalog_category_id`, `name`, `description`, `image_url`, `status`, `created_at`, `updated_at`, `created_by`, `updated_by`, `version`.
- Un catalogue appartient obligatoirement a une entreprise.
- Un catalogue appartient obligatoirement a une categorie.
- La categorie doit appartenir a la meme entreprise que le catalogue.
- Les index necessaires sont presents: `company_id`, `catalog_category_id`, `status`.

### CATALOG-002 - Creer entite et repository Catalog

**En tant que** developpeur,
**je veux** disposer d'une entite et d'un repository Catalog,
**afin de** manipuler les offres de reference.

Criteres d'acceptation:

- L'entite `Catalog` existe.
- L'enum de statut de catalogue existe.
- Le repository permet de lister les catalogues par entreprise et par categorie.
- Le repository permet de filtrer les catalogues actifs.

### CATALOG-010 - Creer un catalogue

**En tant que** administrateur d'entreprise,
**je veux** creer un catalogue,
**afin de** definir une offre de reference de mon entreprise.

Criteres d'acceptation:

- L'endpoint de creation existe.
- Le role `ADMIN` est requis.
- Le nom est obligatoire.
- La categorie est obligatoire.
- La categorie doit appartenir a la meme entreprise.
- Le catalogue est cree avec le statut `ACTIVE`.

### CATALOG-020 - Lister les catalogues par categorie

**En tant que** utilisateur,
**je veux** consulter les catalogues actifs classes par categories,
**afin de** comprendre l'offre d'une entreprise active.

Criteres d'acceptation:

- Les catalogues actifs d'une entreprise active sont consultables.
- Les catalogues sont groupes ou filtrables par categorie.
- Les catalogues archives ne sont pas exposes publiquement.
- Les catalogues d'une entreprise non active ne sont pas exposes publiquement.

### CATALOG-030 - Modifier un catalogue

**En tant que** administrateur d'entreprise,
**je veux** modifier un catalogue,
**afin de** tenir a jour les informations de reference.

Criteres d'acceptation:

- Le role `ADMIN` est requis.
- Le nom, la description, l'image et la categorie peuvent etre modifies.
- La nouvelle categorie doit appartenir a la meme entreprise.
- Les articles issus du catalogue conservent leur reference au catalogue modifie.
- `updated_at` et `version` sont mis a jour.

### CATALOG-040 - Archiver un catalogue

**En tant que** administrateur d'entreprise,
**je veux** archiver un catalogue,
**afin de** le retirer de l'offre courante sans supprimer l'historique.

Criteres d'acceptation:

- Le role `ADMIN` est requis.
- Le statut du catalogue passe a `ARCHIVED`.
- Le catalogue archive n'est plus visible publiquement.
- L'archivage ne supprime pas les articles ou tickets existants.

## Milestone 4 - Unites de service

### SERVICE-001 - Creer la table `service_units`

**En tant que** backend,
**je veux** persister les unites de service,
**afin de** representer les points de prise en charge d'une entreprise.

Criteres d'acceptation:

- Une migration Flyway cree la table `service_units`.
- Les champs sont presents: `id`, `company_id`, `name`, `description`, `location`, `type`, `status`, `public_access_slug`, `settings`, `created_at`, `updated_at`, `created_by`, `updated_by`, `version`.
- Le seul type supporte au MVP est `TICKET_QUEUE`.
- Les statuts supportes sont `CLOSED`, `OPEN`, `ARCHIVED`.
- Une unite appartient obligatoirement a une entreprise.
- `public_access_slug` est unique et stable.
- Les index necessaires sont presents: `company_id`, `status`, `type`, `public_access_slug`.

### SERVICE-002 - Creer entite et repository ServiceUnit

**En tant que** developpeur,
**je veux** disposer d'une entite et d'un repository ServiceUnit,
**afin de** gerer les points de prise en charge.

Criteres d'acceptation:

- L'entite `ServiceUnit` existe.
- Les enums `ServiceUnitType` et `ServiceUnitStatus` existent.
- Le repository permet de lister les unites par entreprise.
- Le repository permet de lister les unites `OPEN` d'une entreprise active.

### SERVICE-010 - Creer une unite de service

**En tant que** administrateur d'entreprise,
**je veux** creer une unite de service,
**afin de** preparer un point de prise en charge.

Criteres d'acceptation:

- L'endpoint de creation existe.
- Le role `ADMIN` est requis.
- Le nom est obligatoire.
- Le type est `TICKET_QUEUE`.
- L'unite est creee avec le statut `CLOSED`.
- L'unite appartient a l'entreprise selectionnee.
- Un `public_access_slug` unique est genere automatiquement.
- L'unite peut etre creee sans article.

### SERVICE-011 - Consulter le lien public d'une unite de service

**En tant que** administrateur d'entreprise,
**je veux** obtenir le lien public stable d'une unite de service,
**afin de** le partager ou de l'utiliser pour generer un QR code hors backend.

Criteres d'acceptation:

- Le role `ADMIN` est requis.
- La reponse contient l'identifiant public ou l'URL publique de l'unite.
- Le backend ne genere pas d'image QR code.
- Le backend ne stocke pas d'image QR code.
- Le lien reste stable tant que l'unite existe.

### ITEM-001 - Creer la table `items`

**En tant que** backend,
**je veux** persister les articles disponibles dans une unite de service,
**afin de** representer les catalogues rendus operationnels dans cette unite.

Criteres d'acceptation:

- Une migration Flyway cree la table `items`.
- Les champs sont presents: `id`, `service_unit_id`, `catalog_id`, `availability`, `configured_quantity`, `reserved_quantity`, `display_order`, `status`, `created_at`, `updated_at`, `version`.
- Un article appartient obligatoirement a une unite de service.
- Un article reference obligatoirement un catalogue.
- Le catalogue doit appartenir a la meme entreprise que l'unite.
- Les index necessaires sont presents: `service_unit_id`, `catalog_id`, `status`, `availability`.

### ITEM-002 - Creer entite et repository Item

**En tant que** developpeur,
**je veux** disposer d'une entite et d'un repository Item,
**afin de** gerer les offres disponibles dans les unites de service.

Criteres d'acceptation:

- L'entite `Item` existe.
- Les enums de statut et disponibilite existent.
- Le repository permet de lister les articles par unite.
- Le repository permet de lister les articles actifs et disponibles d'une unite.

### SERVICE-020 - Associer un catalogue a une unite de service

**En tant que** administrateur d'entreprise,
**je veux** associer un catalogue a une unite de service,
**afin de** rendre cette offre disponible dans l'unite.

Criteres d'acceptation:

- Le role `ADMIN` est requis.
- Le catalogue doit appartenir a la meme entreprise que l'unite.
- L'association cree un article dans l'unite.
- Le meme catalogue ne peut pas etre associe deux fois a la meme unite.
- L'article cree peut avoir ses propres parametres: disponibilite, quantite configuree, ordre d'affichage.

### ITEM-010 - Configurer un article

**En tant que** administrateur d'entreprise,
**je veux** configurer un article d'une unite,
**afin de** controler sa disponibilite operationnelle.

Criteres d'acceptation:

- Le role `ADMIN` est requis.
- La disponibilite peut etre modifiee.
- La quantite configuree peut etre modifiee.
- L'ordre d'affichage peut etre modifie.
- La quantite reservee reste calculee et n'est pas modifiee directement.

### SERVICE-030 - Ouvrir une unite de service

**En tant que** administrateur d'entreprise,
**je veux** ouvrir une unite de service,
**afin de** permettre aux utilisateurs de creer des tickets.

Criteres d'acceptation:

- Le role `ADMIN` est requis.
- L'entreprise de l'unite doit etre `ACTIVE`.
- L'unite doit etre `CLOSED`.
- L'unite peut etre ouverte meme si elle ne possede aucun article.
- Le statut de l'unite passe a `OPEN`.
- Une unite `ARCHIVED` ne peut pas etre ouverte.

### SERVICE-031 - Fermer une unite de service

**En tant que** administrateur d'entreprise,
**je veux** fermer une unite de service,
**afin de** empecher la creation de nouveaux tickets.

Criteres d'acceptation:

- Le role `ADMIN` est requis.
- Une unite `OPEN` peut passer a `CLOSED`.
- Une unite `CLOSED` n'accepte pas de nouveaux tickets.
- Les tickets existants ne sont pas supprimes.

### SERVICE-032 - Archiver une unite de service

**En tant que** administrateur d'entreprise,
**je veux** archiver une unite de service,
**afin de** la retirer de l'usage courant sans perdre l'historique.

Criteres d'acceptation:

- Le role `ADMIN` est requis.
- Une unite `OPEN` ou `CLOSED` peut passer a `ARCHIVED`.
- Une unite `ARCHIVED` n'est pas visible publiquement.
- Une unite `ARCHIVED` n'accepte pas de tickets.
- Les tickets existants restent consultables par les utilisateurs autorises.

### SERVICE-040 - Consulter les unites ouvertes d'une entreprise

**En tant que** utilisateur,
**je veux** consulter les unites ouvertes d'une entreprise active,
**afin de** choisir une prise en charge disponible.

Criteres d'acceptation:

- L'endpoint public liste uniquement les unites `OPEN`.
- L'entreprise doit etre `ACTIVE`.
- Les unites `CLOSED` et `ARCHIVED` ne sont pas exposees publiquement.
- La reponse contient les informations necessaires a l'affichage.

### SERVICE-041 - Consulter une unite ouverte

**En tant que** utilisateur,
**je veux** consulter une unite ouverte,
**afin de** voir les informations de l'unite et creer un ticket.

Criteres d'acceptation:

- L'endpoint public retourne une unite `OPEN`.
- L'entreprise de l'unite doit etre `ACTIVE`.
- Les articles actifs/disponibles sont retournes lorsqu'ils existent.
- Une unite sans article reste consultable publiquement.
- Les articles non disponibles ou archives ne sont pas exposes publiquement.
- La reponse permet la creation d'un ticket general sans ligne de ticket.

### SERVICE-042 - Acceder a une unite par lien public

**En tant que** utilisateur,
**je veux** acceder directement a une unite de service avec son lien public,
**afin de** creer rapidement un ticket apres avoir recu un lien ou scanne un QR code.

Criteres d'acceptation:

- L'endpoint public permet de retrouver une unite par `public_access_slug`.
- L'unite doit etre `OPEN`.
- L'entreprise de l'unite doit etre `ACTIVE`.
- Une unite `CLOSED` ou `ARCHIVED` n'est pas exposee par le lien public.
- Le backend retourne les informations necessaires a l'affichage de l'unite.
- Le backend ne genere pas d'image QR code.

## Milestone 5 - Tickets

### TICKET-001 - Creer les tables `tickets` et `ticket_lines`

**En tant que** backend,
**je veux** persister les tickets et leurs lignes,
**afin de** suivre les demandes de prise en charge.

Criteres d'acceptation:

- Une migration Flyway cree la table `tickets`.
- Une migration Flyway cree la table `ticket_lines`.
- `tickets` contient au minimum: `id`, `number`, `user_id`, `guest_name`, `service_unit_id`, `status`, `notes`, `created_at`, `updated_at`, `closed_at`, `version`.
- `ticket_lines` contient au minimum: `id`, `ticket_id`, `item_id`, `quantity`, `notes`.
- Le numero de ticket est unique par unite de service.
- Une ligne de ticket reference un article appartenant a la meme unite que le ticket.
- Les index necessaires sont presents: `service_unit_id`, `user_id`, `status`, `(service_unit_id, number)`.

### TICKET-002 - Creer entites et repositories Ticket

**En tant que** developpeur,
**je veux** disposer des entites et repositories Ticket,
**afin de** manipuler les demandes de prise en charge.

Criteres d'acceptation:

- Les entites `Ticket` et `TicketLine` existent.
- L'enum `TicketStatus` existe avec `CREATED`, `CONFIRMED`, `CALLED`, `IN_PROGRESS`, `COMPLETED`, `CLOSED`, `CANCELLED`.
- Les repositories permettent de lister les tickets par unite, utilisateur et statut.
- Les transitions de statut sont preparees cote domaine ou service applicatif.

### TICKET-010 - Creer un ticket non authentifie

**En tant que** visiteur non authentifie,
**je veux** creer un ticket dans une unite ouverte,
**afin de** demander une prise en charge sans compte FlowMova.

Criteres d'acceptation:

- L'endpoint de creation publique existe.
- L'unite doit etre `OPEN`.
- L'entreprise de l'unite doit etre `ACTIVE`.
- Le ticket peut contenir zero, une ou plusieurs lignes.
- Le nom libre du visiteur peut etre renseigne.
- Le ticket n'est pas associe a un `user_id`.
- Le ticket est cree avec le statut `CREATED`.
- Un numero unique par unite est genere.

### TICKET-011 - Creer un ticket authentifie

**En tant que** utilisateur authentifie,
**je veux** creer un ticket associe a mon compte,
**afin de** suivre ma demande de prise en charge.

Criteres d'acceptation:

- L'endpoint accepte un utilisateur authentifie.
- L'unite doit etre `OPEN`.
- L'entreprise de l'unite doit etre `ACTIVE`.
- Le ticket est associe au `user_id` authentifie.
- Le ticket peut contenir zero, une ou plusieurs lignes.
- Le ticket est cree avec le statut `CREATED`.
- Un numero unique par unite est genere.

### TICKET-020 - Consulter les tickets d'une unite

**En tant que** utilisateur autorise de l'entreprise,
**je veux** consulter les tickets d'une unite,
**afin de** suivre les demandes de prise en charge.

Criteres d'acceptation:

- L'utilisateur doit appartenir a l'entreprise de l'unite.
- Les roles `ADMIN` et `EMPLOYEE` peuvent consulter les tickets.
- Les tickets sont filtres par unite.
- Les tickets peuvent etre filtres par statut.
- Les donnees d'une autre entreprise ne sont pas exposees.

### TICKET-021 - Consulter mes tickets

**En tant que** utilisateur authentifie,
**je veux** consulter mes tickets,
**afin de** suivre mes propres demandes.

Criteres d'acceptation:

- L'endpoint retourne uniquement les tickets associes au compte authentifie.
- Les tickets non authentifies ne sont pas retournes.
- Les tickets peuvent etre filtres par statut.
- Les informations sensibles internes de l'entreprise ne sont pas exposees.

### TICKET-030 - Changer l'etat d'un ticket

**En tant que** utilisateur autorise de l'entreprise,
**je veux** changer l'etat d'un ticket,
**afin de** faire avancer son cycle de vie.

Criteres d'acceptation:

- Les roles `ADMIN` et `EMPLOYEE` peuvent changer l'etat d'un ticket.
- Les transitions invalides sont refusees.
- Les transitions supportees couvrent: confirmer, appeler, demarrer, terminer, cloturer, annuler.
- Les changements d'etat respectent le cycle de vie FS-001.
- Les changements importants sont journalises.

### TICKET-031 - Annuler son propre ticket authentifie

**En tant que** utilisateur authentifie,
**je veux** annuler mon propre ticket,
**afin de** retirer ma demande de prise en charge lorsque c'est encore possible.

Criteres d'acceptation:

- L'utilisateur doit etre authentifie.
- Le ticket doit etre associe au compte authentifie.
- La transition vers `CANCELLED` doit etre valide.
- Un utilisateur ne peut pas annuler le ticket d'un autre utilisateur.
- Un ticket deja finalise ne peut pas etre annule si le cycle de vie l'interdit.

### TICKET-032 - Refuser l'annulation non authentifiee

**En tant que** plateforme,
**je veux** refuser l'annulation autonome par un visiteur non authentifie,
**afin de** eviter une modification sans identification fiable.

Criteres d'acceptation:

- Un ticket sans `user_id` ne peut pas etre annule par l'interface publique MVP.
- Une tentative d'annulation non authentifiee retourne une erreur coherente.
- L'annulation par utilisateur autorise de l'entreprise reste possible.
- Aucun mecanisme de lien ou jeton public n'est introduit dans le MVP.

### TICKET-040 - Cloturer un ticket

**En tant que** utilisateur autorise de l'entreprise,
**je veux** cloturer un ticket,
**afin de** terminer son cycle de prise en charge.

Criteres d'acceptation:

- Les roles `ADMIN` et `EMPLOYEE` peuvent cloturer un ticket.
- La transition doit respecter le cycle de vie.
- `closed_at` est renseigne a la cloture.
- Un ticket cloture ne peut plus etre modifie librement.

## Milestone 6 - Consultation publique integree

### PUBLIC-001 - Rechercher et consulter le parcours public entreprise

**En tant que** utilisateur,
**je veux** rechercher une entreprise active et consulter sa fiche,
**afin de** acceder aux catalogues et unites de service disponibles.

Criteres d'acceptation:

- La recherche retourne uniquement les entreprises `ACTIVE`.
- La fiche entreprise retourne les catalogues actifs classes par categories.
- La fiche entreprise retourne les unites `OPEN`.
- Les entites archivees ou non actives ne sont pas exposees.
- Le parcours ne requiert pas d'authentification.

### PUBLIC-002 - Consulter le detail public d'une unite de service

**En tant que** utilisateur,
**je veux** consulter le detail d'une unite ouverte,
**afin de** voir ses informations, ses articles eventuels et creer un ticket.

Criteres d'acceptation:

- L'unite doit etre `OPEN`.
- L'entreprise doit etre `ACTIVE`.
- Les articles actifs/disponibles sont affiches lorsqu'ils existent.
- Une unite sans article permet la creation d'un ticket general.
- Les informations necessaires a la creation d'un ticket sont presentes.
- Le parcours ne requiert pas d'authentification.

## Milestone 7 - Audit, validations et documentation

### FS001-VAL-001 - Standardiser les validations FS-001

**En tant que** backend,
**je veux** centraliser les validations FS-001,
**afin de** garantir la coherence des relations entre entreprise, categories, catalogues, unites, articles et tickets.

Criteres d'acceptation:

- Les validations empechent les relations entre entites de differentes entreprises.
- Les validations empechent la creation de tickets dans une unite non ouverte.
- Les validations empechent l'ouverture d'une unite non configuree.
- Les erreurs retournent le format API standard.

### FS001-AUD-001 - Journaliser les operations importantes FS-001

**En tant que** plateforme,
**je veux** journaliser les operations importantes FS-001,
**afin de** assurer la tracabilite fonctionnelle.

Criteres d'acceptation:

- Les creations/modifications/archivages d'entreprise, categories, catalogues et unites sont journalises.
- Les ouvertures et fermetures d'unites sont journalisees.
- Les creations et changements d'etat de tickets sont journalises.
- Les journaux n'exposent pas d'informations sensibles inutiles.

### FS001-DOC-001 - Documenter les endpoints FS-001 dans OpenAPI

**En tant que** developpeur frontend,
**je veux** disposer d'une documentation OpenAPI des endpoints FS-001,
**afin de** integrer les parcours entreprise, catalogues, unites et tickets.

Criteres d'acceptation:

- Les endpoints entreprise sont documentes.
- Les endpoints categories et catalogues sont documentes.
- Les endpoints unites de service et articles sont documentes.
- Les endpoints tickets sont documentes.
- Les erreurs principales sont documentees.

## Premiere verticale recommandee

Pour obtenir rapidement un parcours FS-001 utilisable, traiter dans cet ordre:

1. COMPANY-001
2. COMPANY-002
3. COMPANY-010
4. COMPANY-011
5. COMPANY-030
6. COMPANY-031
7. CATCAT-001
8. CATCAT-002
9. CATCAT-010
10. CATALOG-001
11. CATALOG-002
12. CATALOG-010
13. CATALOG-020
14. SERVICE-001
15. SERVICE-002
16. SERVICE-010
17. SERVICE-011
18. SERVICE-030
19. SERVICE-040
20. SERVICE-041
21. SERVICE-042
22. TICKET-001
23. TICKET-002
24. TICKET-010

Cette verticale permet d'obtenir: creation entreprise -> catalogue -> unite ouverte sans article obligatoire -> consultation publique par fiche ou lien direct -> creation ticket general.

Les issues `ITEM-001`, `ITEM-002`, `SERVICE-020` et `ITEM-010` peuvent ensuite enrichir l'unite avec des articles disponibles.
