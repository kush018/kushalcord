package org.example;

import org.postgresql.util.PSQLException;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.sql.*;
import java.util.LinkedList;
import java.util.List;

public class PSQLManager {

    private static final String DB_IP_FILE = "conf/db_addr";
    private static final String DB_CREDS_FILE = "conf/db_creds";

    private Connection connection;

    private Statement stmt;

    private boolean isSuccessful;

    private boolean psqlManagerCreatedSuccessfully;

    public PSQLManager() throws ClassNotFoundException, SQLException {
        psqlManagerCreatedSuccessfully = false;

        isSuccessful = true;

        FileReader fileReader;
        try {
            fileReader = new FileReader(DB_IP_FILE);
        } catch (FileNotFoundException e) {
            System.out.println("File " + DB_IP_FILE + " not found. Please create the file and fill it with the address of the database for the bot." +
                    " Address of database is: ip-address-of-database/database-name");
            return;
        }
        BufferedReader bufferedReader = new BufferedReader(fileReader);
        String dbAddr;
        try {
            dbAddr = bufferedReader.readLine();
        } catch (IOException e) {
            System.out.println("IOException occurred while reading file: " + DB_IP_FILE);
            return;
        }

        if (dbAddr == null) {
            System.out.println("Database address not specified in file: " + DB_IP_FILE);
            return;
        }

        FileReader fileReader1;
        try {
            fileReader1 = new FileReader(DB_CREDS_FILE);
        } catch (FileNotFoundException e) {
            System.out.println("File " + DB_CREDS_FILE + " not found. Please create the file and fill it with the credentials of the database" +
                    " for the bot. Write the credentials as follows: database-username[return]database-password");
            return;
        }
        BufferedReader bufferedReader1 = new BufferedReader(fileReader1);
        String dbUser, dbPass;
        try {
            dbUser = bufferedReader1.readLine();
            dbPass = bufferedReader1.readLine();
        } catch (IOException e) {
            System.out.println("IO Exception occured while reading file: " + DB_CREDS_FILE);
            return;
        }
        if (dbUser == null || dbPass == null) {
            System.out.println("Database username and password not specified in file: " + DB_CREDS_FILE);
            return;
        }

        Class.forName("org.postgresql.Driver");
        connection = DriverManager.getConnection("jdbc:postgresql://" + dbAddr, dbUser, dbPass);
        stmt = connection.createStatement();

        psqlManagerCreatedSuccessfully = true;
    }

    public boolean isPsqlManagerCreatedSuccessfully() {
        return psqlManagerCreatedSuccessfully;
    }

    public long getBalance(String id) throws SQLException {
        String query = "SELECT * FROM bank WHERE id='" + id + "';";
        ResultSet resultSet = stmt.executeQuery(query);
        long balance = 0;
        isSuccessful = false;
        while (resultSet.next()) {
            balance = resultSet.getLong("balance");
            isSuccessful = true;
        }
        resultSet.close();
        return balance;
    }

    public String getItems(String id) throws SQLException {
        String query = "SELECT * FROM bank WHERE id='" + id + "';";
        ResultSet resultSet = stmt.executeQuery(query);
        String items = "";
        isSuccessful = false;
        while (resultSet.next()) {
            items = resultSet.getString("items");
            isSuccessful = true;
        }
        resultSet.close();
        return items;
    }

    public long getDailyTaken(String id) throws SQLException {
        String query = "SELECT * FROM bank WHERE id='" + id + "';";
        ResultSet resultSet = stmt.executeQuery(query);
        long dailyTaken = 0;
        isSuccessful = false;
        while (resultSet.next()) {
            dailyTaken = resultSet.getLong("dailytaken");
            isSuccessful = true;
        }
        resultSet.close();
        return dailyTaken;
    }

    public void setDailyTaken(String id, long newDailyTaken) throws SQLException {
        String sql = "UPDATE bank SET dailytaken=" + newDailyTaken + " WHERE id='" + id + "';";
        stmt.executeUpdate(sql);
    }

    public void deposit(String id, long amt) throws SQLException {
        String sql = "UPDATE bank SET balance=" + (amt + getBalance(id)) + " WHERE id='" + id + "';";
        stmt.executeUpdate(sql);
    }

    public void withdraw(String id, long amt) throws SQLException {
        String sql = "UPDATE bank SET balance=" + (getBalance(id) - amt) + " WHERE id='" + id + "';";
        stmt.executeUpdate(sql);
    }

