flyway -url="jdbc:mysql://localhost:3306/sftv2" -user=sftv2_usr -password=asdf1234 \
  -schemas=sftv2 \
  -locations="filesystem:db/src/main/resources/db" \
  migrate
