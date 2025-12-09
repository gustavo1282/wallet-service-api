#!/bin/bash

echo ">>> Deslacrando Vault (Unseal)..."

KEY1=$(sed -n 's/Unseal Key 1: //p' init-keys.txt)
KEY2=$(sed -n 's/Unseal Key 2: //p' init-keys.txt)
KEY3=$(sed -n 's/Unseal Key 3: //p' init-keys.txt)

vault operator unseal $KEY1
vault operator unseal $KEY2
vault operator unseal $KEY3

echo ">>> Vault desbloqueado com sucesso."
