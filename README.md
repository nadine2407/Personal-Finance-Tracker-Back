# MyBudget

> Application web de gestion des finances personnelles — Master 1 Informatique, S2 Programmation Web 2025–2026
>
> **Amina YOUS · Nadine MASROUR**

---

## Liens

| | URL |
|---|---|
| Backend (ce dépôt) | https://github.com/nadine2407/Personal-Finance-Tracker-Back |
| Frontend | https://github.com/nadine2407/Personal-Finance-Tracker-Front |
| Swagger UI | http://localhost:8080/swagger-ui.html |

---

## Présentation

Finance Tracker permet à un utilisateur de piloter l'ensemble de sa vie financière depuis une interface unique.

**Fonctionnalités :**
- Gestion de comptes bancaires (courant et épargne)
- Suivi des transactions (revenus, dépenses, virements) avec récurrence mensuelle automatique
- Catégorisation personnalisée (revenu / dépense / les deux)
- Budgets mensuels par catégorie avec alertes visuelles (normal / alerte / dépassé / non planifié)
- Objectifs d'épargne avec allocation, priorisation et débit vers compte courant
- Tableau de bord analytique (statistiques mensuelles, graphique annuel, transactions récentes)
- Transactions masquables sans suppression, notes par transaction
- Interface en français, sécurisée par jeton d'authentification

---

## Lancer le projet

### Prérequis

| Outil | Version minimale |
|---|---|
| Java | 17+ |
| Maven | 3.8+ |

### Démarrage

```bash
git clone https://github.com/nadine2407/Personal-Finance-Tracker-Back.git
cd Personal-Finance-Tracker-Back
./mvnw spring-boot:run
```

- Serveur disponible sur **http://localhost:8080**
- Documentation Swagger : **http://localhost:8080/swagger-ui.html**

> **Remarque :** Les identifiants de connexion à la base de données sont inclus dans `application.properties` afin de simplifier le déploiement immédiat. Dans un contexte de production réelle, ces données critiques (URL, mot de passe, clé JWT) doivent être externalisées via des variables d'environnement ou un fichier `.env` ignoré par Git.

### Compte de démonstration

| Courriel | Mot de passe |
|---|---|
| `demo@gmail.app` | `demo123` |

---

## Stack technique

| Couche | Technologie | Version |
|---|---|---|
| Serveur | Spring Boot — Java 17 | 4.0.6 |
| Sécurité | Spring Security + JWT (jjwt) | 0.12.6 |
| Liaison base | Spring Data JPA + Hibernate | — |
| Base de données | PostgreSQL via Supabase | — |
| Documentation API | SpringDoc OpenAPI / Swagger UI | 2.8.9 |

---

## Architecture

```
┌──────────────────────────────────────────────────────────┐
│                  NAVIGATEUR (Angular 21)                 │
│   Component ←→ Store (Signals) ←→ Service HTTP          │
│   auth.interceptor · error.interceptor · auth.guard      │
└────────────────────────┬─────────────────────────────────┘
                         │ HTTPS · REST/JSON · Bearer JWT
┌────────────────────────▼─────────────────────────────────┐
│                  SPRING BOOT 4 (Java 17)                 │
│   JwtAuthFilter → Controller → Service → Repository      │
│   DTO Request/Response · JPA Specification               │
│   GlobalExceptionHandler                                 │
└────────────────────────┬─────────────────────────────────┘
                         │
┌────────────────────────▼─────────────────────────────────┐
│             PostgreSQL — Supabase (Cloud)                │
│   users · accounts · transactions · categories           │
│   budgets · goals                                        │
└──────────────────────────────────────────────────────────┘
```

**Structure des packages :**

```
com.example.financetracker/
├── security/        JWT filter, UserDetailsService, SecurityConfig
├── common/
│   ├── config/      CORS, DataInitializer (catégories par défaut)
│   ├── dto/         ApiResponse<T>, PageResponse<T>
│   └── exception/   GlobalExceptionHandler
└── domain/
    ├── auth/        Inscription, connexion, profil
    ├── account/     Comptes bancaires
    ├── category/    Catégories
    ├── transaction/ Transactions + filtres JPA Specification
    ├── budget/      Budgets mensuels
    ├── goal/        Objectifs d'épargne
    └── dashboard/   Statistiques et graphiques
```

---

## Base de données

| Propriété | Valeur |
|---|---|
| Système de gestion | PostgreSQL hébergé sur Supabase |
| Gestion du schéma | Hibernate `ddl-auto=update` |
| Chiffrement connexion | `sslmode=require` |
| Pool de connexions | HikariCP — max 3, inactif 5 min, durée 20 min |
| Précision monétaire | `DECIMAL(15,2)` sur tous les montants |
| Expiration du jeton | 86 400 000 ms (24 heures) |

