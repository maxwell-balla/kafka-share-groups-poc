# 🚀 Kafka Share Groups POC — Notification Processing

POC démontrant **KIP-932 (Queues for Kafka)** avec **Spring Kafka 4.1.0-M2** sur **Kafka 4.2.0**.

Use case : distribution de notifications (email, push, SMS) entre workers via Share Groups —
plus besoin de RabbitMQ à côté de Kafka pour les workloads de type queue.

---

## Prérequis

- Docker & Docker Compose
- Java 21+
- Maven 3.9+

---

## Step 1 — Lancer Kafka 4.2 avec Share Groups

```bash
docker compose up -d
```

Ce qui se passe :
1. Un broker Kafka 4.2.0 démarre en mode KRaft (pas de ZooKeeper)
2. Le container `kafka-init` active la feature `share.version=1`
3. Le topic `notifications` est créé (3 partitions)

Vérifie que tout est ok :

```bash
# Le broker est healthy ?
docker compose ps

# Share groups activés ?
docker exec kafka-broker /opt/kafka/bin/kafka-features.sh --bootstrap-server localhost:9092 describe
# → Tu dois voir share.version=1
```

---

## Step 2 — Lancer l'application Spring Boot

```bash
./mvnw spring-boot:run
```

L'app démarre avec :
- **3 threads** de share consumer écoutant le topic `notifications`
- Un endpoint REST pour envoyer des notifications

---

## Step 3 — Tester le Share Group

### Envoyer une notification unique

```bash
curl -X POST http://localhost:8080/api/notifications \
  -H "Content-Type: application/json" \
  -d '{
    "id": "notif-001",
    "recipient": "maxwell@example.com",
    "type": "EMAIL",
    "subject": "Bienvenue",
    "body": "Votre compte a été créé"
  }'
```

### Envoyer un burst de 50 notifications

```bash
curl -X POST "http://localhost:8080/api/notifications/burst?count=50"
```

Observe les logs : les 50 messages sont distribués entre les 3 threads consumer.
Chaque thread traite des messages de n'importe quelle partition — c'est la différence
avec un consumer group classique.

---

## Step 4 — Observer la distribution

```bash
# Voir l'état du share group
docker exec kafka-broker /opt/kafka/bin/kafka-share-groups.sh \
  --bootstrap-server localhost:9092 \
  --describe \
  --group notification-workers
```

---

## Ce qu'il faut observer

| Aspect | Consumer Group classique | Share Group (ce POC) |
|--------|-------------------------|----------------------|
| Parallélisme max | = nombre de partitions (3) | illimité |
| Distribution | par partition | par message |
| Acquittement | par offset | par message (ACCEPT/RELEASE/REJECT) |
| Ordre | garanti par partition | pas garanti |
| Use case | événements métier ordonnés | tâches indépendantes |

---

## Structure du projet

```
kafka-share-groups-poc/
├── docker-compose.yml                    # Kafka 4.2 + activation Share Groups
├── pom.xml                               # Spring Boot 4.1.0-M3 + Spring Kafka
└── src/main/java/com/maxwell/poc/notification/
    ├── KafkaShareGroupsPocApplication.java
    ├── config/KafkaConfig.java           # Producer + Share Consumer factory
    ├── consumer/NotificationShareConsumer.java  # ← Le share consumer
    ├── model/NotificationRequest.java    # Record = notification à traiter
    └── producer/NotificationProducer.java # REST endpoint pour les tests
```

---

## Pour aller plus loin

- [Spring Kafka — Share Consumer docs](https://docs.spring.io/spring-kafka/reference/kafka/kafka-queues.html)
- [KIP-932 — Queues for Kafka](https://cwiki.apache.org/confluence/display/KAFKA/KIP-932)
- [Kafka 4.2 Release Notes](https://kafka.apache.org/42/getting-started/upgrade/)

---

*POC créé par Maxwell — [maxwellballa.substack.com](https://maxwellballa.substack.com)*
