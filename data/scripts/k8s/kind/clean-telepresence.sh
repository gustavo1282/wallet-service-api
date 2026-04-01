#!/bin/bash

# Garante que o script trate caminhos do Windows no Git Bash
USER_PROFILE_PATH=$(cygpath "$USERPROFILE")

echo "Limpando processos antigos do Telepresence..."
# No Git Bash, usamos taskkill.exe explicitamente
taskkill.exe //F //IM telepresence.exe //T 2>/dev/null || true

echo "Limpando arquivos de cache e sockets..."
# Usa o caminho convertido para o formato que o rm entende
rm -rf "${USER_PROFILE_PATH}/AppData/Local/telepresence" 2>/dev/null || true

echo "Resetando daemon do Telepresence..."
telepresence quit -s 2>/dev/null || true

#echo "Tentando nova conexão..."
#telepresence connect
