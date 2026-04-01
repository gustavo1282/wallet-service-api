#!/bin/bash

set -e

echo "🚀 Deploy Wallet Service no KIND"

# Namespace (opcional)
NAMESPACE=eco-wallet-api

echo "📌 Aplicando manifests..."

kubectl apply -f k8s/configmap.yaml -n $NAMESPACE
kubectl apply -f k8s/secret.yaml -n $NAMESPACE
kubectl apply -f k8s/postgres.yaml -n $NAMESPACE
kubectl apply -f k8s/deployment.yaml -n $NAMESPACE
kubectl apply -f k8s/service.yaml -n $NAMESPACE

echo "⏳ Aguardando pods..."

kubectl rollout status deployment/wallet-service -n $NAMESPACE

echo "📊 Status geral:"
kubectl get pods -n $NAMESPACE
kubectl get svc -n $NAMESPACE

echo ""
echo "✅ Deploy finalizado"
echo "👉 Acesse: http://localhost:30080/${APP_NAME}"
