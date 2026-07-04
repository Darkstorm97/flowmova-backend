# Backlog FS-001 - Gestion des unites de service

Ce backlog couvre la fonctionnalite FS-001 "Gestion des unites de service".

Il est construit a partir de FS-001, FS-002 et du DAT. En cas d'ambiguite, les Feature Specifications et le DAT sont prioritaires sur le Product Blueprint.

## Decisions MVP retenues

- Une entreprise represente une organisation utilisant FlowMova.
- Il n'y a pas de concept d'entreprise publique/privee dans le MVP.
- Une entreprise `ACTIVE` est visible par les utilisateurs, authentifies ou non.
- Une entreprise non active n'est pas visible publiquement.
- Une entreprise possede une devise (`currency`) au format ISO 4217 sur 3 lettres, par defaut `CAD` dans le MVP.
- La devise de l'entreprise est la devise de reference pour les prix de ses catalogues, articles et tickets.
- Les utilisateurs peuvent rechercher et consulter les entreprises actives.
- La fiche entreprise affiche les informations de l'entreprise, ses catalogues actifs classes par categories et ses unites de service disponibles.
- Une entreprise peut posseder plusieurs categories de catalogues.
- Une entreprise peut posseder plusieurs catalogues.
- Les categories servent uniquement a organiser les catalogues.
- Les catalogues representent les offres de reference de l'entreprise.
- Un catalogue peut porter un prix indicatif optionnel dans la devise de l'entreprise.
- Les articles representent les offres effectivement disponibles dans une unite de service.
- Les articles appartiennent a une unite de service et sont issus d'un catalogue.
- Un article peut reprendre le prix du catalogue ou definir son propre prix optionnel.
- Dans le MVP, les catalogues n'ont pas de visibilite publique independante.
- Un catalogue actif est consultable si son entreprise est active.
- Le MVP privilegie l'archivage/desactivation logique plutot que la suppression physique.
- Une unite de service represente une file de prise en charge par tickets.
- Le seul type d'unite de service supporte dans le MVP est `TICKET_QUEUE`.
- Les autres modes de fonctionnement comme reservation, commande, livraison ou drive sont hors perimetre MVP.
- Une unite de service possede toujours un emplacement par defaut cree avec elle.
- Une unite de service peut posseder plusieurs emplacements, par exemple des tables dans un restaurant.
- Le lien public stable et le QR code sont rattaches a un emplacement d'unite de service, notamment l'emplacement par defaut.
- Le backend fournit l'identifiant ou l'URL d'acces public de l'emplacement, mais ne genere pas d'image QR code.
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
- Dans le MVP, les quantites d'article sont representatives et informatives; elles ne bloquent pas la creation de ticket si la demande depasse la quantite configuree.
- Un ticket peut contenir zero, une ou plusieurs lignes de ticket.
- Un ticket avec zero ligne permet une prise en charge generale.
- Une ligne de ticket peut preciser une quantite d'article.
- Si la quantite d'une ligne de ticket est absente ou `null` dans la requete, le backend utilise `1`.
- Si la quantite d'une ligne de ticket est fournie, elle doit etre superieure ou egale a `1`.
- Un ticket est toujours rattache a une unite de service et a un emplacement de cette unite.
- Si aucun emplacement n'est precise a la creation d'un ticket, l'emplacement par defaut de l'unite est utilise.
- La creation de ticket utilise un seul endpoint pour les utilisateurs authentifies et les visiteurs non authentifies.
- Pour un visiteur non authentifie, `guestName` est obligatoire.
- Pour un utilisateur authentifie, le ticket est rattache au compte via le JWT et `guestName` n'est pas requis.
- Un ticket peut recevoir un numero de telephone de contact optionnel, que le createur soit authentifie ou non.
- Le numero de telephone du ticket est une information de contact copiee sur le ticket; il ne sert pas a authentifier l'acces au ticket.
- Le total d'un ticket est informatif et calcule a partir des lignes qui possedent un prix.
- La devise du ticket est copiee depuis l'entreprise au moment de la creation du ticket pour conserver l'historique.
- Les prix des lignes de ticket sont figes au moment de la creation pour ne pas modifier les anciens tickets si le catalogue ou l'article change ensuite.
- Le numero de ticket est genere automatiquement.
- Le numero de ticket est unique globalement sur la plateforme afin de faciliter la consultation par un utilisateur non authentifie.
- Le format d'affichage MVP du numero de ticket est un identifiant lisible du type `T-000001`.
- Le numero de ticket seul ne donne pas acces a un ticket.
- Un ticket non authentifie retourne un code d'acces court en plus du numero de ticket.
- Le code d'acces invite est retourne une seule fois a la creation du ticket.
- Le code d'acces invite est stocke uniquement sous forme hashee.
- Un visiteur non authentifie peut consulter son ticket avec `ticketNumber` + `accessCode`.
- Un visiteur non authentifie peut annuler son ticket avec `ticketNumber` + `accessCode` si la transition est valide.
- Un visiteur non authentifie peut confirmer que son ticket a ete traite avec `ticketNumber` + `accessCode` si la transition est valide.
- Les utilisateurs autorises de l'entreprise peuvent gerer les tickets.
- Dans le MVP, les roles `ADMIN` et `EMPLOYEE` peuvent gerer les tickets.
- Dans le MVP, seul le role `ADMIN` gere l'entreprise, les categories, les catalogues, les unites de service et les articles.
- Le createur authentifie peut annuler son propre ticket si le ticket est associe a son compte et si la transition est valide.
- Le createur authentifie peut confirmer que son propre ticket a ete traite si la transition est valide.
- Un utilisateur authentifie accede a ses tickets par son compte et son JWT; aucun code d'acces invite n'est requis.

