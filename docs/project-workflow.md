# Project Workflow

Cette page formalise les regles de travail du depot backend FlowMova.

Issue de reference: [DOC-002](https://github.com/Darkstorm97/flowmova-backend/issues/100).

## Sources de verite

Les specifications fonctionnelles et techniques restent prioritaires:

1. `FS-002 - Gestion des utilisateurs et de l'authentification`
2. `FS-001 - Gestion des unites de service`
3. `DAT - Document d'Architecture Technique`

Le Product Blueprint sert de contexte. En cas d'ambiguite, les Feature Specifications et le DAT priment.

## Regle avant developpement

Aucune fonctionnalite, evolution fonctionnelle ou dette technique planifiee ne doit etre implementee sans:

- discussion ou validation du besoin;
- verification de la documentation projet existante;
- creation ou identification d'une issue GitHub;
- mise a jour de la documentation avant ou avec le developpement si le comportement, l'API, l'architecture ou le workflow change.

## Nomenclature des issues

Les issues doivent suivre la forme:

```text
CODE-000 - Titre en francais
```

Exemples backend valides:

- `AUTH-020 - Connecter un utilisateur`
- `COMPANY-006 - Modifier une compagnie`
- `SERVICE-014 - Configurer le mode de controle de creation de tickets`
- `OPS-006 - Configurer les origines CORS par environnement`

Le code doit venir du backlog ou d'une famille deja utilisee dans le depot. Si aucun code n'existe encore, creer le prochain code coherent avec le domaine concerne avant de developper.

## Documentation attendue

Pour une nouvelle fonctionnalite ou une evolution visible:

- ajouter ou mettre a jour l'entree de backlog concernee;
- documenter les decisions MVP ou les ambiguites resolues si necessaire;
- mettre a jour OpenAPI, Postman, README ou docs d'architecture lorsque l'API, la configuration ou le lancement local change.

## Validation, commit et push

Avant commit:

- executer les tests pertinents;
- verifier que le changement reste aligne avec l'issue et la documentation;
- garder les commits scopes et lisibles.

Apres un commit valide:

- pousser immediatement la branche vers le depot distant;
- garder local et distant synchronises.
