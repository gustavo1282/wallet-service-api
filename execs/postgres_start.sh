# Defina suas credenciais
HOST="localhost" # Usamos localhost porque o docker exec já está no contexto do host
PORT="5432"
USER="wallet_user"
DATABASE="wallet_db"
PASSWORD="wallet_pass"

# Armazenar o nome do contêiner para reutilização
CONTAINER_NAME="cont-wallet-postgres"


# Verifique o status
docker exec -i -h $HOST -p $PORT -U $USER > /dev/null 2>&1

if [ $? -eq 0 ]; then
    echo "Servidor PostgreSQL está pronto e aceitando conexões."
else
    echo "Servidor PostgreSQL não está pronto ou não pode ser contatado."
fi



# Teste a conexão e execute um comando simples dentro do contêiner
docker exec -i "$CONTAINER_NAME" psql -h "$HOST" -p "$PORT" -U "$USER" -d "$DATABASE" -c "SELECT 1"

if [ $? -eq 0 ]; then
    echo "Conexão com PostgreSQL bem-sucedida!"
else
    echo "Falha na conexão com PostgreSQL."
fi