## Ambiguites resolues

- Le Product Blueprint evoque un catalogue unique, mais FS-001 detaille plusieurs categories et plusieurs catalogues. Decision retenue: plusieurs catalogues classes par categories.
- FS-001 evoque la visibilite publique/privee des entreprises. Decision MVP retenue: pas de visibilite publique/privee, seules les entreprises `ACTIVE` sont visibles.
- FS-001 evoque plusieurs modes de fonctionnement possibles. Decision MVP retenue: uniquement `TICKET_QUEUE`.
- FS-001 evoque l'annulation de ticket sans preciser tous les acteurs. Decision retenue: entreprise autorisee, createur authentifie, ou visiteur non authentifie avec `ticketNumber` + `accessCode` valide.
- La regle d'ouverture initiale exigeait au moins un article disponible. Decision corrigee: les articles sont optionnels et une unite peut etre ouverte sans article pour permettre les tickets generaux.
- Le besoin de QR code est retenu sous forme de lien public stable. Decision retenue: le backend ne genere pas d'image QR code.
- La regle initiale de numerotation unique par unite est remplacee par une numerotation globale unique sur la plateforme pour simplifier le suivi par les utilisateurs non authentifies.
- La consultation et certaines actions non authentifiees sont autorisees avec `ticketNumber` + `accessCode`; le numero seul ne suffit jamais.
- L'annulation non authentifiee n'est plus refusee par principe: elle est autorisee seulement avec un code d'acces valide et une transition autorisee.
- Le lien QR n'est plus rattache directement a l'unite de service: il est rattache a un emplacement. L'emplacement par defaut couvre le cas simple.
- La creation de ticket n'est pas separee en deux routes distinctes authentifie/invite: le meme endpoint accepte un JWT optionnel.
- Le prix catalogue est optionnel et sert de base au prix d'article; le ticket fige les montants au moment de sa creation.
- La devise n'est pas portee par chaque prix de catalogue ou d'article: elle vient de l'entreprise, puis elle est copiee sur le ticket pour l'historique.
- Le telephone de contact d'un ticket est optionnel pour les visiteurs et les utilisateurs authentifies; il reste une donnee de contact, pas une preuve d'identite.
- Les quantites d'articles sont representatives au MVP: `configured_quantity` et `reserved_quantity` ne bloquent pas la creation de ticket et pourront servir plus tard a l'affichage, aux alertes ou aux controles operationnels.
- La quantite d'une ligne de ticket est optionnelle dans les requetes de creation. Decision retenue: valeur par defaut `1`, et refus uniquement si une valeur fournie est inferieure a `1`.

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

