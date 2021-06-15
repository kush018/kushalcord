package org.example.commands;

import discord4j.common.util.Snowflake;
import discord4j.core.event.domain.message.MessageCreateEvent;
import discord4j.rest.util.Color;
import org.example.Command;
import org.example.Main;
import org.example.Utils;

import java.sql.SQLException;
import java.util.List;

public class TopRankersCommand implements Command {

    /**
     * When a user uses the "toprankers" command, he or she can specify the number of ranks to be displayed (from the message ranking system). This defines the maximum limit of ranks that can be displayed.
     */
    public static final int MAX_RANKS_TO_BE_DISPLAYED = 15;

    @Override
    public void execute(MessageCreateEvent event, String[] argv, String argvStr) {
        //n - number of ranks to be displayed
        int n = 0;
        try {
            //n = first argument
            n = Integer.parseInt(argv[0]);
        } catch (NumberFormatException e) {
            //invalid number
            event.getMessage().getChannel().block().createEmbed((embed) -> {
                embed.setColor(Color.MOON_YELLOW);
                embed.setDescription("Invalid number, bruh");
                embed.setTitle("Xp Top Rankers");
            }).block();
            return;
        } catch (IndexOutOfBoundsException e) {
            //if no argument specified, just set n to 10
            n = 10;
        }
        if (n > MAX_RANKS_TO_BE_DISPLAYED) {
            event.getMessage().getChannel().block().createEmbed((embed) -> {
                embed.setColor(Color.MOON_YELLOW);
                embed.setDescription("I cant print that many ranks");
                embed.setTitle("Xp Top Rankers");
            }).block();
            return;
        }
        String guildId = event.getMessage().getGuildId().get().asString();
        List<String> topRanksIds = null;
        //topRanksIds stores the ids of the users holding the top n ranks from the server leaderboard
        try {
            topRanksIds = Main.dbManager.getTopFromGuildXpLeaderboard(guildId, n);
        } catch (SQLException e) {
            e.printStackTrace();
            return;
        }
        //StringBuilder is used to create long Strings by appending one String after the other
        //using StringBuilder is more efficient than String concatenation
        StringBuilder builder = new StringBuilder();
        for (int i = 0; i < topRanksIds.size(); i++) {
            //for each top ranker
            String id = topRanksIds.get(i);
            try {
                //append() appends a String to the StringBuilder
                //client.getUserById() gets a User object from an id stored as a Snowflake object
                //Snowflake.of() converts the user id as a String to the user id as a Snowflake
                //getTag() helps get the full discord tag of the user
                //getXpOfUser() gets the xp of a user
                //event.getMessage().getGuildId().get().asString() gets the guildId of the message as a String
                builder.append(i + 1).append(") ").append(Main.client.getUserById(Snowflake.of(id)).block().getTag()).append(" - lvl ").append(Utils.calculateLevel(Main.dbManager.getXpOfUser(id, event.getMessage().getGuildId().get().asString()))).append("\n");
            } catch (SQLException ignored) {}
        }
        int finalN = n;
        //builder.toString() returns the String form of the StringBuilder
        event.getMessage().getChannel().block().createEmbed((embed) -> {
            embed.setColor(Color.MOON_YELLOW);
            embed.setDescription("Top " + finalN + " ranks:\n" + builder.toString());
            embed.setTitle("Xp Top Rankers");
        }).block();
    }

    @Override
    public String getHelpString() {
        return "toprankers <n> - prints the leaderboard of the top n ranks in the guild xp system\n" +
                "toprankers - same as the above commands where n = 10";
    }
}
