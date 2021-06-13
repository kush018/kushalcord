package org.example;

import java.sql.*;
import java.util.LinkedList;
import java.util.List;

/**
 * A class which has functions to help in managing and doing database stuff for kushalcord
 * 
 * @author Kushal Galrani
*/
public class DBManager {

    /**
     * The path to the database file
     */
    private static final String DB_FILE = "database.db";

    /**
     * The connection object, which represents a connection to the database
     */
    private final Connection connection;

    /**
     * The statement object of the database
     */
    private final Statement stmt;

    /**
     * Stores if the last operation on the database was successfull
     */
    private boolean isSuccessful;

    /**
     * If the manager was created successfully, without any problems
     */
    private boolean dbManagerCreatedSuccessfully;

    public DBManager() throws SQLException {
        //by default this value is false
        dbManagerCreatedSuccessfully = false;

        //no operation has been done so, it is by default true
        isSuccessful = true;

        //creates a connection object
        connection = DriverManager.getConnection("jdbc:sqlite:" + DB_FILE);
        //creates a "statement"
        stmt = connection.createStatement();

        //since we reached here without any problem (if there was a problem the function would have ended prematurely),
        //we can conclude that the DBManager object was created successfully so we set the below value to true
        dbManagerCreatedSuccessfully = true;
    }

    /**
     * @return true if the DBManager object was created without any problem. If its false, the object was created with a problem and cant be used
     */
    public boolean isDbManagerCreatedSuccessfully() {
        return dbManagerCreatedSuccessfully;
    }

    /**
     * Gets the balance of a user from the database, given his or her user id
     * 
     * If an exception occures or the user was not found isSuccessful is false
     * 
     * @param id - The user id in the form of a string
     * @return The bank balance of the user
     */
    public long getBalance(String id) throws SQLException {
        //the corresponding SQL query statement for getting bank balance (bank is the name of the table which stores bank data)
        String query = "SELECT * FROM bank WHERE id='" + id + "';";
        //gets the results of the query
        ResultSet resultSet = stmt.executeQuery(query);
        //balance of the user
        long balance = 0;
        //if the operation is successful ("not yet")
        isSuccessful = false;
        while (resultSet.next()) {
            //while there is something else
            //if the user id does not exist the code doesnt even enter here
            //and isSuccessful becomes false
            //else, the balance is set
            //getLong() gets the corresponding value from the column "balance"
            balance = resultSet.getLong("balance");
            isSuccessful = true;
        }
        resultSet.close();
        return balance;
    }

    /**
     * Gets the items in the user's inventory (as a string), given his or her user id
     * 
     * If an exception occures or the user was not found isSuccessful is false
     * 
     * @param id - The user's id
     * @return The items the user has (in the form of a String)
     */
    public String getItems(String id) throws SQLException {
        String query = "SELECT * FROM bank WHERE id='" + id + "';";
        ResultSet resultSet = stmt.executeQuery(query);
        String items = "";
        isSuccessful = false;
        while (resultSet.next()) {
            //get the items as a String from the column items
            items = resultSet.getString("items");
            isSuccessful = true;
        }
        resultSet.close();
        return items;
    }

    /**
     * Gets the time when daily was last taken (as a long - representing the milliseconds passed after the UNIX EPOCH), given the users id
     * 
     * If an exception occures or the user was not found isSuccessful is false
     * 
     * @param id - The user's id
     * @return The last time daily was taken (as a long)
     */
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

    /**
     * Sets the time when the daily is taken - i.e., if the user acquires his or her daily bonus, we set the time of the dailytaken to the current time, given the user id
     * 
     * @param id - The user id of the user whose dailytaken must be set
     * @param newDailyTaken - The new value of dailytaken
     */
    public void setDailyTaken(String id, long newDailyTaken) throws SQLException {
        //a simple SQL statement which sets dailytaken to newDailyTaken
        String sql = "UPDATE bank SET dailytaken=" + newDailyTaken + " WHERE id='" + id + "';";
        stmt.executeUpdate(sql);
    }