### COMPANY-003 - Ajouter la devise de compagnie

**En tant que** administrateur d'entreprise,
**je veux** definir la devise de mon entreprise,
**afin de** utiliser une devise coherente pour les prix, articles et tickets.

Criteres d'acceptation:

- Une migration Flyway ajoute le champ `currency` a la table `companies`.
- La devise est stockee sous forme de code ISO 4217 sur 3 lettres, par exemple `CAD`, `USD` ou `EUR`.
- La valeur par defaut MVP est `CAD`.
- La devise est obligatoire, normalisee en majuscules et validee.
- La creation d'entreprise accepte une devise optionnelle; si elle est absente, `CAD` est utilise.
- Les reponses entreprise exposent la devise.
- Les collections Postman sont mises a jour si l'API de creation ou de consultation change.

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
- Les champs sont presents: `id`, `company_id`, `catalog_category_id`, `name`, `description`, `image_url`, `price_amount`, `status`, `created_at`, `updated_at`, `created_by`, `updated_by`, `version`.
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
- Un prix indicatif optionnel peut etre renseigne.
- Le prix utilise la devise de l'entreprise; aucune devise separee n'est stockee sur le catalogue.
- Le catalogue est cree avec le statut `ACTIVE`.

### CATALOG-020 - Lister les catalogues par categorie

**En tant que** utilisateur,
**je veux** consulter les catalogues actifs classes par categories,
**afin de** comprendre l'offre d'une entreprise active.

Criteres d'acceptation:

- Les catalogues actifs d'une entreprise active sont consultables.
- Les catalogues sont groupes ou filtrables par categorie.
- Le meme endpoint accepte un query param optionnel `catalogCategoryId`.
- Sans `catalogCategoryId`, tous les catalogues actifs de l'entreprise sont retournes.
- Avec `catalogCategoryId`, seuls les catalogues actifs de cette categorie sont retournes.
- Si `catalogCategoryId` ne correspond pas a une categorie de l'entreprise, une erreur `400 BAD_REQUEST` est retournee.
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
- Les champs sont presents: `id`, `company_id`, `name`, `description`, `location`, `type`, `status`, `settings`, `created_at`, `updated_at`, `created_by`, `updated_by`, `version`.
- Le seul type supporte au MVP est `TICKET_QUEUE`.
- Les statuts supportes sont `CLOSED`, `OPEN`, `ARCHIVED`.
- Une unite appartient obligatoirement a une entreprise.
- Le lien public direct n'est pas stocke sur l'unite mais sur ses emplacements.
- Les index necessaires sont presents: `company_id`, `status`, `type`.

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
- Un emplacement par defaut est cree dans la meme transaction.
- L'unite peut etre creee sans article.

### LOCATION-001 - Creer la table `service_unit_locations`

**En tant que** backend,
**je veux** persister les emplacements d'une unite de service,
**afin de** rattacher les tickets et les liens QR a un contexte precis.

Criteres d'acceptation:

- Une migration Flyway cree la table `service_unit_locations`.
- Les champs sont presents: `id`, `service_unit_id`, `name`, `description`, `type`, `is_default`, `public_access_slug`, `status`, `created_at`, `updated_at`, `created_by`, `updated_by`, `version`.
- Un emplacement appartient obligatoirement a une unite de service.
- Un seul emplacement par defaut actif existe par unite de service.
- `public_access_slug` est unique et stable sur la plateforme.
- Les index necessaires sont presents: `service_unit_id`, `status`, `is_default`, `public_access_slug`.

