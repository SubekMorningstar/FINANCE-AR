# Accounts Receivable (AR) System

Sistem manajemen piutang usaha (Accounts Receivable) berbasis REST API menggunakan Kotlin dan Ktor Framework.

## 👥 Kontribusi Tim

| Nama | Peran | Kontribusi |
|------|-------|------------|
| **Member 1** | Lead Domain & Analis | Class Diagram, Business Rules, Domain Entities |
| **Member 2** | Lead Implementasi OOP/API | Kotlin Implementation, Ktor API, Services |
| **Member 3** | Lead Pengujian & Dokumentasi | Postman Testing, Unit Tests, Documentation |

---

## 🏗️ Arsitektur

```
src/main/kotlin/com/pbo/ar/
├── api/              # REST API Layer
│   ├── dto/          # Data Transfer Objects
│   ├── plugins/      # Ktor Plugins
│   └── routes/       # API Route Handlers
├── data/             # Data Layer
│   ├── database/     # Tables & Connection
│   └── repository/   # Repository Pattern
├── di/               # Dependency Injection
└── domain/           # Domain Layer
    ├── model/        # Entity & Value Objects
    └── service/      # Business Logic
```

---

## 🎯 Fitur Utama

### Domain Entities
- **Customer** - Data pelanggan dengan credit limit
- **Invoice** - Faktur dengan line items dan status lifecycle
- **Payment** - Pembayaran dan alokasi ke faktur

### API Endpoints
| Method | Endpoint | Deskripsi |
|--------|----------|-----------|
| GET/POST | `/api/customers` | CRUD Customer |
| GET/POST | `/api/invoices` | CRUD Invoice |
| POST | `/api/invoices/{id}/items` | Tambah line item |
| POST | `/api/invoices/{id}/send` | Kirim invoice |
| POST | `/api/payments` | Buat pembayaran |
| POST | `/api/payments/{id}/allocate` | Alokasi ke invoice |
| GET | `/api/reports/aging` | Laporan aging |
| GET | `/api/reports/summary` | Summary piutang |

---

## 🚀 Quick Start

### 1. Setup Database
```sql
CREATE DATABASE ar_system;
```

### 2. Run API Server
```bash
gradle runApi
```

Server: `http://localhost:8080`

### 3. Import Postman Collection
File: `postman/AR_System_API.postman_collection.json`

---

## 📐 Class Diagram

Lihat file: `docs/class-diagram.puml`

### Penerapan OOP:
- **Enkapsulasi**: Service classes membungkus business logic
- **Abstraksi**: Repository pattern, Result sealed class
- **Inheritance**: InvoiceStatus, PaymentMethod sealed classes
- **Polimorfisme**: Status behaviors, method dispatching

### Domain Invariants:
1. Invoice.totalAmount = Σ(LineItem.amount) × (1 + taxRate)
2. PaymentAllocation ≤ min(Payment.unallocated, Invoice.balance)
3. InvoiceStatus transitions mengikuti valid state machine
4. Customer.creditLimit ≥ outstanding balance

---

## 🛠️ Tech Stack

- **Kotlin** 1.9.21
- **Ktor** 2.3.7 (REST API)
- **Exposed** 0.45.0 (ORM)
- **MySQL** 8.0
- **HikariCP** (Connection Pool)

---

## 📝 License

MIT License - PBO Project 2024
# FINANCE-AR
