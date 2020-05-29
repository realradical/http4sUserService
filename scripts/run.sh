#!/bin/bash
# This is the master script that acts as the ENTRYPOINT for docker.
#set -x

#Run the application
java -cp http4sUserService.jar -Xmx1G com.jacobshao.userservice.UserServiceServer \
  --dbUrl "$DB_URL" \
  --dbUser "$DB_USER" \
  --dbPassword "$DB_PASSWORD"