### LOCATION-002 - Creer entite et repository ServiceUnitLocation

**En tant que** developpeur,
**je veux** disposer d'une entite et d'un repository ServiceUnitLocation,
**afin de** gerer les emplacements et leurs liens publics.

Criteres d'acceptation:

- L'entite `ServiceUnitLocation` existe.
- Les enums de type et de statut d'emplacement existent si necessaire.
- Le repository permet de lister les emplacements par unite.
- Le repository permet de retrouver l'emplacement par defaut d'une unite.
- Le repository permet de retrouver un emplacement par `public_access_slug`.

### LOCATION-010 - Creer l'emplacement par defaut d'une unite

**En tant que** plateforme,
**je veux** creer automatiquement un emplacement par defaut avec chaque unite de service,
**afin de** permettre le parcours simple sans selection d'emplacement.

Criteres d'acceptation:

- La creation d'une unite cree un emplacement par defaut dans la meme transaction.
- L'emplacement par defaut possede un `public_access_slug` unique.
- Le nom par defaut est coherent, par exemple `Default` ou `Principal`.
- Une unite ne peut pas se retrouver sans emplacement par defaut actif.
- Si la creation de l'emplacement echoue, la creation de l'unite est annulee.

### LOCATION-011 - Creer un emplacement dans une unite de service

**En tant que** administrateur d'entreprise,
**je veux** creer des emplacements dans une unite de service,
**afin de** representer des tables, comptoirs, zones ou points de prise en charge.

Criteres d'acceptation:

- Le role `ADMIN` est requis.
- L'unite doit appartenir a l'entreprise administree.
- Le nom de l'emplacement est obligatoire.
- Un `public_access_slug` unique est genere automatiquement.
- Le backend retourne le lien public ou l'identifiant public de l'emplacement.
- Le backend ne genere pas d'image QR code.

### LOCATION-020 - Consulter les emplacements d'une unite de service

**En tant que** administrateur d'entreprise,
**je veux** lister les emplacements d'une unite de service,
**afin de** gerer les points de creation de tickets.

Criteres d'acceptation:

- Le role `ADMIN` est requis.
- Les emplacements de l'unite sont retournes.
- L'emplacement par defaut est identifiable.
- Les emplacements d'une autre entreprise ne sont pas exposes.
- Les resultats sont pagines si la liste peut grandir.

### LOCATION-030 - Acceder a une unite par lien public d'emplacement

**En tant que** utilisateur,
**je veux** acceder directement a un emplacement d'une unite avec son lien public,
**afin de** creer rapidement un ticket apres avoir recu un lien ou scanne un QR code.

Criteres d'acceptation:

- L'endpoint public permet de retrouver un emplacement par `public_access_slug`.
- L'unite de l'emplacement doit etre `OPEN`.
- L'entreprise de l'unite doit etre `ACTIVE`.
- Un emplacement inactif n'est pas expose publiquement.
- La reponse contient les informations publiques de l'entreprise, de l'unite et de l'emplacement.
- La reponse permet de creer un ticket directement dans cet emplacement.
- Le backend ne genere pas d'image QR code.

### SERVICE-011 - Consulter le lien public par defaut d'une unite de service

**En tant que** administrateur d'entreprise,
**je veux** obtenir le lien public de l'emplacement par defaut d'une unite de service,
**afin de** le partager ou de l'utiliser pour generer un QR code hors backend.

Criteres d'acceptation:

- Le role `ADMIN` est requis.
- La reponse contient l'identifiant public ou l'URL publique de l'emplacement par defaut.
- Le backend ne genere pas d'image QR code.
- Le backend ne stocke pas d'image QR code.
- Le lien reste stable tant que l'emplacement par defaut existe.

### SERVICE-012 - Consulter les unites de service en administration

**En tant que** administrateur d'entreprise,
**je veux** consulter toutes les unites de service de mon entreprise,
**afin de** gerer les unites ouvertes, fermees ou archivees depuis l'espace admin.

