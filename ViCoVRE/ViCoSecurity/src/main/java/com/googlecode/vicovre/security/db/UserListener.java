package com.googlecode.vicovre.security.db;

public interface UserListener {

    public void addUser(String username, Role role, String homeFolder);

    public void changeRole(String username, Role role, String homeFolder);

    public void deleteUser(String username, Role role, String homeFolder);
}
