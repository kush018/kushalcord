package org.example.commands;

import discord4j.common.util.Snowflake;
import discord4j.core.event.domain.message.MessageCreateEvent;
import discord4j.core.object.entity.User;
import discord4j.rest.util.Color;
import org.example.Command;
import org.example.Main;

import java.sql.SQLException;
import java.util.Set;

public class TransferCommand implements Command {

    @Override
    public void execute(MessageCreateEvent event, String[] argv, String argvStr) {
        long toTransfer = 0;
        try {
            //the first argument stores the amount that needs to be transferred to another user
            toTransfer = Long.parseLong(argv[0]);
            if (toTransfer < 1) {
                //invalid transfer amount
                event.getMessage().getChannel().block().createEmbed((embed) -> {
                    embed.setColor(Color.DARK_GOLDENROD);
                    embed.setDescription("Bruh, what are you trying to do?");
                    embed.setTitle("Bank Transfer Request");
                }).block();
                return;
            }
        } catch (NumberFormatException e) {
            //if the transfer amount entered is not a valid number
            event.getMessage().getChannel().block().createEmbed((embed) -> {
                embed.setColor(Color.DARK_GOLDENROD);
                embed.setDescription("Yo, I can't transfer " + argv[0] + " coins. What are you trying to do?");
                embed.setTitle("Bank Transfer Request");
            }).block();
            return;
        } catch (ArrayIndexOutOfBoundsException e) {
            //if arguments were not entered, then argv[0] would throw ArrayIndexOutOfBoundsException
            event.getMessage().getChannel().block().createEmbed((embed) -> {
                embed.setColor(Color.DARK_GOLDENROD);
                embed.setDescription("Invalid usage syntax");
                embed.setTitle("Bank Transfer Request");
            }).block();
            return;
        }
        User user = event.getMessage().getAuthor().get();
        try {
            //if the user is trying to transfer more than he or she owns
            if (toTransfer > Main.dbManager.getBalance(user.getId().asString())) {
                event.getMessage().getChannel().block().createEmbed((embed) -> {
                    embed.setColor(Color.DARK_GOLDENROD);
                    embed.setDescription("You cant transfer more money than you own!");
                    embed.setTitle("Bank Transfer Request");
                }).block();
                return;
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        //gets set of user mentions
        Set<Snowflake> mentionIdSet = event.getMessage().getUserMentionIds();
        if (mentionIdSet.size() == 0) {
            //if no one was mentioned, we dont know who to transfer to
            event.getMessage().getChannel().block().createEmbed((embed) -> {
                embed.setColor(Color.DARK_GOLDENROD);
                embed.setDescription("You haven't told me who to send the money to!");
                embed.setTitle("Bank Transfer Request");
            }).block();
            return;
        }
        //this takes the user who we need to transfer to
        //if multiple users were mentioned, we only consider the first user
        Snowflake user2Id = (Snowflake)mentionIdSet.toArray()[0];
        try {
            Main.dbManager.getBalance(user2Id.asString());
            if (!Main.dbManager.isOperationSuccessful()) {
                //if the bank account of the user to whom we need to transfer to does not exist, we must create it
                Main.dbManager.addAccount(user2Id.asString());
            }
        } catch (SQLException e) {
            e.printStackTrace();
            return;
        }
        try {
            //deducts the transfer amount from the transferring user
            Main.dbManager.withdraw(user.getId().asString(), toTransfer);
            //adds the transfer amount to the user to whom the transfer needs to be done
            Main.dbManager.deposit(user2Id.asString(), toTransfer);
        } catch (SQLException e) {
            e.printStackTrace();
        }
        long finalToTransfer = toTransfer;
        //confirmation message
        event.getMessage().getChannel().block().createEmbed((embed) -> {
            embed.setColor(Color.DARK_GOLDENROD);
            embed.setDescription("Sucessfully transferred " + finalToTransfer);
            embed.setTitle("Bank Transfer Request");
        }).block();
    }

    @Override
    public String getHelpString() {
        return "transfer <amt> <mention> - transfers amt coins to user mentioned";
    }
}