    public void setBalance(String id, long newBalance) throws SQLException {
        String sql = "UPDATE bank SET balance=" + newBalance + " WHERE id='" + id + "';";
        stmt.executeUpdate(sql);
    }

    public void setItems(String id, String newItems) throws SQLException {
        String sql = "UPDATE bank SET items='" + newItems + "' WHERE id='" + id + "';";
        stmt.executeUpdate(sql);
    }

    public void addAccount(String id) throws SQLException {
        String sql = "INSERT INTO bank (id, balance, items, dailytaken) VALUES ('" + id + "', 0, '', 0);";
        stmt.executeUpdate(sql);
    }

    public void addGuildToXpSystem(String id) throws SQLException {
        String sql = "CREATE TABLE xp_" + id + " (" +
                "id text NOT NULL," +
                "xp bigint," +
                "lastmessage bigint," +
                "PRIMARY KEY (id)" +
                ");";
        try {
            stmt.executeUpdate(sql);
        } catch (PSQLException ignored) {}
    }

    public void removeGuildFromXpSystem(String id) throws SQLException {
        String sql = "DROP TABLE xp_" + id + ";";
        stmt.executeUpdate(sql);
    }

    public void addUserToGuildXpSystem(String userId, String guildId) throws SQLException {
        String sql = "INSERT INTO xp_" + guildId + " (id, xp, lastmessage) VALUES " +
                "('" + userId + "', 0, 0);";
        try {
            stmt.executeUpdate(sql);
        } catch (PSQLException ignored) {}
    }

    public void removeUserFromGuildXpSystem(String userId, String guildId) throws SQLException {
        String sql = "DELETE FROM xp_" + guildId + " WHERE id='" + userId + "';";
        stmt.executeUpdate(sql);
    }

    public long getXpOfUser(String userId, String guildId) throws SQLException {
        String sql = "SELECT * FROM xp_" + guildId + " WHERE id='" + userId + "';";
        ResultSet resultSet = stmt.executeQuery(sql);
        isSuccessful = false;
        long xp = 0;
        while (resultSet.next()) {
            isSuccessful = true;
            xp = resultSet.getLong("xp");
        }
        resultSet.close();
        return xp;
    }

    public long getRankOfUser(String userId, String guildId) throws SQLException {
        String sql = "SELECT * FROM xp_" + guildId + " ORDER BY xp DESC;";
        ResultSet resultSet = stmt.executeQuery(sql);
        LinkedList<String> xpList = new LinkedList<>();
        while (resultSet.next()) {
            xpList.add(resultSet.getString("id"));
        }
        return xpList.indexOf(userId);
    }

    public long getLastMessageTime(String userId, String guildId) throws SQLException {
        String sql = "SELECT * FROM xp_" + guildId + " WHERE id='" + userId + "';";
        ResultSet resultSet = stmt.executeQuery(sql);
        long msgTime = 0;
        isSuccessful = false;
        while (resultSet.next()) {
            isSuccessful = true;
            msgTime = resultSet.getLong("lastmessage");
        }
        return msgTime;
    }

    public void setXpOfUser(String userId, String guildId, long newXp) throws SQLException {
        String sql = "UPDATE xp_" + guildId + " SET xp=" + newXp + " WHERE id='" + userId + "';";
        stmt.executeUpdate(sql);
    }

    public List<String> getTopFromGuildXpLeaderboard(String guildId, long n) throws SQLException {
        String sql = "SELECT * FROM xp_" + guildId + " ORDER BY xp DESC;";
        ResultSet resultSet = stmt.executeQuery(sql);
        LinkedList<String> topList = new LinkedList<>();
        for (long i = 0; i < n && resultSet.next(); i++) {
            topList.add(resultSet.getString("id"));
        }
        return topList;
    }

    public void setLastMessageTimeOfUser(String userId, String guildId, long newLastMessageTime) throws SQLException {
        String sql = "UPDATE xp_" + guildId + " SET lastmessage=" + newLastMessageTime + " WHERE id='" + userId + "';";
        stmt.executeUpdate(sql);
    }

    public void removeAccount(String id) throws SQLException {
        String sql = "DELETE FROM bank WHERE id='" + id + "';";
        stmt.executeUpdate(sql);
    }

    public void close() throws SQLException {
        stmt.close();
        connection.close();
    }

    public boolean isOperationSuccessful() {
        return this.isSuccessful;
    }
}
