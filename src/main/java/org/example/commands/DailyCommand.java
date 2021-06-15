package org.example.commands;

import discord4j.core.event.domain.message.MessageCreateEvent;
import discord4j.core.object.entity.User;
import discord4j.rest.util.Color;
import org.example.Command;
import org.example.Main;

import java.sql.SQLException;

public class DailyCommand implements Command {

    /**
     * One second in milliseconds (used for representing time)
     */
    public static final long ONE_SECOND = 1000;
    /**
     * One minute in milliseconds (used for representing time)
     */
    public static final long ONE_MINUTE = 60 * ONE_SECOND;
    /**
     * One hour in milliseconds (used for representing time)
     */
    public static final long ONE_HOUR = 60 * ONE_MINUTE;
    /**
     * One day in milliseconds (used for representing time)
     */
    public static final long ONE_DAY = 24 * ONE_HOUR;

    /**
     * This is the money the user receives when he or she uses the command daily. (This is the daily bonus - in kc coins)
     */
    public static final long DAILY_ALLOWANCE = 10000;

    @Override
    public void execute(MessageCreateEvent event, String[] argv, String argvStr) {
        //daily command is for collecting user kc coins bonus
        //the User who issued this command is stored in the below user object
        User user = event.getMessage().getAuthor().get();
        try {
            //every time a person collects the daily bonus, the time of collecting is recorded, so that we can find out if the person has already collected his or her
            //daily bonus in the last 24 hours
            //dailyTaken stores the last time when the daily was taken in the form of milliseconds after the unix epoch
            long dailyTaken = Main.dbManager.getDailyTaken(user.getId().asString());
            if (!Main.dbManager.isOperationSuccessful()) {
                //if the user does not exist already in our database
                //then we must create the user
                //in this case dailyTaken will be 0
                Main.dbManager.addAccount(user.getId().asString());
            }
            //System.currentTimeMillis() gets the current system time in milliseconds after unix epoch
            if (dailyTaken + ONE_DAY <= System.currentTimeMillis()) {
                //if one day is over, then we must give the bonus to the user
                Main.dbManager.deposit(user.getId().asString(), DAILY_ALLOWANCE);
                //this gives the user confirmation
                event.getMessage().getChannel().block().createEmbed((embed) -> {
                    embed.setColor(Color.DEEP_LILAC);
                    embed.setDescription("Added " + DAILY_ALLOWANCE + " kc coins to your account");
                    embed.setTitle("Get Daily Allowance");
                }).block();
                //since the daily was taken, we set the dailytaken entry of the user to the current time
                Main.dbManager.setDailyTaken(user.getId().asString(), System.currentTimeMillis());
            } else {
                //if the user has already taken his or her daily allowance in the last 24 hours
                event.getMessage().getChannel().block().createEmbed((embed) -> {
                    embed.setColor(Color.DEEP_LILAC);
                    embed.setDescription("You've already received your daily kc coins allowance. Wait another 24 hours for more coins!");
                    embed.setTitle("Get Daily Allowance");
                }).block();
            }
        } catch (SQLException throwables) {
            throwables.printStackTrace();
        }
    }

    @Override
    public String getHelpString() {
        return "daily - collect your daily allowance";
    }
}
