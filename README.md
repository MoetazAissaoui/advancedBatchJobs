# Job Scheduler Application - README

## Overview
This is a Spring Boot application for managing scheduled jobs with JWT authentication. The application provides REST APIs for user registration, authentication, and job scheduling operations.

## Prerequisites
- Java 17 or higher
- Maven 3.8+
- PostgreSQL 12+
- Postman (for API testing)

## Installation & Setup

### 1. Clone the Repository
```bash
git clone https://github.com/your-repository/advancedBatchJobs.git
cd advancedBatchJobs
```

### 2. Build the Application
```bash
mvn clean install
```
This will:
- Download all dependencies
- Run tests
- Create an executable JAR file in the `target` directory

### 3. Database Configuration
Modify the `application.properties` file with your PostgreSQL credentials:

```properties
# Postgres configuration
spring.datasource.url=jdbc:postgresql://localhost:5432/scheduler
spring.datasource.username=admin
spring.datasource.password=admin
spring.jpa.hibernate.ddl-auto=update

# JWT Configuration (generate your own secret)
jwt.secret=your-256-bit-secret-base64-encoded
jwt.expiration=86400000 # 24 hours
```

### 4. Run the Application

#### Option A: Run from Command Line
```bash
java -jar target/your-application-name.jar
```

#### Option B: Run in IntelliJ
1. Open the project in IntelliJ
2. Locate the main class (`BatchJobsApplication.java`)
3. Right-click and select "Run"

## API Documentation

### Import Postman Collection
Import the Postman collection to test the APIs:
[Job Scheduler API Tests Collection](https://github.com/MoetazAissaoui/advancedBatchJobs/blob/main/Job%20Scheduler%20API%20Tests.postman_collection.json)

### Available Endpoints

#### Authentication
- `POST /api/auth/register` - Register a new user
- `POST /api/auth/authenticate` - Authenticate and get JWT token

#### Job Operations
- `POST /api/jobs` - Create a new job
- `GET /api/jobs` - List all jobs
- `GET /api/jobs/{id}` - Get job details
- `PUT /api/jobs/{id}` - Update a job
- `DELETE /api/jobs/{id}` - Delete a job

## Database Setup
1. Create a PostgreSQL database named `scheduler`
2. The application will automatically create tables on startup (due to `spring.jpa.hibernate.ddl-auto=update`)

## Security
- Uses JWT for authentication
- All job endpoints require authentication
- Default user role is "USER"

## Troubleshooting

### Common Issues
1. **Database Connection Errors**:
    - Verify PostgreSQL is running
    - Check credentials in `application.properties`
    - Ensure database `scheduler` exists

2. **JWT Errors**:
    - Make sure you've set a proper 256-bit secret key
    - Tokens expire after 24 hours by default

3. **Build Errors**:
    - Run `mvn clean` then `mvn install`
    - Check Java version (requires 17+)

## License
[MIT License](LICENSE)