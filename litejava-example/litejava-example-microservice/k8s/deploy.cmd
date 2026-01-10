@echo off
echo === LiteJava K8s Deployment ===

REM 1. Build all services
echo [1/4] Building services...
cd ..
call mvn clean package -DskipTests -pl gateway-k8s,product-service,user-service,order-service -am
if errorlevel 1 (
    echo Build failed!
    exit /b 1
)

REM 2. Build Docker images
echo [2/4] Building Docker images...
docker build -t litejava/gateway-k8s:latest gateway-k8s
docker build -t litejava/product-service:latest product-service
docker build -t litejava/user-service:latest user-service
docker build -t litejava/order-service:latest order-service

REM 3. Create namespace
echo [3/4] Creating namespace...
kubectl apply -f k8s/namespace.yaml

REM 4. Deploy services
echo [4/4] Deploying services...
kubectl apply -f k8s/product-service.yaml
kubectl apply -f k8s/user-service.yaml
kubectl apply -f k8s/order-service.yaml
kubectl apply -f k8s/gateway.yaml

echo.
echo === Deployment Complete ===
echo.
echo Check status: kubectl get pods -n litejava
echo Gateway URL:  kubectl get svc gateway -n litejava
