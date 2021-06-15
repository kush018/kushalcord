package org.example.commands;

import discord4j.common.util.Snowflake;
import discord4j.core.event.domain.message.MessageCreateEvent;
import discord4j.rest.util.Color;
import org.example.Command;
import org.example.Main;
import org.example.Utils;

import java.sql.SQLException;
import java.util.Set;

public class RankCommand implements Command {

    @Override
    public void execute(MessageCreateEvent event, String[] argv, String argvStr) {
        Set<Snowflake> userMentionSet = event.getMessage().getUserMentionIds();
        if (userMentionSet.size() == 0) {
            //if no one was mentioned, then we print the rank of the user issuing the command
            //guildId stores the id for the server as a string. The ranking system creates a table in the database for each server
            //the bot is in, identified by the server id (server is also known as guild in discord)
            String guildId = event.getMessage().getGuildId().get().asString();
            String userId = event.getMessage().getAuthor().get().getId().asString();
            try {
                //get the xp of the user from the database using the userId and guildId
                long xp = Main.dbManager.getXpOfUser(userId, guildId);
                //calculates the level which the user is it using the calculateLevel() function
                long level = Utils.calculateLevel(xp);
                //finds out the xp the user needs to reach to get to the next level
                long nextLvlXp = Utils.calculateXpForLvl(level + 1);
                //getRankOfUser() returns the rank of the user
                event.getMessage().getChannel().block().createEmbed((embed) -> {
                    embed.setColor(Color.MAGENTA);
                    try {
                        embed.setDescription("Ur xp: " + xp + "/" + nextLvlXp +
                                "\n" + "Ur lvl: " + level +
                                "\n" + "Ur rank: " + (Main.dbManager.getRankOfUser(userId, guildId) + 1));
                    } catch (SQLException throwables) {
                        throwables.printStackTrace();
                    }
                    embed.setTitle("Xp Rank");
                }).block();
            } catch (SQLException e) {
                e.printStackTrace();
            }
        } else {
            //if someone was mentioned, we need to show the rank of all the people mentioned
            for (Snowflake userIdSnowflake : userMentionSet) {
                String guildId = event.getMessage().getGuildId().get().asString();
                String userId = userIdSnowflake.asString();
                try {
                    long xp = Main.dbManager.getXpOfUser(userId, guildId);
                    long level = Utils.calculateLevel(xp);
                    long nextLvlXp = Utils.calculateXpForLvl(level + 1);
                    event.getMessage().getChannel().block().createEmbed((embed) -> {
                        embed.setColor(Color.MAGENTA);
                        try {
                            embed.setDescription("Rank of: " + Main.client.getUserById(userIdSnowflake).block().getUsername() +
                                    "\n" + "xp: " + xp + "/" + nextLvlXp +
                                    "\n" + "lvl: " + level +
                                    "\n" + "rank: " + (Main.dbManager.getRankOfUser(userId, guildId) + 1));
                        } catch (SQLException throwables) {
                            throwables.printStackTrace();
                        }
                        embed.setTitle("Xp Rank");
                    }).block();
                } catch (SQLException e) {
                    e.printStackTrace();
                    return;
                }
            }
        }
    }

    @Override
    public String getHelpString() {
        return "rank - tells ur xp, level and rank\n" +
                "rank <mention1> <mention2> ... - tells the rank of all users mentioned";
    }
}