---

## Points d'accès API

**Base URL :** `http://localhost:8080/api/v1`  
**Authentification :** `Authorization: Bearer <token>` (sauf `/auth/**`)

| Domaine | Méthode | Endpoint | Description |
|---|---|---|---|
| **Auth** | POST | `/auth/users` | Inscription |
| | POST | `/auth/sessions` | Connexion → jeton 24h |
| **Utilisateur** | GET | `/users/profile` | Récupérer le profil |
| | PUT | `/users/profile` | Modifier le profil |
| | PUT | `/users/password` | Changer le mot de passe |
| **Comptes** | GET | `/accounts` | Lister les comptes |
| | POST | `/accounts` | Créer un compte |
| | PUT | `/accounts/{id}` | Modifier un compte |
| | DELETE | `/accounts/{id}` | Supprimer un compte |
| **Catégories** | GET | `/categories` | Lister (personnalisées + système) |
| | POST | `/categories` | Créer |
| | PUT | `/categories/{id}` | Modifier |
| | DELETE | `/categories/{id}` | Supprimer |
| **Transactions** | GET | `/transactions` | Lister avec filtres paginés |
| | POST | `/transactions` | Créer (série si récurrente) |
| | PUT | `/transactions/{id}` | Modifier |
| | PATCH | `/transactions/{id}/note` | Modifier la note |
| | PATCH | `/transactions/{id}/hidden` | Masquer / afficher |
| | DELETE | `/transactions/{id}` | Supprimer |
| | DELETE | `/transactions/{id}/future` | Supprimer l'occurrence et les suivantes |
| **Budgets** | GET | `/budgets/status` | Statut du mois (`?month=&year=`) |
| | POST | `/budgets` | Créer un budget |
| | PUT | `/budgets/{id}` | Modifier |
| | DELETE | `/budgets/{id}` | Supprimer |
| | POST | `/budgets/copies` | Dupliquer depuis un mois précédent |
| **Objectifs** | GET | `/goals` | Lister les objectifs actifs |
| | POST | `/goals` | Créer un objectif |
| | PUT | `/goals/{id}` | Modifier |
| | PATCH | `/goals/{id}/allocation` | Définir le montant alloué |
| | PATCH | `/goals/{id}/priority` | Monter / descendre la priorité |
| | PATCH | `/goals/{id}/debit` | Virer vers un compte courant |
| | DELETE | `/goals/{id}` | Supprimer |
| **Tableau de bord** | GET | `/dashboard/summary` | Résumé financier du mois |
| | GET | `/dashboard/monthly-chart` | Données graphique sur 12 mois |

---

## Règles fonctionnelles

### Comptes
Deux types de comptes sont gérés : courant et épargne. Chaque compte dispose d'un solde initial fixé à la création et d'un solde courant mis à jour automatiquement à chaque transaction. Le solde initial ne peut plus être modifié après la création du compte. Les objectifs d'épargne ne peuvent être liés qu'à un compte épargne. Les virements depuis un compte épargne passent obligatoirement par un compte courant intermédiaire.

### Transactions
Trois types de transactions sont disponibles : revenu, dépense et virement. Chaque transaction doit obligatoirement être associée à un compte. Les transactions de type revenu et dépense doivent être rattachées à une catégorie, tandis que les virements n'en nécessitent pas. Il est possible de masquer une transaction sans la supprimer : elle n'est alors plus prise en compte dans les soldes ni dans les statistiques. Une transaction peut être configurée comme récurrente avec une fréquence mensuelle, dans la limite d'un an.

### Catégories
Les catégories sont de trois types : revenu, dépense ou les deux. Dans le formulaire de saisie, seules les catégories compatibles avec le type de transaction sélectionné sont proposées. L'application fournit douze catégories par défaut non modifiables.

### Budgets
L'utilisateur définit un budget mensuel par catégorie. L'application calcule en temps réel le montant dépensé. Les budgets d'un mois peuvent être dupliqués en un clic.

### Objectifs d'épargne
Les objectifs sont liés à un compte épargne. Le total des allocations ne peut jamais dépasser le solde disponible. En cas de baisse du solde, l'application réduit automatiquement les allocations en commençant par les objectifs de plus faible priorité. Le débit d'un objectif atteint crée un virement vers un compte courant puis enregistre la dépense correspondante.

### Tableau de bord
Synthèse mensuelle (revenus, dépenses, épargne nette, répartition par catégorie), graphique annuel en barres, et cinq dernières transactions du mois.




