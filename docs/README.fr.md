<div align="center">

<img src="../app/src/main/res/drawable-nodpi/ic_oblivion_logo.png" width="140" alt="Logo Oblivion">

# Oblivion V2

**Système anti-forensic de wipe sous contrainte pour Android**

[![Licence : AGPL v3](https://img.shields.io/badge/Licence-AGPL_v3-red.svg)](../LICENSE)
[![Android : 8.0+](https://img.shields.io/badge/Android-8.0%20→%2014+-green.svg)]()
[![Offline](https://img.shields.io/badge/100%25-offline-blue.svg)]()
[![Télémétrie : 0](https://img.shields.io/badge/Télémétrie-0-black.svg)]()

*Des déclencheurs indépendants qui réinitialisent ton téléphone quand tu ne peux plus le protéger toi-même.*

[English version](../README.md) · [Politique de sécurité](../SECURITY.md) · [Limites connues](#limites-connues)

</div>

---

## Public visé

Oblivion est un système anti-forensic défensif conçu pour protéger les données
personnelles contre la saisie illégale. Il est destiné aux **utilisateurs
qui font face à un risque réel** sur la souveraineté de leurs données :

- Journalistes opérant en zone de conflit ou sous régimes autoritaires
- Activistes des droits humains, dissidents et organisateurs politiques
- Lanceurs d'alerte protégeant l'identité de leurs sources
- Victimes de violences conjugales et de harcèlement
- Chercheurs en sécurité étudiant les systèmes de résistance sous contrainte
- Particuliers soucieux de leur vie privée, avec un modèle de menace concret

Ce logiciel est distribué sous **GNU Affero General Public License v3.0**.
L'usage détourné pour la destruction de preuves dans le cadre de procédures
pénales légales est illégal dans la plupart des juridictions et n'est pas
cautionné par les mainteneurs.

---

## Fonctionnement

Oblivion fournit **sept triggers indépendants**. Chacun s'active et se
configure séparément. Si l'un d'eux se déclenche, il exécute un factory reset
complet via l'API native Android `DevicePolicyManager.wipeData()`.

| # | Trigger | Mécanisme |
|---|---|---|
| 1 | **Guard — Lockscreen** | PIN de détresse ou longueur-piège (via `AccessibilityService`) ; N tentatives échouées (via `setMaximumFailedPasswordsForWipe` de la plateforme) |
| 2 | **USB Kill** | Branchement USB/chargeur écran verrouillé → compte à rebours → wipe |
| 3 | **SMS Wipe** | Numéro autorisé + mot-clé secret via `BroadcastReceiver` |
| 4 | **Voice Wipe** | Reconnaissance vocale offline (Vosk · modèles FR + EN bundlés) |
| 5 | **Dead Man's Switch** | Auto-wipe sans check-in biométrique pendant N heures/jours |
| 6 | **Scheduled Wipe** | `AlarmManager` exact, résistant au reboot |
| 7 | **Decoy Mode** | PIN leurre → fausse page "Mise à jour système" plein écran pendant que le wipe s'exécute en silence derrière |

Tous les triggers tournent en parallèle. Désarmer n'importe quel trigger
nécessite une authentification biométrique. L'application **ne déclare pas la
permission `INTERNET`** : elle ne peut donc ouvrir aucune connexion réseau, ce
que chacun peut vérifier dans
[`AndroidManifest.xml`](../app/src/main/AndroidManifest.xml).

---

## Stack de sécurité

| Composant | Implémentation |
|---|---|
| Chiffrement | AES-256-GCM via `EncryptedSharedPreferences` |
| Clé maître | Android Keystore (matériel sur les appareils dotés d'un TEE ou d'une StrongBox) |
| Hash PIN | PBKDF2-HMAC-SHA256, 100 000 itérations + sel aléatoire 32 octets |
| Comparaison PIN | À temps constant |
| Mécanisme de wipe | `DevicePolicyManager.wipeData()` (Device Admin natif) |
| Reconnaissance vocale | Vosk (offline, modèles small FR + EN bundlés — voir la note de licence plus bas) |
| Persistance | Alarmes `AlarmManager` exactes avec repli `WorkManager`, réarmées après reboot |
| Min SDK | API 26 · Android 8.0 |
| Target SDK | API 33 · volontaire (API 34+ bloque `wipeData()` pour apps non-DO) |

## Garanties de vie privée

- Aucune donnée n'est jamais envoyée à un serveur tiers
- Zéro télémétrie, zéro analytics, aucun crash reporter
- Tous les secrets stockés en `EncryptedSharedPreferences` (Tink/AES-256-GCM)
- Aucune notification au repos. Quand USB Kill ou Voice Wipe est armé,
  Android impose une notification de service de premier plan ; la
  notification du decoy est volontaire
- Tous les triggers individuellement activables et configurables
- Authentification biométrique requise pour désarmer
- Triggers réarmés après un reboot (voir *Limites connues* pour le force-stop)
- Open-source, auditable, modifiable

---

## Compiler depuis les sources

### Prérequis

- Android Studio **Hedgehog (2023.1.1)** ou plus récent
- JDK 17 (fourni avec Android Studio)
- Android SDK 34 installé
- Un appareil ou émulateur sous **Android 8.0 (API 26) ou plus**

### Commandes de build

```bash
git clone https://github.com/lethe-labs/oblivion-v2.git
cd oblivion-v2
./gradlew assembleRelease
```

Tests unitaires (JVM pur, aucun appareil requis) :

```bash
./gradlew testDebugUnitTest
```

APK généré : `app/build/outputs/apk/release/app-release.apk`

### Signer ton propre build release

Crée un fichier `keystore.properties` à la racine du projet :

```properties
storeFile=chemin/vers/ton.keystore
storePassword=mot-de-passe-du-keystore
keyAlias=alias-de-la-cle
keyPassword=mot-de-passe-de-la-cle
```

Ce fichier est dans `.gitignore` et **ne doit jamais être commit**.

### Modèles Vosk

Le repo embarque **les modèles français et anglais** dans
`app/src/main/assets/model-fr/` et `app/src/main/assets/model-en/`
(~65 Mo chacun). Rien à télécharger pour un build standard.

Pour remplacer un modèle, télécharge-le depuis
<https://alphacephei.com/vosk/models> :

- **FR** : `vosk-model-small-fr-pguyot`
- **EN** : `vosk-model-small-en-us-0.15`

Extrais le ZIP et copie son contenu dans `app/src/main/assets/model-<lang>/`,
en respectant la structure des modèles déjà présents.

#### Licences — à lire avant toute redistribution

Les deux modèles embarqués ne sont **pas** sous la même licence :

| Modèle | Licence | Logiciel libre ? |
|---|---|---|
| `vosk-model-small-en-us-0.15` | Apache 2.0 | Oui |
| `vosk-model-small-fr-pguyot` (embarqué) | **CC BY-NC-SA 4.0** | **Non** — clause non commerciale |

La clause *non commerciale* du modèle français signifie qu'une APK construite
depuis ce dépôt ne peut pas être redistribuée à des fins commerciales, et
n'est pas entièrement un logiciel libre. Le texte de la licence et
l'attribution sont livrés avec le modèle dans `app/src/main/assets/model-fr/`.

Il existe un modèle français sous Apache 2.0, `vosk-model-small-fr-0.22`
(~41 Mo). Il n'a pas encore été évalué dans ce projet ; le passer à ce modèle
est le correctif prévu, et un prérequis pour l'inclusion dans F-Droid.

---

## Setup sur l'appareil

1. Installer l'APK signé (accepter la prompt "Installer depuis sources inconnues")
2. Ouvrir Oblivion, définir le PIN maître
3. **Activer Device Admin** : Paramètres → Sécurité → Admins de l'appareil → Oblivion
4. **Activer le service d'accessibilité** (seulement si tu utilises Guard) : Paramètres → Accessibilité → Oblivion
5. Accorder les permissions runtime correspondant aux triggers choisis
6. Configurer les triggers voulus et les armer

Temps total de setup : ~5 minutes.

---

## Limites connues

Oblivion est construit sur des APIs Android standards et respecte les
contraintes de la plateforme. Les limites suivantes sont connues et
documentées :

- **Rien ne tourne quand le téléphone est éteint.** Chaque trigger est du
  logiciel qui s'exécute sur un appareil allumé. Un adversaire qui éteint le
  téléphone avant qu'un trigger ne parte, ou qui le coupe du réseau mobile
  (dans une cage de Faraday par exemple), neutralise les triggers à distance
  et temporels. Un téléphone éteint n'est alors protégé que par le
  chiffrement d'Android lui-même.
- **Un force-stop suspend les triggers.** Forcer l'arrêt de l'app depuis les
  Paramètres annule ses alarmes et bloque ses broadcasts jusqu'à sa prochaine
  ouverture. Cela suppose un appareil déjà déverrouillé, hors du modèle de
  menace ci-dessous, mais mieux vaut le savoir.
- **La gestion agressive de la batterie de certains constructeurs peut tuer
  les services en arrière-plan** (Xiaomi, Huawei, Samsung, Oppo…). Exclus
  Oblivion de l'optimisation batterie, et teste ta configuration sur ton
  propre appareil.
- **La carte SD externe n'est pas effacée.** `DevicePolicyManager.wipeData()`
  ne factory-reset que le stockage interne. Si ton modèle de menace inclut
  le stockage externe, chiffre-le séparément.
- **Aucune protection contre l'extraction RAM à chaud** sur les appareils
  saisis allumés et déverrouillés. Le wipe nécessite un événement déclencheur.
- **Aucune protection contre la récupération forensic post-wipe** sur les
  appareils non-chiffrés ou avec FBE cassé. Sur Android 10+ avec FBE actif,
  la récupération est extrêmement difficile.
- **L'icône de l'app reste visible** dans le launcher. Utilise un launcher
  custom pour la cacher si ton modèle de menace exige la dissimulation.
- **La reconnaissance vocale peut se déclencher par accident** si la
  phrase-clé est trop banale. Choisis 3-5 mots improbables ensemble. Le
  risque de faux positif est réel.
- **Le Dead Man's Switch peut retomber sur une vérification toutes les 15
  minutes.** Il se déclenche normalement sur une alarme exacte à l'échéance.
  Si la permission d'alarme exacte est retirée, ou si le gestionnaire de
  batterie du constructeur tue l'alarme, une vérification périodique prend
  le relais, avec jusqu'à 15 minutes de retard.
- **Le numéro expéditeur d'un SMS est usurpable.** Le réseau n'authentifie
  pas l'identité de l'expéditeur, et des passerelles commerciales permettent
  de la forger. Qui connaît ton mot-clé peut donc déclencher le wipe à
  distance. Aucun contrôle côté récepteur ne peut l'empêcher : traite le
  mot-clé comme un secret de même valeur que le PIN de détresse, et désactive
  le trigger SMS si le wipe à distance ne vaut pas ce risque. Saisir le
  numéro autorisé au format international complet (`+33…`) est plus strict
  que le format national.
- **Choisis un PIN de détresse qui n'est pas un suffixe de ton vrai PIN.**
  Les triggers du lockscreen se déclenchent dès que les derniers chiffres
  saisis correspondent au secret — c'est ce qui leur permet de fonctionner
  sans appuyer sur « valider », et de fonctionner encore après une saisie
  ratée. Conséquence : un vrai PIN `981234` déclencherait un PIN de détresse
  `1234` à chaque déverrouillage normal.

---

## Modèle de menace

Oblivion n'est utile que si un trigger se déclenche **avant** que l'adversaire
ne contrôle l'alimentation et la connectivité de l'appareil. Dans cette
fenêtre, il est conçu pour aider contre :

- ✓ La saisie, le vol ou la contrainte, quand tu peux saisir un PIN de
  détresse, ou que l'appareil reste allumé et verrouillé
- ✓ Le branchement d'un outil d'extraction USB sur un appareil allumé et
  verrouillé (USB Kill se déclenche au branchement)
- ✓ La recherche du PIN par essais sur l'écran de verrouillage (seuil de
  tentatives échouées)
- ✓ Le wipe à distance après perte ou saisie, tant que l'appareil est allumé
  et reçoit les SMS (voir la réserve sur l'usurpation plus haut)

Oblivion **n'est pas** conçu pour défendre contre :

- ✗ L'acquisition d'un appareil éteint, et les attaques matérielles (JTAG,
  chip-off)
- ✗ Un adversaire qui éteint l'appareil ou le coupe du réseau avant qu'un
  trigger ne parte
- ✗ Les outils forensiques commerciaux (Cellebrite, GrayKey…), au-delà de ce
  qu'apporte USB Kill quand on les branche sur un appareil allumé et
  verrouillé
- ✗ Les adversaires dotés d'exploits noyau ou capables de modifier le matériel
- ✗ Les appareils déjà déverrouillés et live dans les mains de l'adversaire
- ✗ Le shoulder-surfing : quelqu'un qui t'a vu taper ton vrai PIN peut
  simplement déverrouiller le téléphone
- ✗ L'exfiltration réseau pré-wipe par un malware déjà installé
- ✗ La saisie légale où tu es légalement contraint de fournir l'accès

---

## Roadmap

Triggers futurs en réflexion :

- Trigger de shake / geste de panique
- Trigger combo touches volume
- Smart logic Wi-Fi (réduction des faux positifs)
- Arm/disarm par geofence
- Désarmement par tag NFC
- Launcher furtif (cacher l'icône de l'app)
- Wipe sélectif (apps + photos au lieu du factory reset)
- Modes leurre multiples (batterie faible, pas de signal, etc.)
- Camera trap (photo front-cam sur tentative lockscreen échouée)
- "Dernier message" SMS chiffré pré-wipe

Les pull requests sont bienvenues. Merci d'ouvrir d'abord une issue pour discussion.

---

## Contribuer

Ce projet accueille les contributions de la communauté privacy et sécurité.
Merci d'ouvrir une issue pour discussion avant de soumettre une pull request
conséquente.

En contribuant, tu acceptes que ta contribution soit licenciée sous la même
licence que le projet (AGPL-3.0-or-later).

Pour les problèmes de sécurité, suis la [politique de divulgation](../SECURITY.md)
plutôt que d'ouvrir une issue publique.

---

## Licence

Ce projet est licencié sous la **GNU Affero General Public License v3.0
or later** — voir [LICENSE](../LICENSE) pour le texte complet.

L'AGPL garantit que toute version modifiée d'Oblivion, même distribuée sur
le réseau comme un service, doit rester libre et ouverte. Ça protège contre
les forks propriétaires qui pourraient éroder la confiance dans l'outil.

L'AGPL couvre le code. Les modèles Vosk embarqués sont des œuvres distinctes,
sous leurs propres licences — voir [Licences](#licences--à-lire-avant-toute-redistribution).

---

## Contact

- **GitHub** : <https://github.com/lethe-labs>
- **Sécurité** : voir [SECURITY.md](../SECURITY.md) pour la clé PGP et la politique de divulgation
- **F-Droid** : soumission prévue, une fois le modèle vocal français remplacé
  par un modèle libre

---

<div align="center">

*Aucune donnée ne quitte jamais l'appareil.*

</div>
