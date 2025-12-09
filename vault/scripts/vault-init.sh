#!/bin/bash

echo ">>> Inicializando Vault..."

vault operator init > init-keys.txt

echo ">>> Arquivo init-keys.txt gerado com unseal keys e root token."
echo "Guarde esse arquivo em local seguro."