Criteres d'acceptation:

- Le role `ADMIN` est requis.
- L'entreprise doit etre `ACTIVE`.
- La liste retourne les unites `OPEN`, `CLOSED` et `ARCHIVED` de l'entreprise.
- Les unites d'une autre entreprise ne sont pas exposees.
- La reponse inclut l'emplacement par defaut lorsqu'il existe.
- Un filtre optionnel par `status` permet de limiter les resultats.
- Les resultats sont pagines.
- L'endpoint n'est pas public et requiert un JWT valide.

### SERVICE-013 - Modifier une unite de service

**En tant que** administrateur d'entreprise,
**je veux** modifier les informations d'une unite de service,
**afin de** corriger ou faire evoluer sa presentation sans recreer l'unite.

Criteres d'acceptation:

- Le role `ADMIN` est requis.
- L'entreprise doit etre `ACTIVE`.
- L'unite doit appartenir a l'entreprise.
- Les champs modifiables sont: `name`, `description`, `location`.
- Le nom est obligatoire.
- Le statut de l'unite n'est pas modifie par cet endpoint.
- L'emplacement par defaut n'est pas modifie par cet endpoint.
- Une unite `OPEN`, `CLOSED` ou `ARCHIVED` peut etre modifiee.
- La reponse retourne l'unite mise a jour avec son emplacement par defaut lorsqu'il existe.

### ITEM-001 - Creer la table `items`

**En tant que** backend,
**je veux** persister les articles disponibles dans une unite de service,
**afin de** representer les catalogues rendus operationnels dans cette unite.

Criteres d'acceptation:

- Une migration Flyway cree la table `items`.
- Les champs sont presents: `id`, `service_unit_id`, `catalog_id`, `price_amount`, `availability`, `configured_quantity`, `reserved_quantity`, `display_order`, `status`, `created_at`, `updated_at`, `version`.
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
- L'article reprend le prix indicatif du catalogue par defaut si celui-ci existe.

### ITEM-010 - Configurer un article

**En tant que** administrateur d'entreprise,
**je veux** configurer un article d'une unite,
**afin de** controler sa disponibilite operationnelle.

Criteres d'acceptation:

- Le role `ADMIN` est requis.
- La disponibilite peut etre modifiee.
- La quantite configuree peut etre modifiee.
- L'ordre d'affichage peut etre modifie.
- Le prix de l'article peut etre modifie ou laisse vide.
- Le prix utilise la devise de l'entreprise; aucune devise separee n'est stockee sur l'article.
- La quantite configuree est representative dans le MVP; elle ne bloque pas la creation de ticket si elle est depassee.
- La quantite reservee reste calculee et n'est pas modifiee directement.
- La quantite reservee est representative dans le MVP; elle ne bloque pas la creation de ticket.

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

### SERVICE-042 - Acceder a une unite par son emplacement par defaut

**En tant que** utilisateur,
**je veux** acceder directement a une unite de service via son emplacement par defaut,
**afin de** creer rapidement un ticket apres avoir recu un lien ou scanne un QR code.

Criteres d'acceptation:

- L'endpoint public utilise le `public_access_slug` de l'emplacement par defaut.
- L'unite doit etre `OPEN`.
- L'entreprise de l'unite doit etre `ACTIVE`.
- Une unite `CLOSED` ou `ARCHIVED` n'est pas exposee par le lien public.
- Le backend retourne les informations necessaires a l'affichage de l'unite et de l'emplacement par defaut.
- Le backend ne genere pas d'image QR code.

## Milestone 5 - Tickets

### TICKET-001 - Creer les tables `tickets` et `ticket_lines`

**En tant que** backend,
**je veux** persister les tickets et leurs lignes,
**afin de** suivre les demandes de prise en charge.

Criteres d'acceptation:

