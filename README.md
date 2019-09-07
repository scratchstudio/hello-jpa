# hello-jpa

## 테스트 DB 설정

```shell
$ docker-compose build
$ docker-compose up -d
$ docker exec -it jpa-test-db bash

# docker 접속
root@dd755ff299ef:/# psql -d test -U foo

# mysql 접속
root:/# mysql -u user -p db
root:/# show tables;
```