    /**
     * Adds money to a users account, given his or her user account id and amount to be added
     * 
     * @param id - The user id
     * @param amt - The amount to be deposited
     */
    public void deposit(String id, long amt) throws SQLException {
        String sql = "UPDATE bank SET balance=" + (amt + getBalance(id)) + " WHERE id='" + id + "';";
        stmt.executeUpdate(sql);
    }

    /**
     * Removes money from a users account, given his or her user account id and amount to be reduced
     * 
     * @param id - The user id
     * @param amt - The amount to be reduced
     */
    public void withdraw(String id, long amt) throws SQLException {
        String sql = "UPDATE bank SET balance=" + (getBalance(id) - amt) + " WHERE id='" + id + "';";
        stmt.executeUpdate(sql);
    }

    /**
     * Sets a users bank balance, given his or her user account id and new amount
     * 
     * @param id - The user id
     * @param newBalance - The new amount
     */
    public void setBalance(String id, long newBalance) throws SQLException {
        String sql = "UPDATE bank SET balance=" + newBalance + " WHERE id='" + id + "';";
        stmt.executeUpdate(sql);
    }

    /**
     * Sets a users new items, given his or her user account id and new items as a String
     * 
     * @param id - The user id
     * @param newItems - The new items (as a String)
     */
    public void setItems(String id, String newItems) throws SQLException {
        String sql = "UPDATE bank SET items='" + newItems + "' WHERE id='" + id + "';";
        stmt.executeUpdate(sql);
    }

    /**
     * Adds an entry to the bank table in the database, given the user id, with bank balance zero, dailytaken to be zero and items to be an empty String
     * 
     * @param id - The user id
     */
    public void addAccount(String id) throws SQLException {
        //simple SQL statement which adds an entry to the bank table
        String sql = "INSERT INTO bank (id, balance, items, dailytaken) VALUES ('" + id + "', 0, '', 0);";
        stmt.executeUpdate(sql);
    }

    /**
     * Creates a new table for the guild ranking system - the table stores the xp of each user in the guild
     * 
     * @param id - The guild (discord server) id
     */
    public void addGuildToXpSystem(String id) throws SQLException {
        //simple SQL statement which creates a table names xp_id with fields (columns) for the user id, xp, time of last message, with the user id being the primary key 
        String sql = "CREATE TABLE xp_" + id + " (" +
                "id text NOT NULL," +
                "xp bigint," +
                "lastmessage bigint," +
                "PRIMARY KEY (id)" +
                ");";
        try {
            stmt.executeUpdate(sql);
        } catch (SQLException ignored) {}
    }

    public void createBankTable() {
        String sql = "CREATE TABLE bank (" +
                "id text NOT NULL," +
                "balance bigint," +
                "items text," +
                "dailytaken bigint," +
                "PRIMARY KEY (id)" +
                ");";
        try {
            stmt.executeUpdate(sql);
        } catch (SQLException ignored) {}
    }

    /**
     * Removes a table from the guild ranking system, given the guild id
     * 
     * This is useful when the bot is kicked from a guild or the guild is deleted as getting rid of unnecessary data saves space
     * 
     * @param id - The guild id
     */
    public void removeGuildFromXpSystem(String id) throws SQLException {
        //simple SQL statement to drop (delete) table
        String sql = "DROP TABLE xp_" + id + ";";
        stmt.executeUpdate(sql);
    }

    /**
     * Adds an entry for a user to a particular guild ranking system table, given the guild id and user id
     * 
     * @param userId - The userid
     * @param guildId - The guildid
     */
    public void addUserToGuildXpSystem(String userId, String guildId) throws SQLException {
        //inserts a dummy entry to the table with xp and lastmessage set to 0
        String sql = "INSERT INTO xp_" + guildId + " (id, xp, lastmessage) VALUES " +
                "('" + userId + "', 0, 0);";
        try {
            stmt.executeUpdate(sql);
        } catch (SQLException ignored) {}
        //if entry already exists
    }