- Une migration Flyway cree la table `tickets`.
- Une migration Flyway cree la table `ticket_lines`.
- `tickets` contient au minimum: `id`, `ticket_number`, `user_id`, `guest_name`, `customer_phone`, `guest_access_code_hash`, `service_unit_id`, `service_unit_location_id`, `status`, `notes`, `currency`, `total_amount`, `created_at`, `updated_at`, `closed_at`, `version`.
- `ticket_lines` contient au minimum: `id`, `ticket_id`, `item_id`, `quantity`, `unit_price_amount`, `line_total_amount`, `notes`.
- `ticket_lines.quantity` est persiste avec une valeur superieure ou egale a `1`.
- `ticket_number` est unique globalement sur la plateforme.
- Le format d'affichage MVP du numero est du type `T-000001`.
- `guest_access_code_hash` est renseigne uniquement pour les tickets non authentifies.
- Le code d'acces invite n'est jamais stocke en clair.
- `customer_phone` est optionnel et stocke tel que normalise pour permettre a l'entreprise de recontacter le client.
- Chaque ticket reference une unite de service et un emplacement appartenant a cette unite.
- Une ligne de ticket reference un article appartenant a la meme unite que le ticket.
- Les prix de ligne sont figes a la creation du ticket.
- Le total du ticket est calcule a titre informatif a partir des lignes qui possedent un prix.
- La devise du ticket est copiee depuis l'entreprise au moment de la creation.
- Les index necessaires sont presents: `ticket_number` unique, `service_unit_id`, `service_unit_location_id`, `user_id`, `status`.

### TICKET-002 - Creer entites et repositories Ticket

**En tant que** developpeur,
**je veux** disposer des entites et repositories Ticket,
**afin de** manipuler les demandes de prise en charge.

Criteres d'acceptation:

- Les entites `Ticket` et `TicketLine` existent.
- L'enum `TicketStatus` existe avec `CREATED`, `CONFIRMED`, `CALLED`, `IN_PROGRESS`, `COMPLETED`, `CLOSED`, `CANCELLED`.
- L'entite `Ticket` expose un champ `customerPhone` optionnel.
- Les repositories permettent de lister les tickets par unite, utilisateur et statut.
- Les transitions de statut sont preparees cote domaine ou service applicatif.

### TICKET-010 - Creer un ticket non authentifie

**En tant que** visiteur non authentifie,
**je veux** creer un ticket dans une unite ouverte,
**afin de** demander une prise en charge sans compte FlowMova.

Criteres d'acceptation:

- Le meme endpoint de creation est utilise pour les visiteurs et les utilisateurs authentifies: `POST /api/service-units/{serviceUnitId}/tickets`.
- L'unite doit etre `OPEN`.
- L'entreprise de l'unite doit etre `ACTIVE`.
- `locationId` est optionnel dans la requete.
- Si `locationId` est absent, l'emplacement par defaut de l'unite est utilise.
- Si `locationId` est present, il doit appartenir a l'unite.
- Le ticket peut contenir zero, une ou plusieurs lignes.
- Pour chaque ligne, `quantity` est optionnel; si absent ou `null`, la valeur retenue est `1`.
- Si `quantity` est fourni, il doit etre superieur ou egal a `1`.
- Les quantites configurees ou reservees des articles ne bloquent pas la creation du ticket dans le MVP.
- `guestName` est obligatoire pour un visiteur non authentifie.
- `customerPhone` est optionnel pour un visiteur non authentifie.
- Le ticket n'est pas associe a un `user_id`.
- Un code d'acces invite court est genere automatiquement.
- Le hash du code d'acces invite est stocke dans `guest_access_code_hash`.
- Le code d'acces invite en clair est retourne une seule fois dans la reponse.
- Le ticket est cree avec le statut `CREATED`.
- Un numero de ticket unique globalement est genere.
- La devise du ticket est copiee depuis l'entreprise.
- Les montants des lignes et le total informatif sont calcules lorsque les articles possedent un prix.
- La reponse contient au minimum `ticketNumber` et `accessCode`.
- Le visiteur est informe qu'il doit conserver le code pour suivre ou modifier son ticket.

