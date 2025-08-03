"""
# Nyx

Nyx is a private-first toolkit for secure file drops, screen sharing, and peer-to-peer media streaming — all within your local or custom mesh.

## Features

- Secure file uploads and downloads
- Peer-to-peer media streaming (local or mesh-based)
- Screen sharing within private network
- Zero dependency on external storage or servers
- Optional login with Spring Security
- Fully in-memory JSON-based temporary data handling

## Tech Stack

- Java 24
- Spring Boot 3.5+
- Thymeleaf
- Spring Security
- WebSocket
- HTML/CSS/JS

## Getting Started

### Prerequisites

- Java 17 or higher (Java 24 recommended)
- Maven 3.x

### Clone the Repo

```bash
git clone https://github.com/yourusername/nyx.git
cd nyx
```

### Build & Run

```bash
./mvnw spring-boot:run
```

Server will start on `http://localhost:8080`.

### Optional Configs

You can tweak `application.properties`:

```properties
server.port=8080
spring.thymeleaf.cache=false
spring.servlet.multipart.max-file-size=10MB
spring.servlet.multipart.max-request-size=10MB
```

> No database is used — all storage is temporary and handled in-memory.

## Development

- Hot reload is enabled via Spring DevTools
- HTML templates live in `src/main/resources/templates/`
- Static assets go under `src/main/resources/static/`

## Project Structure

```
nyx/
├── src/
│   ├── main/
│   │   ├── java/com/nyx/...
│   │   ├── resources/
│   │   │   ├── templates/
│   │   │   └── static/
│   └── test/
├── pom.xml
└── README.md
```

## License

This project is open source. License to be added.
"""
