package main.java.elya.propertiesbeans;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.PropertySource;

import java.util.ArrayList;
import java.util.List;

@Configuration
@ConfigurationProperties(prefix = "apicredentials")
@PropertySource(value = "classpath:apicredentials.properties", ignoreResourceNotFound = true)
public class ApiEmulatorCredentials {
    private List<Credential> users = new ArrayList<>();

    public List<Credential> getUsers() {
        return users;
    }

    public void setUsers(List<Credential> users) {
        this.users = users;
    }

    @Override
    public String toString() {
        return "ApiEmulatorCredentials{" +
                "users=" + users +
                '}';
    }

    public static class Credential {
        private String login;
        private String password;

        public String getLogin() {
            return login;
        }

        public void setLogin(String login) {
            this.login = login;
        }

        public String getPassword() {
            return password;
        }

        public void setPassword(String password) {
            this.password = password;
        }

        @Override
        public String toString() {
            return "Credential{" +
                    "login='" + login + '\'' +
                    ", password='" + password + '\'' +
                    '}';
        }
    }
}
