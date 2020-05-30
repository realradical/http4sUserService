# Http4s User Service
## Local Setup
1. Build Dokcer image\
`docker build -t jacob-userservice`
2. Tag local Docker repo\
`docker tag jacob-userservice localhost:5000/jacob-userservice`
3. Push to local Docker repo\
`docker push localhost:5000/jacob-userservice`
4. Start PostgreSQL and UserService\
`docker-compose up`

Now you can hit our endpoint at `localhost:8080/users`