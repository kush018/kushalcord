package org.example;

import discord4j.core.event.domain.message.MessageCreateEvent;
import discord4j.core.object.entity.User;
import discord4j.core.object.entity.channel.MessageChannel;
import discord4j.rest.util.Color;

import java.sql.SQLException;
import java.util.ArrayList;

import static org.example.Main.*;

public class Utils {
    /**
     * Applies message to the job system
     *
     * @param event - The corresponding MessageCreateEvent
     *
     * @return true if the message was for the job system and the message need not be processed further
     */
    public static boolean applyMessageToJobsSystem(MessageCreateEvent event) {
        for (Job job : currentWorkingJobsList) {
            //for each job in the current running jobs list
            if (job.checkChannel(event) && job.checkUser(event)) {
                //if the channel and user in which the event occured are the same as that of the job
                if (job.checkJob(event)) {
                    //if the job was executed correctly
                    event.getMessage().getChannel().block().createEmbed((embed) -> {
                        embed.setColor(Color.DISCORD_WHITE);
                        embed.setDescription("Great job "
                                + event.getMessage().getAuthor().get().getUsername()
                                + "! You've earned yourself " + job.getWage());
                        embed.setTitle("Work Application");
                    }).block();
                    try {
                        //deposits the wage of the job to the users account
                        psqlManager.deposit(event.getMessage().getAuthor().get().getId().asString(), job.getWage());
                    } catch (SQLException ignored) {
                    }
                } else {
                    //if the job was not done correctly
                    event.getMessage().getChannel().block().createEmbed((embed) -> {
                        embed.setColor(Color.DISCORD_WHITE);
                        embed.setDescription("Terrible job " +
                                event.getMessage().getAuthor().get().getUsername() + ". This is not expected from you.\n" +
                                "I will not be paying you anything for that.");
                        embed.setTitle("Work Application");
                    }).block();
                }
                //since the job is done, we can remove it from the currentWorkingJobsList
                currentWorkingJobsList.remove(job);
                //there is no need to check for any more jobs
                //since this message was applied to the job system, the message does not need to be processed any further
                return true;
            }
        }
        //the message was not applied to the job system and needs to be processed further
        return false;
    }

