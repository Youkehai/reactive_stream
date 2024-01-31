package com.hs.reactive.reactivestream.r2dbc;

import io.asyncer.r2dbc.mysql.MySqlConnectionConfiguration;
import io.asyncer.r2dbc.mysql.MySqlConnectionFactory;
import io.r2dbc.spi.ConnectionFactory;
import reactor.core.publisher.Mono;

import java.io.IOException;

public class R2DbcDemo {

    public static void main(String[] args) throws IOException {
        //1.获取连接
        MySqlConnectionConfiguration mySqlConnectionConfiguration = MySqlConnectionConfiguration.builder().host("xxx").port(3306).username("root").database("xxx").password("xx").build();
        ConnectionFactory connectionFactory = MySqlConnectionFactory.from(mySqlConnectionConfiguration);

        //2.获取连接
        Mono.from(connectionFactory.create()).flatMapMany(connection -> connection.createStatement("select * from system_users where id in(1,117)").execute()).flatMap(result -> result.map(readable -> new User(readable.get("id", Long.class), readable.get("nickname", String.class)))).subscribe(user -> {
            System.out.println(user.id);
            System.out.println(user.nickname);
        });
        System.in.read();
    }

    static class User {
        Long id;

        String nickname;

        public User(Long id, String nickname) {
            this.id = id;
            this.nickname = nickname;
        }
    }
}
