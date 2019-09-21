# jpa-scratch

## 테스트 DB 설정

```shell
$ docker-compose build
$ docker-compose up -d
$ docker exec -it jpa-test-db bash

# mysql 접속
root:/# mysql -u user -p db
root:/# show tables;
```