### TICKET-011 - Creer un ticket authentifie

**En tant que** utilisateur authentifie,
**je veux** creer un ticket associe a mon compte,
**afin de** suivre ma demande de prise en charge.

Criteres d'acceptation:

- Le meme endpoint de creation accepte un utilisateur authentifie avec JWT: `POST /api/service-units/{serviceUnitId}/tickets`.
- L'unite doit etre `OPEN`.
- L'entreprise de l'unite doit etre `ACTIVE`.
- `locationId` est optionnel dans la requete.
- Si `locationId` est absent, l'emplacement par defaut de l'unite est utilise.
- Si `locationId` est present, il doit appartenir a l'unite.
- Le ticket est associe au `user_id` authentifie.
- `guestName` n'est pas requis pour un utilisateur authentifie.
- `customerPhone` est optionnel pour un utilisateur authentifie et n'est pas automatiquement considere comme le telephone du profil utilisateur.
- Aucun code d'acces invite n'est genere pour un ticket authentifie.
- `guest_access_code_hash` reste vide pour un ticket authentifie.
- Le ticket peut contenir zero, une ou plusieurs lignes.
- Pour chaque ligne, `quantity` est optionnel; si absent ou `null`, la valeur retenue est `1`.
- Si `quantity` est fourni, il doit etre superieur ou egal a `1`.
- Les quantites configurees ou reservees des articles ne bloquent pas la creation du ticket dans le MVP.
- Le ticket est cree avec le statut `CREATED`.
- Un numero de ticket unique globalement est genere.
- La devise du ticket est copiee depuis l'entreprise.
- Les montants des lignes et le total informatif sont calcules lorsque les articles possedent un prix.

### TICKET-020 - Consulter les tickets d'une unite

**En tant que** utilisateur autorise de l'entreprise,
**je veux** consulter les tickets d'une unite,
**afin de** suivre les demandes de prise en charge.

Criteres d'acceptation:

- L'endpoint `GET /api/companies/{companyId}/admin/service-units/{serviceUnitId}/tickets` existe.
- L'utilisateur doit appartenir a l'entreprise de l'unite.
- Les roles `ADMIN` et `EMPLOYEE` peuvent consulter les tickets.
- L'entreprise doit etre `ACTIVE`.
- L'unite doit appartenir a l'entreprise.
- Les tickets sont filtres par unite.
- La liste est paginee.
- Les tickets peuvent etre filtres par statut.
- Les tickets peuvent etre recherches par numero de ticket.
- La recherche par numero est limitee aux tickets de l'unite.
- La recherche par numero accepte une valeur partielle et est insensible a la casse.
- Les donnees d'une autre entreprise ne sont pas exposees.

### TICKET-021 - Consulter mes tickets

**En tant que** utilisateur authentifie,
**je veux** consulter mes tickets,
**afin de** suivre mes propres demandes.

Criteres d'acceptation:

- L'endpoint `GET /api/users/me/tickets` existe.
- L'endpoint retourne uniquement les tickets associes au compte authentifie.
- Les tickets non authentifies ne sont pas retournes.
- La liste est paginee.
- Les tickets peuvent etre filtres par statut.
- Les tickets peuvent etre recherches par numero de ticket.
- La recherche par numero est limitee aux tickets du compte authentifie.
- La recherche par numero accepte une valeur partielle et est insensible a la casse.
- Les informations sensibles internes de l'entreprise ne sont pas exposees.

### TICKET-022 - Consulter un ticket non authentifie avec numero et code

**En tant que** visiteur non authentifie,
**je veux** consulter mon ticket avec son numero et son code d'acces,
**afin de** suivre ma demande sans creer de compte FlowMova.

Criteres d'acceptation:

- L'endpoint de consultation non authentifiee existe.
- Le visiteur fournit `ticketNumber` et `accessCode`.
- Le numero de ticket seul ne suffit pas.
- Le backend compare le code fourni avec le hash stocke.
- Si le numero ou le code est invalide, l'acces est refuse.
- La reponse retourne uniquement les informations publiques du ticket.
- Les informations internes de l'entreprise ne sont pas exposees.

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

