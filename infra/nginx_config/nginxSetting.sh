openssl req -newkey rsa:4096 -days 30 -nodes -x509 -subj "/C=KR/ST=Seoul/L=Seoul/O=likelion/OU=${ORGANIZATION}/CN=${TEAM_NAME}.BESP.xyz" -keyout "/etc/ssl/${TEAM_NAME}.BESP.xyz.key" -out "/etc/ssl/${TEAM_NAME}.BESP.xyz.crt" 2>/dev/null

nginx -g 'daemon off;'