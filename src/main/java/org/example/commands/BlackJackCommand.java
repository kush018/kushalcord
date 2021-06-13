package org.example.commands;

import discord4j.core.event.domain.message.MessageCreateEvent;
import discord4j.core.object.entity.User;
import discord4j.core.object.entity.channel.MessageChannel;
import discord4j.rest.util.Color;
import org.example.BJGame;
import org.example.Command;
import org.example.Main;

import java.sql.SQLException;

public class BlackJackCommand implements Command {

    @Override
    public void execute(MessageCreateEvent event, String[] argv, String argvStr) {
        //gets the channel in which the command was issued
        MessageChannel messageChannel = event.getMessage().getChannel().block();
        User user = event.getMessage().getAuthor().get();
        if (argv.length == 0) {
            //if no arguments were entered
            event.getMessage().getChannel().block().createEmbed((embed) -> {
                embed.setColor(Color.DEEP_SEA);
                embed.setDescription("U havent told me how much money u wanna gamble");
                embed.setTitle("The BlackJack Casino");
            }).block();
            return;
        }
        long toGamble;
        try {
            //gets the amount of money the user wants to gamble.
            //this amount is the first argument
            toGamble = Long.parseLong(argv[0]);
        } catch (NumberFormatException e) {
            //invalid number
            event.getMessage().getChannel().block().createEmbed((embed) -> {
                embed.setColor(Color.DEEP_SEA);
                embed.setDescription("Not a valid number!");
                embed.setTitle("The BlackJack Casino");
            }).block();
            return;
        }
        if (toGamble < 1) {
            //invalid number to gamble
            event.getMessage().getChannel().block().createEmbed((embed) -> {
                embed.setColor(Color.DEEP_SEA);
                embed.setDescription("What are you trying to do? lol");
                embed.setTitle("The BlackJack Casino");
            }).block();
            return;
        }
        long balance;
        try {
            //gets the current balance of the user
            balance = Main.dbManager.getBalance(user.getId().asString());
        } catch (SQLException e) {
            e.printStackTrace();
            return;
        }
        if (toGamble > balance) {
            //if the user tries to gamble more than he or she owns
            event.getMessage().getChannel().block().createEmbed((embed) -> {
                embed.setColor(Color.DEEP_SEA);
                embed.setDescription("You cant gamble more than you own!");
                embed.setTitle("The BlackJack Casino");
            }).block();
            return;
        }
        //creates a new BJGame object, representing the blackjack game
        BJGame bjGame = new BJGame(event, toGamble);
        //adds the object to the currently running BJGames list
        Main.runningBJGamesList.add(bjGame);
        //tells the user his or her cards and the valid commands
        event.getMessage().getChannel().block().createEmbed((embed) -> {
            embed.setColor(Color.DEEP_SEA);
            embed.setDescription("Ur cards: " + bjGame.getUserCards() +
                    "\nh=hit, s=stand, e=end");
            embed.setTitle("The BlackJack Casino");
        }).block();
    }

    @Override
    public String getHelpString() {
        return "bj <amount> - gamble your money away in a game of blackjack";
    }
}
