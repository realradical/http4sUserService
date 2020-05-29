# ssh into postgres docker
docker exec -it http4suserservice_postgres_1 psql -U postgres

# build docker image
docker build -t jacob-userservice .

# tag local repo
docker tag jacob-userservice localhost:5000/jacob-userservice

# push to local docker repo
docker push localhost:5000/jacob-userservice