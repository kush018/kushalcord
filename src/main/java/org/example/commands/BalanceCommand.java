package org.example.commands;

import java.sql.SQLException;
import java.util.Set;

import org.example.Command;
import org.example.Main;

import discord4j.common.util.Snowflake;
import discord4j.core.event.domain.message.MessageCreateEvent;
import discord4j.core.object.entity.User;
import discord4j.rest.util.Color;

public class BalanceCommand implements Command {

    @Override
    public void execute(MessageCreateEvent event, String[] argv, String argvStr) {
        //gets a Set of users that were mentioned
        Set<Snowflake> userMentionSet = event.getMessage().getUserMentionIds();
        if (userMentionSet.size() == 0) {
            //if no one was mentioned, then we must just print the balance of the user who issued the command
            //in this case the object "user" represents the User who issued the command
            User user = event.getMessage().getAuthor().get();
            try {
                //getBalance() gets the balance of a particular user from the database, given a user id
                //user.getId().asString() gets the id of the user as a String
                long bal = Main.psqlManager.getBalance(user.getId().asString());
                if (!Main.psqlManager.isOperationSuccessful()) {
                    //if the user who issued the command does not already exist in the database the query fails, we create an entry for the user
                    Main.psqlManager.addAccount(user.getId().asString());
                }
                event.getMessage().getChannel().block().createEmbed((embed) -> {
                    embed.setColor(Color.GRAY_CHATEAU);
                    embed.setDescription("Your bank balance: " + bal);
                    embed.setTitle("Bank balance");
                }).block();
            } catch (SQLException throwables) {
                //if an SQLException occured during the operation
                throwables.printStackTrace();
            }
        } else {
            //if someone was mentioned
            for (Snowflake userId : userMentionSet) {
                //we take the user-id for each user mentioned as a Snowflake object and print the bank balance for each user
                try {
                    long bal = Main.psqlManager.getBalance(userId.asString());
                    if (!Main.psqlManager.isOperationSuccessful()) {
                        Main.psqlManager.addAccount(userId.asString());
                    }
                    User currentUser = Main.client.getUserById(userId).block();
                    event.getMessage().getChannel().block().createEmbed((embed) -> {
                        embed.setColor(Color.GRAY_CHATEAU);
                        embed.setDescription(currentUser.getUsername() + "'s bank balance: " + bal);
                        embed.setTitle("Bank balance");
                    }).block();
                } catch (SQLException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    @Override
    public String getHelpString() {
        return "bal - tells your bank balance\n" +
                "bal <mention1> <mention2> ... - tells the bank balance of all the users mentioned";
    }

}
