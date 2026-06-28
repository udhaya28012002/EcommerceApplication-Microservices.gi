@echo off

echo Building all modules...
call mvn clean install

if errorlevel 1 (
    echo Build failed!
    pause
    exit /b 1
)

echo Build completed successfully.

echo Starting API-Gateway...
start "api-gateway" cmd /k "mvn -pl api-gateway spring-boot:run"

echo Starting Cart-Service...
start "Cart-Service" cmd /k "mvn -pl cart-service spring-boot:run"

echo Starting Discount-Service...
start "Discount-Service" cmd /k "mvn -pl discount-service spring-boot:run"

echo Starting Inventory-Service...
start "Inventory-Service" cmd /k "mvn -pl inventory-service spring-boot:run"

echo Starting Order-Service...
start "Order-Service" cmd /k "mvn -pl order-service spring-boot:run"

echo Starting Payment-Service...
start "Payment-Service" cmd /k "mvn -pl payment-service spring-boot:run"

echo Starting Products-Service...
start "Products-Service" cmd /k "mvn -pl products-service spring-boot:run"

echo Starting Users-Service...
start "Users-Service" cmd /k "mvn -pl users-service spring-boot:run"

echo All services started.