    /**
     * Applies message to the blackjack game system
     *
     * @param event - The corresponding MessageCreateEvent
     *
     * @return true if the message was for the bj system and the message need not be processed further
     */
    public static boolean applyMessageToBJSystem(MessageCreateEvent event) {
        for (BJGame bjGame : runningBJGamesList) {
            //for each game in BJGamesList - the list of currently "running" games
            User user = event.getMessage().getAuthor().get();
            MessageChannel messageChannel = event.getMessage().getChannel().block();
            if (bjGame.checkChannel(event) && bjGame.checkUser(event)) {
                //if the user and channel that triggered the MessageCreateEvent are the same as those in the BJGame class
                String message = event.getMessage().getContent();
                //result of the current turn
                int result = 0;
                if (message.equals("h")) {
                    //if the user sent a command for a "hit" in the game
                    bjGame.hit();
                    result = bjGame.getGameStatus();
                } else if (message.equals("s")) {
                    //if the user sent a command for a "stand" in the game
                    bjGame.stand();
                    result = bjGame.getGameStatus();
                } else if (message.equals("e")) {
                    //if the user sent a command to end the game
                    event.getMessage().getChannel().block().createEmbed((embed) -> {
                        embed.setColor(Color.DEEP_SEA);
                        embed.setDescription("Blackjack game ended. You wont win or lose anything.\n" +
                                "But heres your cards: " + bjGame.getUserCards() +
                                "\nAnd here is the dealers cards: " + bjGame.getDealerCards());
                        embed.setTitle("The BlackJack Casino");
                    }).block();
                    //removes the current bj game from the currently running bjgames list
                    runningBJGamesList.remove(bjGame);
                    return true;
                } else {
                    //invalid command
                    event.getMessage().getChannel().block().createEmbed((embed) -> {
                        embed.setColor(Color.DEEP_SEA);
                        embed.setDescription("Invalid command\nUr cards: " + bjGame.getUserCards() +
                                "\nh=hit, s=stand, e=end");
                        embed.setTitle("The BlackJack Casino");
                    }).block();
                    //since the user has ended the game, we dont need to worry about processing the value in result
                    return true;
                }

                if (result == BJGame.STATUS_LOSE) {
                    //if the user lost the game
                    event.getMessage().getChannel().block().createEmbed((embed) -> {
                        embed.setColor(Color.DEEP_SEA);
                        embed.setDescription("Ur cards: " + bjGame.getUserCards() +
                                "\nDealers cards: " + bjGame.getDealerCards() +
                                "\nYou lose! Say bye to " + bjGame.getToGamble());
                        embed.setTitle("The BlackJack Casino");
                    }).block();
                    try {
                        //if the user lost, he or she loses all the money he or she betted
                        psqlManager.withdraw(user.getId().asString(), bjGame.getToGamble());
                    } catch (SQLException e) {
                        e.printStackTrace();
                    }
                    runningBJGamesList.remove(bjGame);
                } else if (result == BJGame.STATUS_WIN) {
                    //if the user won
                    event.getMessage().getChannel().block().createEmbed((embed) -> {
                        embed.setColor(Color.DEEP_SEA);
                        embed.setDescription("Ur cards: " + bjGame.getUserCards() +
                                "\nDealers cards: " + bjGame.getDealerCards() +
                                "\nYou won! You win " + bjGame.getToGamble());
                        embed.setTitle("The BlackJack Casino");
                    }).block();
                    try {
                        //we must give the user the money he or she betted
                        psqlManager.deposit(user.getId().asString(), bjGame.getToGamble());
                    } catch (SQLException e) {
                        e.printStackTrace();
                    }
                    runningBJGamesList.remove(bjGame);
                } else if (result == BJGame.STATUS_TIE) {
                    //if it was a tie
                    event.getMessage().getChannel().block().createEmbed((embed) -> {
                        embed.setColor(Color.DEEP_SEA);
                        embed.setDescription("Ur cards: " + bjGame.getUserCards() +
                                "\nDealers cards: " + bjGame.getDealerCards() +
                                "\nIts a tie! You dont lose or win any money");
                        embed.setTitle("The BlackJack Casino");
                    }).block();
                    //no money is added or removed
                    runningBJGamesList.remove(bjGame);
                } else {
                    //if everything is as normal i.e., nothing special happened
                    event.getMessage().getChannel().block().createEmbed((embed) -> {
                        embed.setColor(Color.DEEP_SEA);
                        embed.setDescription("Ur cards: " + bjGame.getUserCards() +
                                "\nh=hit, s=stand, e=end");
                        embed.setTitle("The BlackJack Casino");
                    }).block();
                }
                //we have found the corresponding BJGame and processed the message, so no need to iterate through the rest of BJGame objects
                return true;
            }
        }
        //if the message was not for the blackjack system
        return false;
    }

