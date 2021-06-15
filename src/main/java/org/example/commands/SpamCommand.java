package org.example.commands;

import discord4j.core.event.domain.message.MessageCreateEvent;
import discord4j.core.object.entity.User;
import discord4j.core.object.entity.channel.MessageChannel;
import discord4j.rest.util.Color;
import org.example.Command;
import org.example.Main;

import java.util.concurrent.atomic.AtomicLong;

public class SpamCommand implements Command {

    /**
     * When a user uses the "spam" command a cooldown is required before he or she can use the command again. The spam cooldown is decided by picking a random number between a lower and upper limit. This is the lower limit. (in seconds)
     */
    public static final long SPAM_TIME_OUT_MIN = 150;
    /**
     * When a user uses the "spam" command a cooldown is required before he or she can use the command again. The spam cooldown is decided by picking a random number between a lower and upper limit. This is the upper limit. (in seconds)
     */
    public static final long SPAM_TIME_OUT_MAX = 300;

    public static final int MAX_SPAM_LIMIT = 10;

    @Override
    public void execute(MessageCreateEvent event, String[] argv, String argvStr) {
        MessageChannel channel = event.getMessage().getChannel().block();
        //the User who used the spam command is stored in the form of a User object in "spammer"
        //this is helpful in identifying the spammer, helping in knowing if the spam cooldown was completed.
        User spammer = event.getMessage().getAuthor().get();
        try {
            //gets the amount of seconds left as an AtomicLong for the cooldown to get over
            if (Main.userSpamCooldownTimerMap.get(spammer) == null || Main.userSpamCooldownTimerMap.get(spammer).get() == 0) {
                //if its zero, that means the cooldown is over
                //if its null, that means that the user has not even spammed yet
                //either way, we let the user spam
                //times stores the number of times the user wants the bot to spam (it is the first argument)
                int times = Integer.parseInt(argv[0]);
                if (times > MAX_SPAM_LIMIT) {
                    //if the user wants the bot to spam too many times, above the max spam limit, we shouldn't allow him or her to do so and the comamnd is simply ignored
                    event.getMessage().getChannel().block().createEmbed((embed) -> {
                        embed.setColor(Color.PINK);
                        embed.setDescription("Woah! Hold on there! You don't wanna spam **too** much now do you?");
                        embed.setTitle("Spammer");
                    }).block();
                    return;
                }
                //we will reach here only if the number of times the user wants to spam is lesser than 20 because if its more, the program simply returns and ends the function
                //the String toSpam stores the message that needs to be spammed
                //argvStr stores all the arguments as one String, out of which we need to separate the first argument, telling us the number of times the message needs to be spammed
                //substring() creates a string out of which is first argument is removed
                //so if the command was spam 10 message goes here
                //argv[0].length = 2 [as argv[0] = "10"]
                //argvStr = "10 message goes here" ["10" needs to be removed]
                //argvStr.substring(argv[0].length [which evaluates to 2]) = "message goes here"
                String toSpam = argvStr.substring(argv[0].length());
                if (argv.length == 1) {
                    //if argv.length == 1, it means that only one argument has been given i.e., the user has not specified what he or she wants to spam
                    //thus, the function simply ends and wont go further
                    event.getMessage().getChannel().block().createEmbed((embed) -> {
                        embed.setColor(Color.PINK);
                        embed.setDescription("You didn't tell me what to spam");
                        embed.setTitle("Spammer");
                    }).block();
                    return;
                }
                //this is the for loop which actually spams the required message
                for (int i = 0; i < times; i++) {
                    channel.createMessage(toSpam).block();
                }
                //the userSpamCooldownTimerMap maps a User with the time left before cooldown is complete in seconds
                //so here, we are adding a Key-Value pair - where the Key is the "spammer" - the spamming user
                //and the Value is a new AtomicLong object which stores a random value between SPAM_TIME_OUT_MAX and SPAM_TIME_OUT_MIN
                Main.userSpamCooldownTimerMap.put(spammer, new AtomicLong((long)(Math.random() * (SPAM_TIME_OUT_MAX - SPAM_TIME_OUT_MIN) + SPAM_TIME_OUT_MIN)));
            } else {
                //if the spam cooldown has not reached zero yet
                event.getMessage().getChannel().block().createEmbed((embed) -> {
                    embed.setColor(Color.PINK);
                    embed.setDescription("Stop spamming so much. You'll annoy everyone!");
                    embed.setTitle("Spammer");
                }).block();
            }
        } catch (NumberFormatException e) {
            //if the command was issued without a valid number i.e., if the command was used like: spam abc def
            //abc is not a valid number and will throw an NumberFormatException when it is attempted to convert to a number
            //if no valid number was entered, it will spam the message n times where n = times
            int times = 5;
            if (Main.userSpamCooldownTimerMap.get(spammer) == null || Main.userSpamCooldownTimerMap.get(spammer).get() == 0) {
                //to check if the users cooldown was completed
                //here we dont need to create a substring of argvStr because no valid argument was given telling us the number of times to spam
                String toSpam = argvStr;
                if (argv.length == 0) {
                    event.getMessage().getChannel().block().createEmbed((embed) -> {
                        embed.setColor(Color.PINK);
                        embed.setDescription("You didn't tell me what to spam");
                        embed.setTitle("Spammer");
                    }).block();
                    return;
                }
                //spams the message times times
                for (int i = 0; i < times; i++) {
                    channel.createMessage(toSpam).block();
                }
                //adds cooldown timer for user
                Main.userSpamCooldownTimerMap.put(spammer, new AtomicLong((long)(Math.random() * (SPAM_TIME_OUT_MAX - SPAM_TIME_OUT_MIN) + SPAM_TIME_OUT_MIN)));
            } else {
                //if cooldown was not completed
                event.getMessage().getChannel().block().createEmbed((embed) -> {
                    embed.setColor(Color.PINK);
                    embed.setDescription("Stop spamming so much. You'll annoy everyone!");
                    embed.setTitle("Spammer");
                }).block();
            }
        } catch (ArrayIndexOutOfBoundsException e) {
            //if no arguments were given at all, then
            //when the program tried to acces argv[0] it will throw ArrayIndexOutOfBoundsException
            //in which case, we must inform the user
            event.getMessage().getChannel().block().createEmbed((embed) -> {
                embed.setColor(Color.PINK);
                embed.setDescription("Invalid arguments");
                embed.setTitle("Spammer");
            }).block();
        }
    }

    @Override
    public String getHelpString() {
        return "spam <message> - spams a message 5 times\n" +
                "spam <n> <message> - spams a message n times\n" +
                "(use at your own risk lol)";
    }
}
