# DreamMapper API

Yapay zekâ destekli rüya analiz uygulaması - Spring Boot REST API

## Özellikler

- **JWT Authentication**: Güvenli token tabanlı kimlik doğrulama
- **Rüya Yönetimi**: Rüyaları kaydetme, güncelleme, silme ve arama
- **AI Analiz**: Google Gemini API ile rüya analizi
- **Kullanıcı Profili**: Profil güncelleme ve şifre değiştirme
- **Rüya Geçmişi**: Analiz geçmişini görüntüleme
- **RESTful API**: Modern REST API tasarımı
- **Swagger UI**: API dokümantasyonu ve test arayüzü

## Gereksinimler

- Java 17+
- Maven 3.6+
- PostgreSQL 12+
- Google Gemini API Key

## Kurulum

### 1. Veritabanı Kurulumu

```sql
CREATE DATABASE dreammapperdb;
```

### 2. Konfigürasyon

`src/main/resources/application.properties` dosyasını oluşturun:

```properties
# DATABASE CONFIG
spring.datasource.url=jdbc:postgresql://localhost:5432/dreammapperdb
spring.datasource.username=postgres
spring.datasource.password=your_password

# JPA CONFIG
spring.jpa.hibernate.ddl-auto=update
spring.jpa.show-sql=true
spring.jpa.properties.hibernate.dialect=org.hibernate.dialect.PostgreSQLDialect

# SERVER CONFIG
server.port=8080

# GEMINI API
gemini.api.key=YOUR_GEMINI_API_KEY

# JWT CONFIG
jwt.secret=your-secret-key-min-32-characters-long
jwt.expiration-ms=3600000
jwt.refresh-expiration-ms=604800000

# CORS
cors.allowed-origins=http://localhost:3000
```

### 3. Uygulamayı Çalıştırma

```bash
mvn spring-boot:run
```

veya

```bash
./mvnw spring-boot:run
```

## API Dokümantasyonu

Uygulama çalıştıktan sonra Swagger UI'ya erişebilirsiniz:

- **Swagger UI**: http://localhost:8080/swagger-ui.html
- **API Docs**: http://localhost:8080/api-docs

## API Endpoints

### Authentication

- `POST /api/auth/register` - Kullanıcı kaydı
- `POST /api/auth/login` - Giriş yapma
- `POST /api/auth/refresh` - Token yenileme

### Dreams

- `GET /api/dreams` - Kullanıcının rüyalarını listele (pagination)
- `GET /api/dreams/{id}` - Rüya detayı
- `POST /api/dreams` - Yeni rüya ekle
- `PUT /api/dreams/{id}` - Rüya güncelle
- `DELETE /api/dreams/{id}` - Rüya sil
- `GET /api/dreams/search` - Rüya ara
- `PATCH /api/dreams/{id}/favorite` - Favori işaretle/kaldır
- `PATCH /api/dreams/{id}/tags` - Tag'leri güncelle

### Analysis

- `POST /api/analysis/dream` - Rüya analizi yap
- `GET /api/analysis/dream/{dreamId}/history` - Analiz geçmişi

### Users

- `GET /api/users/me` - Mevcut kullanıcı bilgileri
- `GET /api/users/{id}` - Kullanıcı bilgileri
- `PUT /api/users/profile` - Profil güncelle
- `PUT /api/users/password` - Şifre değiştir
- `DELETE /api/users/{id}` - Hesabı sil

##  Güvenlik

- Tüm API endpoint'leri (auth hariç) JWT token gerektirir
- Token'lar Authorization header'da `Bearer <token>` formatında gönderilmelidir
- Şifreler BCrypt ile hash'lenir
- CORS yapılandırması mevcuttur

##  Test

```bash
mvn test
```

## Build

```bash
mvn clean package
```

JAR dosyası `target/dreammapper-0.0.1-SNAPSHOT.jar` konumunda oluşturulur.

## 🛠️ Teknolojiler

- **Spring Boot 3.5.6**
- **Spring Security** - Güvenlik
- **Spring Data JPA** - Veritabanı erişimi
- **PostgreSQL** - Veritabanı
- **JWT** - Token tabanlı kimlik doğrulama
- **Lombok** - Boilerplate kod azaltma
- **Swagger/OpenAPI** - API dokümantasyonu
- **Google Gemini API** - AI analiz

##  Notlar

- JWT secret key en az 32 karakter olmalıdır
- Production ortamında `application.properties` dosyasını `.gitignore`'a ekleyin
- Gemini API key'i environment variable olarak da ayarlanabilir



