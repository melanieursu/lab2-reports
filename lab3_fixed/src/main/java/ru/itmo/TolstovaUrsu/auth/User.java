package ru.itmo.TolstovaUrsu.auth;
public final class User {

    private final String login;
    private final String passwordHash;

    public User(String login, String passwordHash) {
        this.login        = login;
        this.passwordHash = passwordHash;
    }

    public String getLogin()        { return login; }
    public String getPasswordHash() { return passwordHash; }

    @Override
    public String toString() {
        return "User{login='" + login + "'}";
    }
}