    /**
     * Applies message to the ranking system
     *
     * @param event - The corresponding MessageCreateEvent
     */
    public static void applyMessageToRankingSystem(MessageCreateEvent event) {
        //when this message was sent
        long timeOfMessage = System.currentTimeMillis();
        //if the author does not exist, then end this function
        if (!event.getMessage().getAuthor().isPresent()) {return;}
        //author of the message
        User messageAuthor = event.getMessage().getAuthor().get();
        //id of the author
        String userId = messageAuthor.getId().asString();
        //id of the server (guild) in which this message was sent
        String guildId = event.getMessage().getGuildId().get().asString();
        try {
            //adds guild to the xp system (if present it will do nothing)
            psqlManager.addGuildToXpSystem(guildId);
            //adds user to the guild xp system (if present it will do nothing)
            psqlManager.addUserToGuildXpSystem(userId, guildId);
            //checks the last time the user sent a message in the guild
            long lastMessageSentTime = psqlManager.getLastMessageTime(userId, guildId);
            if (lastMessageSentTime + ONE_MINUTE > timeOfMessage) {
                //if one minute has not yet passed after the last message was sent, we wont apply the message to the ranking system
                return;
            }
            //if we are applying the message to the ranking system, we must set the last message time of the user in the guild to the time
            //of the current message
            psqlManager.setLastMessageTimeOfUser(userId, guildId, timeOfMessage);
            //xpToAdd stores the amount of xp to be added to the user (random value between 15 and 25)
            long xpToAdd = (long) (Math.random() * (26 - 15) + 15);
            //newXp stores the new xp which the user has right now (oldxp + xptoadd)
            long newXp = psqlManager.getXpOfUser(userId, guildId) + xpToAdd;
            //sets the xp of the user to the new xp
            psqlManager.setXpOfUser(userId, guildId, newXp);
            //calculateLevel calculates the level which the user is at, given his or her xp
            if (calculateLevel(newXp) != calculateLevel(newXp - xpToAdd)) {
                //if the level after the xp addition is different from the level before xp addition, it means that the level has increased
                //because the xp cant reduce, so the level cant reduce, so it definitely increased
                //since the played his levelled up, we must celebrate this occasion by pinging him or her and telling what his or her new
                //level is
                event.getMessage().getChannel().block().createMessage("Congratulations <@" + userId + ">! Your new level is: " + calculateLevel(newXp)).block();
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    /**
     * Divides a String into an array arguments
     *
     * Examples:
     * this is a string - {"this", "is", "a", "string"}
     * "argument one" argument two - {"argument one", "argument", "two"}
     *
     * @param s - The String that must be divided into an array of arguments
     *
     * @return The array of arguments which the String has been divided into
     */
    public static String[] divideIntoArgs(String s) {
        //an arraylist of all the arguments
        ArrayList<String> contentArr = new ArrayList<>();
        //if quotation marks have been found
        boolean foundQuotationMarks = false;
        //if a new argument has "started" while traversing through the array
        boolean newWord = true;
        //for each character in the String s, with the index of the character being i,
        for (int i = 0; i < s.length(); i++) {
            if (s.charAt(i) == ' ') {
                //if the current character is a space
                if (!foundQuotationMarks) {
                    //if quotation marks were not found, we must start a new argument
                    newWord = true;
                } else {
                    //if quotation marks were found, then we must consider the space character to be part of the argument
                    if (newWord) {
                        //if a new argument was just started, we must add the current character as a String to the contentArr
                        contentArr.add(Character.toString(s.charAt(i)));
                        newWord = false;
                    } else {
                        //if a new argument as not just been started and we are in the middle of an argument
                        //we must add the current character to the last element in contentArr
                        contentArr.set(contentArr.size() - 1, contentArr.get(contentArr.size() - 1) + s.charAt(i));
                    }
                }
            } else if (s.charAt(i) == '"') {
                //if the current character is a double quote
                if (!foundQuotationMarks) {
                    //if quotation marks were not already found, it means that a new argument is starting
                    foundQuotationMarks = true;
                } else {
                    //if quotation marks were already found the double quotes mark the end of the argument
                    foundQuotationMarks = false;
                    //the quotation marks also start a new argument
                    newWord = true;
                }
            } else {
                //if its a normal character
                if (newWord) {
                    //if a new argument just started
                    contentArr.add(Character.toString(s.charAt(i)));
                    newWord = false;
                } else {
                    contentArr.set(contentArr.size() - 1, contentArr.get(contentArr.size() - 1) + s.charAt(i));
                }
            }
        }
        //we need to convert the ArrayList contentArr to a plain one dimensional String array
        //we create an empty String array with the same length as contentArr
        String[] argvArr = new String[contentArr.size()];
        //for each element in contentArr
        for (int i = 0; i < contentArr.size(); i++) {
            //we sent the element in the plain String array to the contentArr
            argvArr[i] = contentArr.get(i);
        }
        //finally, we can return argvArr
        return argvArr;
    }

    /**
     * Calculates the level of a user, given his or her total xp (This is for the ranking system)
     *
     * @param xp - The total xp of the user
     * @return The level of the user, given his or her total xp
     */
    public static long calculateLevel(long xp) {
        return (long) Math.floor(
                (Math.sqrt(625 + 100 * xp) - 25)
                        /50
        );
    }

    /**
     * Calculates the total xp required to reach a particular level (This is for the ranking system)
     *
     * @param lvl - The level to be reached
     * @return The total xp required to reach the level
     */
    public static long calculateXpForLvl(long lvl) {
        return 25 * lvl * (lvl + 1);
    }
}