    /**
     * Removes a user from the guild xp system table, given the user id and guild id
     * 
     * Useful for getting rid of unnecessary user data if the user is kicked out of a server or leaves
     * 
     * @param userId - The user id to be deleted
     * @param guildId - The guild id from which the user id is to be deleted
     */
    public void removeUserFromGuildXpSystem(String userId, String guildId) throws SQLException {
        //deletes the entry of the user from the guild's table
        String sql = "DELETE FROM xp_" + guildId + " WHERE id='" + userId + "';";
        stmt.executeUpdate(sql);
    }

    /**
     * Gets the xp of a user, given the user id and guild id
     * 
     * @param userId - The user id whose xp is to be taken
     * @param guildId - The guild id in which the user is in
     * @return - The xp of the user
     */
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

    /**
     * Gets the rank of the user (by xp) in a particular guild, given the guild id and user id
     * 
     * @param userId - The user id
     * @param guildId - The guild id
     * @return The rank of the user
     */
    public long getRankOfUser(String userId, String guildId) throws SQLException {
        //SQL statement to query the entire table of the particular guild, ordered by xp in descending order
        String sql = "SELECT * FROM xp_" + guildId + " ORDER BY xp DESC;";
        ResultSet resultSet = stmt.executeQuery(sql);
        //list containing the id's of all the users sorted by rank
        LinkedList<String> xpList = new LinkedList<>();
        while (resultSet.next()) {
            //while there is more, add more
            xpList.add(resultSet.getString("id"));
        }
        //return the rank of the user
        return xpList.indexOf(userId);
    }

    /**
     * Get the time when the last message was sent, given the user id and guild id
     * 
     * @param userId - The userid
     * @param guildId - The guildid
     * @return The time when the user sent the last message (in the form of a long i.e., milliseconds after the UNIX EPOCH)
     */
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

    /**
     * Set xp of the user in the guild ranking system, given the user id and guild id
     * 
     * @param userId - The user id
     * @param guildId - The guild id
     * @param newXp - The new xp to be set
     */
    public void setXpOfUser(String userId, String guildId, long newXp) throws SQLException {
        String sql = "UPDATE xp_" + guildId + " SET xp=" + newXp + " WHERE id='" + userId + "';";
        stmt.executeUpdate(sql);
    }

    /**
     * Get the user ids of the top n users from the guild xp leaderboard
     * 
     * @param guildId - The guild id
     * @param n - The number of top users to be taken
     * @return - A list of the top n users
     */
    public List<String> getTopFromGuildXpLeaderboard(String guildId, long n) throws SQLException {
        String sql = "SELECT * FROM xp_" + guildId + " ORDER BY xp DESC;";
        ResultSet resultSet = stmt.executeQuery(sql);
        LinkedList<String> topList = new LinkedList<>();
        for (long i = 0; i < n && resultSet.next(); i++) {
            topList.add(resultSet.getString("id"));
        }
        return topList;
    }

    /**
     * Sets the time when the last message was sent by a particular user in a particular guild.
     * 
     * @param userId - The userId
     * @param guildId - The guildId
     * @param newLastMessageTime - The new time
     */
    public void setLastMessageTimeOfUser(String userId, String guildId, long newLastMessageTime) throws SQLException {
        String sql = "UPDATE xp_" + guildId + " SET lastmessage=" + newLastMessageTime + " WHERE id='" + userId + "';";
        stmt.executeUpdate(sql);
    }

    /**
     * Removes account from the bank, given the user id
     * 
     * @param id - The user id
     */
    public void removeAccount(String id) throws SQLException {
        String sql = "DELETE FROM bank WHERE id='" + id + "';";
        stmt.executeUpdate(sql);
    }

    /**
     * Closes this DBManager object
     * 
     * Should only be called when not needed anymore. It closes the statement and connection object as well.
     *
     */
    public void close() throws SQLException {
        stmt.close();
        connection.close();
    }

    /**
     * If the last operation was successful
     * 
     * @return value of isSuccessful variable
     */
    public boolean isOperationSuccessful() {
        return this.isSuccessful;
    }
}