### TICKET-032 - Refuser les actions invitees avec numero ou code invalide

**En tant que** plateforme,
**je veux** refuser les actions invitees lorsque le numero ou le code d'acces est invalide,
**afin de** proteger les tickets non authentifies.

Criteres d'acceptation:

- Le numero de ticket seul ne permet aucune consultation ou modification.
- Un code d'acces invalide refuse l'action.
- Un code d'acces absent refuse l'action.
- Les refus retournent une erreur coherente sans exposer l'existence du ticket lorsque possible.
- Les actions autorisees avec un code valide restent: consulter, annuler, confirmer que le ticket a ete traite.

### TICKET-033 - Annuler un ticket non authentifie avec numero et code

**En tant que** visiteur non authentifie,
**je veux** annuler mon ticket avec son numero et son code d'acces,
**afin de** retirer ma demande sans compte FlowMova.

Criteres d'acceptation:

- Le visiteur fournit `ticketNumber` et `accessCode`.
- Le backend valide le code d'acces avec le hash stocke.
- La transition vers `CANCELLED` doit etre valide.
- Un numero ou un code invalide refuse l'annulation.
- Un ticket deja finalise ne peut pas etre annule si le cycle de vie l'interdit.

### TICKET-034 - Confirmer le traitement d'un ticket non authentifie avec numero et code

**En tant que** visiteur non authentifie,
**je veux** confirmer que mon ticket a ete traite avec son numero et son code d'acces,
**afin de** indiquer que ma prise en charge est terminee sans compte FlowMova.

Criteres d'acceptation:

- Le visiteur fournit `ticketNumber` et `accessCode`.
- Le backend valide le code d'acces avec le hash stocke.
- L'action ne correspond pas a la confirmation operationnelle interne de l'entreprise.
- La transition vers l'etat de traitement termine doit etre valide.
- Un numero ou un code invalide refuse l'action.
- Un ticket deja finalise ne peut pas etre modifie si le cycle de vie l'interdit.

### TICKET-035 - Confirmer le traitement de son propre ticket authentifie

**En tant que** utilisateur authentifie,
**je veux** confirmer que mon propre ticket a ete traite,
**afin de** indiquer que ma prise en charge est terminee depuis mon compte.

Criteres d'acceptation:

- L'utilisateur doit etre authentifie.
- Le ticket doit etre associe au compte authentifie.
- L'action ne correspond pas a la confirmation operationnelle interne de l'entreprise.
- La transition vers l'etat de traitement termine doit etre valide.
- Un utilisateur ne peut pas confirmer le traitement du ticket d'un autre utilisateur.

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
5. COMPANY-003
6. COMPANY-030
7. COMPANY-031
8. CATCAT-001
9. CATCAT-002
10. CATCAT-010
11. CATALOG-001
12. CATALOG-002
13. CATALOG-010
14. CATALOG-020
15. SERVICE-001
16. SERVICE-002
17. LOCATION-001
18. LOCATION-002
19. SERVICE-010
20. LOCATION-010
21. SERVICE-011
22. SERVICE-012
23. SERVICE-013
24. SERVICE-030
25. SERVICE-040
26. SERVICE-041
27. LOCATION-030
28. TICKET-001
29. TICKET-002
30. TICKET-010
31. TICKET-022

Cette verticale permet d'obtenir: creation entreprise avec devise -> catalogue avec prix optionnel -> unite ouverte sans article obligatoire -> emplacement par defaut et lien direct -> creation ticket general dans un emplacement -> consultation invitee avec numero et code.

Les issues `LOCATION-011`, `LOCATION-020`, `ITEM-001`, `ITEM-002`, `SERVICE-020` et `ITEM-010` peuvent ensuite enrichir l'unite avec des emplacements supplementaires et des articles disponibles.
