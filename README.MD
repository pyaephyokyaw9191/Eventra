# Eventra - Event Service Booking Platform

Eventra is a robust Spring Boot application designed to connect customers with various service providers for events. It provides a platform for users to register, browse offered services, make bookings, leave reviews, and communicate with service providers. Service providers can manage their profiles, services, bookings, and track their performance.

## ✨ Features

* **User Management & Authentication:**
    * User registration (Customer, Service Provider, Admin roles)
    * Secure login using JWT (JSON Web Tokens)
    * Role-based access control for API endpoints
    * Account management (view own details, update, delete)
* **Service Provider Features:**
    * Service Provider Profile management (service name, category, ABN, location, images)
    * Offered Services management (create, update, delete services with descriptions, pricing, images)
    * Dashboard summary for providers (booking stats, revenue, reviews)
* **Booking Management:**
    * Customers can create booking requests for services.
    * Providers can accept or reject booking requests.
    * Customers and Providers can cancel bookings under certain conditions.
    * Booking payment confirmation (simulated).
    * Marking bookings as completed.
    * Viewing booking history (for customers and providers).
    * Unique booking reference generation.
* **Notifications:**
    * Event-driven in-app notifications for booking status changes (e.g., request, accepted, rejected, confirmed, cancelled, completed).
    * Users can view and delete their notifications.
* **Reviews and Ratings:**
    * Customers can submit reviews (rating and comment) for services/providers they've booked.
    * Calculation and display of average ratings for service providers.
    * Users can manage their own reviews (update, delete).
* **Payment System (Simulated):**
    * Simulated payment processing for bookings.
    * Simulated subscription fee payment for service provider account activation.
* **Chat Functionality:**
    * Users can create/retrieve chat rooms with other users (e.g., customer with provider).
    * Sending and retrieving chat messages within a room.
* **File Storage:**
    * Upload and management of images for profile pictures, cover photos, and service images.
    * Configurable storage locations.
* **Admin Functionality:**
    * Initial admin users created by `DataInitializer`.

## 🛠️ Technologies Used

* **Backend:**
    * Java 21
    * Spring Boot 3.4.5
    * Spring Security (Authentication & Authorization)
    * Spring Data JPA (with Hibernate)
    * JWT (JSON Web Tokens) for stateless authentication
    * ModelMapper
    * Lombok
* **Database:**
    * MySQL
* **Build Tool:**
    * Apache Maven
* **API Documentation:**
    * Postman API Collection

## 📂 Project Structure

The project is organized into the following primary packages within `src/main/java/com/cedric/Eventra/`:

-   `controller/`: Contains REST API controllers that handle incoming HTTP requests and delegate to services.
-   `service/`: Houses the core business logic of the application. Interfaces define contracts, and implementation classes (`*ServiceImpl`) contain the logic. This includes sub-packages like `notification/strategy/` for specific patterns.
-   `repository/`: Includes Spring Data JPA repository interfaces for database operations.
-   `entity/`: Defines the JPA entities that map to database tables (data models).
-   `dto/`: (Data Transfer Objects) Used to shape data for API requests and responses.
-   `config/`: Holds Spring configuration classes (e.g., `ModelMapperConfig`, application-wide beans).
-   `security/`: Contains all security-related classes, including JWT utilities, filters, and custom user details services.
-   `exception/`: Defines custom exception classes and a global exception handler for consistent error responses.
-   `enums/`: Contains various enumerations used throughout the application (e.g., `UserRole`, `BookingStatus`, `ServiceCategory`, `NotificationType`).
-   `event/`: (As created for Observer pattern) Holds application event classes (e.g., `BookingCreatedEvent`).
-   `init/`: Contains the `DataInitializer` class for populating the database on startup.

Other important locations:
-   `src/main/resources/application.properties`: Main application configuration file.
-   `src/main/resources/Initialize-images/`: Source images for data initialization.

## 🚀 Setup and Installation

1.  **Prerequisites:**
    * JDK 21 or newer
    * Apache Maven 3.6+
    * MySQL Server (ensure it's running)

2.  **Database Configuration:**
    * Create a MySQL database named `eventra` (or as configured in `application.properties`).
    * Currently, database property is set up to `ddl-auto=create-drop` for testing and data initialization, if without data initialization, property can be set up `ddl-auto = update`. 
    * Update the database URL, username, and password in `src/main/resources/application.properties` if different from the defaults:
        ```properties
        spring.datasource.url = jdbc:mysql://localhost:3306/eventra
        spring.datasource.username=your_mysql_user
        spring.datasource.password=your_mysql_password
        ```


3.  **File Upload Directory Configuration:**
    * The application saves uploaded files (profile pictures, cover photos, service images) to local directories.
    * Configure the base path and subdirectories in `application.properties`. Example:
        ```properties
        file.upload-dir.base=/path/to/your/eventra_uploads
        file.upload-dir.profile-pictures=profile-pictures
        file.upload-dir.cover-photos=cover-photos
        file.upload-dir.service-images=service-images
        ```
    * Ensure the base directory (`/path/to/your/eventra_uploads` in the example) is writable by the application. The subdirectories will be created automatically if they don't exist.

4.  **Build the Project:**
    Open a terminal in the project root directory and run:
    ```bash
    mvn clean install
    ```

5.  **Run the Application:**
    You can run the application using Maven:
    ```bash
    mvn spring-boot:run
    ```
    Alternatively, you can run the main application class `EventraApplication.java` from your IDE.
    The application will start on port `9090` by default.

## 📖 API Endpoints & Documentation

The application uses Springdoc OpenAPI to generate API documentation. Once the application is running, you can access the Swagger UI at:

Key API groups include:
* `/api/auth`: User registration and login.
* `/api/users`: User account management.
* `/api/services`: Offered service management (creation by providers, Browse).
* `/api/bookings`: Booking creation and lifecycle management.
* `/api/reviews`: Creating and viewing reviews.
* `/api/notifications`: Managing user notifications.
* `/api/payments`: Simulated payment processing.
* `/api/chat`: Chat room and message management.
* `/api/reports`: Provider dashboard summaries.
* `/uploads/...`: Accessing uploaded files (requires proper serving configuration, e.g., via a resource handler).

## 🌱 Data Initialization

* The `DataInitializer` class runs on application startup if no users are found in the database. 
* It populates the database with:
    * 3 Admin users.
    * 100 Service Provider users, each with a profile and 3 offered services.
    * 100 Customer users.
    * Randomized reviews for the service providers.
* **Important:** For image initialization without placeholder pictures, we need to have the following default images in the `src/main/resources/Initialize-images/` subdirectories:
    * `profile-pictures/profile-picture.png`
    * `cover-photos/cover-photo.jpg`
    * `service-images/service-image.jpg`

## 📝 To-Do / Future Enhancements

* **Full Payment Gateway Integration:** Move beyond simulated payments and integrate a real payment provider like Stripe for bookings and subscriptions.
* **Advanced Search & Filtering:** Implement more sophisticated search and filtering for services (e.g., by location, rating, availability, category).
* **Admin Dashboard & Management Tools:** Develop comprehensive admin functionalities (user management, content moderation, site statistics).
* **Email Notifications:** Extend `NotificationServiceImpl` to send email notifications in addition to in-app ones.
* **Enhanced Reporting:** More detailed reports for providers and admins.
* **Calendar Integration:** For managing bookings and availability.

## 📄 License
(License to be determined)