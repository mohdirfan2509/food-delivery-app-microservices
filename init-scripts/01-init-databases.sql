-- ==============================================================================
-- Initialization Script for Food Delivery Microservices Databases
-- Creates isolated databases for each bounded microservice context.
-- ==============================================================================

CREATE DATABASE IF NOT EXISTS `userdb` CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
CREATE DATABASE IF NOT EXISTS `fooddb` CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
CREATE DATABASE IF NOT EXISTS `orderdb` CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
CREATE DATABASE IF NOT EXISTS `paymentdb` CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
CREATE DATABASE IF NOT EXISTS `notificationdb` CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;

-- Grant permissions for root or standard development user
GRANT ALL PRIVILEGES ON `userdb`.* TO 'root'@'%';
GRANT ALL PRIVILEGES ON `fooddb`.* TO 'root'@'%';
GRANT ALL PRIVILEGES ON `orderdb`.* TO 'root'@'%';
GRANT ALL PRIVILEGES ON `paymentdb`.* TO 'root'@'%';
GRANT ALL PRIVILEGES ON `notificationdb`.* TO 'root'@'%';

FLUSH PRIVILEGES;
