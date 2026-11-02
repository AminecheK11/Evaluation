
# Évaluation Hibernate / JPA – Gestion de stock, projets et état civil

**Réalisé par : Amine Chekkouri**

## Objectif

Cette évaluation consiste à développer trois applications Java utilisant Hibernate et JPA pour gérer la persistance des données dans MySQL. Les exercices permettent de mettre en pratique les relations entre entités, les opérations CRUD, les requêtes JPQL, Criteria et NamedQuery, ainsi que les tests unitaires.

## Technologies utilisées

- Java
- Hibernate ORM 5.6
- JPA
- MySQL et MySQL Workbench
- Maven
- JUnit
- IntelliJ IDEA

---



## Exercice 1 : Gestion de stock

### Présentation

Cet exercice consiste à développer une application de gestion de stock avec Hibernate et JPA pour gérer les catégories, les produits et les commandes, ainsi que leurs différentes relations.

### 1. Structure du projet

Le projet est organisé en packages `classes`, `dao`, `service`, `test` et `util`. Les principales entités sont `Categorie`, `Produit`, `Commande` et `LigneCommandeProduit`.

<img width="414" height="590" alt="cap1_ex1" src="https://github.com/user-attachments/assets/cabc819b-2ae8-4c65-99dd-112ce43a843a" />

### 2. Exécution de l'application

L'application affiche la liste des produits avec leurs prix, les produits d'une catégorie, les produits commandés pendant une période donnée, le détail d'une commande avec les quantités et les produits dont le prix dépasse 100 DH grâce à une NamedQuery.

<img width="556" height="664" alt="cap2_ex1" src="https://github.com/user-attachments/assets/8724df87-9c0f-4f57-9841-36f340c687d1" />

### 3. Résultats de l'exécution

Cette capture présente les résultats obtenus dans la console IntelliJ IDEA et confirme le bon fonctionnement des différentes requêtes Hibernate, avec une exécution terminée sans erreur.

<img width="556" height="664" alt="cap3_ex1" src="https://github.com/user-attachments/assets/05b8d280-b289-48cb-a9bd-dddc75a1d182" />

### 4. Vérification dans MySQL Workbench

La base `evaluation_stock` contient quatre tables : `categorie`, `commande`, `ligne_commande_produit` et `produit`.

<img width="940" height="608" alt="cap4_ex1" src="https://github.com/user-attachments/assets/9fc6530e-9df9-4220-b681-a0351fe3f3a8" />

La consultation de la table `produit` confirme l'enregistrement des cinq produits avec leurs références, leurs prix et leurs catégories.

<img width="919" height="632" alt="cap5_ex1" src="https://github.com/user-attachments/assets/2d241efb-bdb7-4c45-93ed-2712d7e3e1f8" />

### 5. Tests unitaires

Les tests Maven vérifient les opérations de persistance et les fonctionnalités de l'application. Le résultat indique **16 tests exécutés, 0 échec et 0 erreur**, avec le message `BUILD SUCCESS`.

<img width="850" height="601" alt="cap6_ex1" src="https://github.com/user-attachments/assets/863de5f7-06a3-459f-a9bb-296a4b810fa0" />

---


## Exercice 2 : Gestion des projets

### Présentation

Cet exercice porte sur la gestion des projets, des tâches et des employés. L'application permet de suivre la planification des tâches, leurs dates réelles de réalisation, les employés affectés et les coûts associés.

### 1. Structure du projet

Le projet comprend les entités `Projet`, `Tache`, `Employe` et `EmployeTache`, ainsi que les services, les classes utilitaires et les tests nécessaires à leur gestion.

<img width="291" height="638" alt="cap1_ex2" src="https://github.com/user-attachments/assets/6dab4e59-dc5c-43f7-8529-f57a06ce3f3c" />


### 2. Exécution de l'application

L'exécution affiche les tâches planifiées du projet « Gestion de stock », les tâches réalisées avec leurs dates réelles et les employés concernés. Elle permet également de rechercher les tâches dont le prix dépasse 1 000 DH à l'aide d'une NamedQuery, de filtrer les tâches réalisées sur une période et de calculer le coût global planifié du projet, qui s'élève à **9 500 DH**.

