#!/bin/bash

set -e

CLUSTER_NAME="wallet-cluster"
IMAGE_NAME="wallet-service-api:1.0.9"
K8S_DIR="./k8s"

echo "?? Validando contexto Kubernetes..."
kubectl cluster-info > /dev/null 2>&1 || {
  echo "? kubectl n�o conectado"
  exit 1
}

CURRENT_CONTEXT=$(kubectl config current-context)
echo "?? Contexto atual: $CURRENT_CONTEXT"

if [[ "$CURRENT_CONTEXT" != *"$CLUSTER_NAME"* ]]; then
  echo "? Voc� n�o est� no cluster kind correto ($CLUSTER_NAME)"
  exit 1
fi

echo "?? Validando exist�ncia da imagem Docker..."
docker image inspect $IMAGE_NAME > /dev/null 2>&1 || {
  echo "? Imagem $IMAGE_NAME n�o encontrada. Fa�a o build antes."
  exit 1
}

echo "?? Validando manifests Kubernetes..."
kubectl apply --dry-run=client -f $K8S_DIR > /dev/null || {
  echo "? Erro nos manifests"
  exit 1
}

echo "?? Carregando imagem no kind..."
kind load docker-image $IMAGE_NAME --name $CLUSTER_NAME

echo "?? Aplicando manifests..."
kubectl apply -f $K8S_DIR

echo "? Aguardando rollout..."
kubectl rollout status deployment/wallet-api

echo "?? Validando pods..."
kubectl get pods -l app=wallet-api

echo "?? Validando service..."
kubectl get svc wallet-api

echo "? Deploy finalizado com sucesso!"
echo "?? Acesse: http://localhost:30080/wallet-service-api"