<img width="910" height="590" alt="cap2_ex2" src="https://github.com/user-attachments/assets/64349f8f-84f5-4810-bf3b-be0f7ababfcd" />


### 3. Vérification dans MySQL Workbench

La consultation de la base `evaluation_projets` permet de vérifier les tâches enregistrées, leurs dates de début et de fin, leurs prix et leur association aux projets.

<img width="605" height="360" alt="cap3_ex2" src="https://github.com/user-attachments/assets/e34dbda5-4419-4ad7-98c1-dd882c794e8f" />


### 4. Tests unitaires

L'exécution des tests Maven confirme la validation des fonctionnalités : **22 tests exécutés, 0 échec et 0 erreur de test**. La compilation se termine avec le message `BUILD SUCCESS`.

<img width="595" height="619" alt="cap4_ex2" src="https://github.com/user-attachments/assets/2510f140-bd0c-4fd2-8289-97c6c493e870" />


---

## Exercice 3 : Gestion de l'état civil

### Présentation

Cet exercice consiste à développer une application de gestion de l'état civil permettant de gérer les personnes, les hommes, les femmes et les mariages. Il met en pratique l'héritage entre entités, les associations et les requêtes avancées.

### 1. Structure du projet

L'application est organisée en plusieurs packages : `beans` pour les entités `Personne`, `Homme`, `Femme` et `Mariage`, `dao` pour l'accès aux données, `service` pour les traitements métier, `util` pour les outils Hibernate et `test` pour les exécutions et les tests.

<img width="303" height="597" alt="cap1_ex3" src="https://github.com/user-attachments/assets/8d5066bc-0006-4dd6-88c8-81c6384e2359" />


### 2. Exécution de l'application

L'exécution permet d'afficher les 10 femmes enregistrées, d'identifier la femme la plus âgée, de rechercher les épouses sur une période donnée, de calculer le nombre d'enfants et de retrouver les femmes mariées au moins deux fois. Elle permet également d'identifier les hommes ayant quatre épouses simultanément entre 2013 et 2020 et de consulter les détails des mariages en cours et terminés.

<img width="517" height="553" alt="cap2 1_ex3" src="https://github.com/user-attachments/assets/e52e3f35-23e9-4ad1-8d1b-0ae38aa85490" />

<img width="592" height="374" alt="cap2 2_ex3" src="https://github.com/user-attachments/assets/1f2c2ef4-5610-4383-8458-9953b0fa009c" />


### 3. Vérification dans MySQL Workbench

La consultation de la table `mariage` dans la base `evaluation_etat_civil` confirme l'enregistrement des mariages avec leurs dates de début et de fin, le nombre d'enfants et les identifiants des époux.

<img width="522" height="559" alt="cap3_ex3" src="https://github.com/user-attachments/assets/df0ff771-eb23-4ae2-b5b7-7951210e4546" />


### 4. Tests unitaires

L'exécution des tests Maven confirme la validation des fonctionnalités : **20 tests exécutés, 0 échec et 0 erreur de test**. La compilation se termine avec le message `BUILD SUCCESS`.

<img width="632" height="601" alt="cap4_ex3" src="https://github.com/user-attachments/assets/2a3585f3-be45-4ff0-851a-f16e4388b447" />


---

## Bilan des tests

| Exercice | Tests exécutés | Échecs | Erreurs de test | Résultat |
|----------|---------------:|-------:|----------------:|----------|
| Exercice 1 – Stock | 16 | 0 | 0 | BUILD SUCCESS |
| Exercice 2 – Projets | 22 | 0 | 0 | BUILD SUCCESS |
| Exercice 3 – État civil | 20 | 0 | 0 | BUILD SUCCESS |
| **Total** | **58** | **0** | **0** | **Succès** |

## Conclusion

Cette évaluation a été l'occasion de mettre en pratique Hibernate et JPA à travers trois applications de gestion. Les exercices abordent la modélisation des entités, les relations entre objets, les opérations de persistance et les requêtes avancées. Les vérifications dans MySQL et la réussite des 58 tests Maven confirment le bon fonctionnement des principales fonctionnalités développées.
