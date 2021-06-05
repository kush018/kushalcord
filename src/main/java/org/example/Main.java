package org.example;

import discord4j.core.DiscordClientBuilder;
import discord4j.core.GatewayDiscordClient;
import discord4j.core.event.domain.guild.GuildDeleteEvent;
import discord4j.core.event.domain.guild.MemberLeaveEvent;
import discord4j.core.event.domain.lifecycle.ReadyEvent;
import discord4j.core.event.domain.message.MessageCreateEvent;
import discord4j.core.object.entity.User;
import discord4j.core.object.entity.channel.MessageChannel;
import discord4j.core.object.presence.Activity;
import discord4j.core.object.presence.Presence;
import discord4j.rest.util.Color;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.sql.SQLException;
import java.util.*;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicLong;

public class Main {
    /**
     * Path to the file containing the API key for the discord bot, relative to the project root folder.
     */
    public static final String APIKEY_FILE = "conf/api_key";

    /**
     * Prefix for the bot commands
     */
    public static final String COMMAND_PREFIX = "kc ";

    /**
     * Status of the bot right after login.
     */
    public static final String INITIAL_BOT_STATUS = COMMAND_PREFIX + "help";

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
     * When a user uses the "spam" command a cooldown is required before he or she can use the command again. The spam cooldown is decided by picking a random number between a lower and upper limit. This is the lower limit. (in seconds)
     */
    public static final long SPAM_TIME_OUT_MIN = 15;
    /**
     * When a user uses the "spam" command a cooldown is required before he or she can use the command again. The spam cooldown is decided by picking a random number between a lower and upper limit. This is the upper limit. (in seconds)
     */
    public static final long SPAM_TIME_OUT_MAX = 120;

    /**
     * When a user uses the "work" command, a cooldown is required before he or she can use the command again. This field defines that cooldown (in seconds).
     */
    public static final long USER_WORK_TIME_OUT = ONE_HOUR / 1000;

    /**
     * This is the money the user receives when he or she uses the command daily. (This is the daily bonus - in kc coins)
     */
    public static final long DAILY_ALLOWANCE = 10000;

    /**
     * When a user uses the "toprankers" command, he or she can specify the number of ranks to be displayed (from the message ranking system). This defines the maximum limit of ranks that can be displayed.
     */
    public static final int MAX_RANKS_TO_BE_DISPLAYED = 15;

    /**
     * This is the PSQLManager object.
     */
    public static PSQLManager psqlManager;

    /**
     * It is a HashMap which Maps a discord user with his or her spam cooldown (time left before cooldown is over). AtomicLong is used to store cooldown to ensure thread-safety. The cooldown value reduces by one every second. When the cooldown is zero the user can spam again.
     */
    public static HashMap<User, AtomicLong> userSpamCooldownTimerMap;
    /**
     * It is a HashMap which Maps a discord user with his or her work cooldown (time left before cooldown is over). AtomicLong is used to store cooldown to ensure thread-safety. The cooldown value reduces by one every second. When the cooldown is zero the user can work again.
     */
    public static HashMap<User, AtomicLong> userWorkCooldownTimerMap;

    /**
     * This is a list of the currently "running" jobs i.e., the Job objects that represents ongoing jobs - jobs which users are working on. CopyOnWriteArrayList is used instead of plain ArrayList for thread-safety.
     */
    public static CopyOnWriteArrayList<Job> currentWorkingJobsList;
    /**
     * This is a list of the currently "running" blackjack games i.e, the BJGame objects that represents ongoing blackjack games - games which the users are playing.
     */
    public static ArrayList<BJGame> runningBJGamesList;

    public static GatewayDiscordClient client;

    public static HashMap<String, String> helpCommandsMap;

    public static String helpMenu;

    public static CommandFactory commandFactory;

    public static void main(String[] args) {

        /* This part initilises objects */
        userWorkCooldownTimerMap = new HashMap<>();
        userSpamCooldownTimerMap = new HashMap<>();
        currentWorkingJobsList = new CopyOnWriteArrayList<>();
        runningBJGamesList = new ArrayList<>();

        commandFactory = new CommandFactory();

        /* Initialises the PSQLManager object */ 
        try {
            psqlManager = new PSQLManager();
        } catch (ClassNotFoundException | SQLException e) {
            //on creation of the PSQLManager object, the constructor may throw some exceptions which are caught here
            //on catching these exceptions, the program simply prints the stacktrace to stdout and end the program as the bot cannot function properly without a database
            e.printStackTrace();
            return;
        }

        if (!psqlManager.isPsqlManagerCreatedSuccessfully()) {
            //this happens when the "address", username or password have not been configured correctly, or do not exist
            //in which case the connection to the database cannot be set up and the program terminates
            System.out.println("Unable to set up connection to the database.");
            return;
        }

        //FileReader object create to read from the file containing the API key for the discord bot
        FileReader fileReader;
        try {
            //initialises the FileReader object with APIKEY_FILE, which is the path to the API key, relative to the project's
            //root directory. This creates a FileReader which can read from file at path APIKEY_FILE
            fileReader = new FileReader(APIKEY_FILE);
        } catch (FileNotFoundException e) {
            //if a FileNotFoundException is thrown, it means that the APIKEY_FILE does not exist
            System.out.println("File: " + APIKEY_FILE + " not found. Please make the file and type the discord bot's API key in it.");
            //without the API key, a connection with discord is not possible, hence the program is terminated
            return;
        }
        //creates a BufferedReader which is responsible for buffering input from the FileReader
        //the BufferedReader buffers the input from the FileReader and returns it to the program.
        BufferedReader reader = new BufferedReader(fileReader);
        //the String apiKey stores our API key which is present at the first line of the API key file
        String apiKey;
        try {
            //reads the first line of the file APIKEY_FILE from the FileReader and returns it as a String
            apiKey = reader.readLine();
        } catch (IOException e) {
            //while reading a file an IO related exception might occur. If this happens then it is caught and an error message is printed
            System.out.println("An IO exception occurred while reading file: " + APIKEY_FILE);
            //since we dont have the apikey, there is no point in going any further
            return;
        }

        //creates a connection with discord using the obtained API key and creates a GatewayDiscordClient
        client = DiscordClientBuilder.create(apiKey)
                .build().login().block();

        //this part of the code decides what the program should do when we have successfully logged into discord using the apikey
        //the ReadyEvent is triggered whenever we have successfully logged in
        //subscribe() tells our program that it needs to execute the code that follows whenever the event is triggered
        client.getEventDispatcher().on(ReadyEvent.class)
                .subscribe(event -> {
                    //the ReadyEvent object that triggered the ReadyEvent is stored in an object "event"
                    //Obtains and stores the bot account's User in an object called "self"
                    User self = event.getSelf();
                    //prints a confirmation telling the username and discriminator of the bot account
                    System.out.printf(
                            "Logged in as %s#%s%n", self.getUsername(), self.getDiscriminator()
                    );
                    //updates the bot's status on discord, saying that it is online and "watching" the value in INITIAL_BOT_STATUS
                    client.updatePresence(Presence.online(Activity.watching(INITIAL_BOT_STATUS))).block();
                });

        /* This String is the String that gets printed when the help command is used.
        It gives a list of all the valid commands as well as the prefix that this bot uses. */
        helpMenu = "Use prefix: " + COMMAND_PREFIX + "\n" +
                "Commands:\n" +
                "1) help\n" +
                "2) ping\n" +
                "3) ask\n" +
                "4) spam\n" +
                "5) delete\n" +
                "6) bal\n" +
                "7) daily\n" +
                "8) transfer\n" +
                "9) rank\n" +
                "10) toprankers\n" +
                "11) work\n" +
                "12) say\n" +
                "13) bj\n" +
                "NOTE: Type \"kc help <any command> for more info on that command";

        /* This HashMap maps a command to its help menu message.
        The help menu message is displayed when help <command> is typed */
        helpCommandsMap = new HashMap<>();
        helpCommandsMap.put("help", "help - prints general help menu\n" +
                "help <command> - prints help menu for command");
        helpCommandsMap.put("ping", "ping - causes bot to reply with Pong!\n" +
                "(used to test if bot is running properly or not)");
        helpCommandsMap.put("ask", "ask <question> - ask a yes/no type question to the bot to hear the answer\n" +
                "(its not real its just for fun. the bot only gives random answers)");
        helpCommandsMap.put("spam", "spam <message> - spams a message 5 times\n" +
                "spam <n> <message> - spams a message n times\n" +
                "(use at your own risk lol)");
        helpCommandsMap.put("delete", "delete <n> - deletes the last n messages, and the delete message request");
        helpCommandsMap.put("bal", "bal - tells your bank balance\n" +
                "bal <mention1> <mention2> ... - tells the bank balance of all the users mentioned");
        helpCommandsMap.put("daily", "daily - collect your daily allowance");
        helpCommandsMap.put("transfer", "transfer <amt> <mention> - transfers amt coins to user mentioned");
        helpCommandsMap.put("rank", "rank - tells ur xp, level and rank\n" +
                "rank <mention1> <mention2> ... - tells the rank of all users mentioned");
        helpCommandsMap.put("toprankers", "toprankers <n> - prints the leaderboard of the top n ranks in the guild xp system\n" +
                "toprankers - same as the above commands where n = 10");
        helpCommandsMap.put("work", "work <job> - work for a job\n" +
                "currently available jobs: hacker, paanwala");
        helpCommandsMap.put("say", "say <something> - make the bot say something and delete the command so that it looks like the bot" +
                " said it out of its own free will");
        helpCommandsMap.put("bj", "bj <amount> - gamble your money away in a game of blackjack");
		
        //a TimerTask represents a certain task to be repeated at fixed intervals
        //this timertask manages the spam cooldown timers for each user
		TimerTask reduceTimeOutTask = new TimerTask() {
            @Override
            public void run() {
                //for every entry (key-value pair) in the userSpamCooldownTimerMap,
                for (Map.Entry<User, AtomicLong> entry : userSpamCooldownTimerMap.entrySet()) {
                    //if the value of the entry is not zero
                    if (entry.getValue().get() != 0) {
                        //then decrement the value
                        entry.getValue().decrementAndGet();
                    }
                }
            }
        };
        //creates a Timer object which can run a particular timertask at fixed intervals
        Timer timer = new Timer();
        //the timertask reduceTimeOutTask is run every 1000 milliseconds
        timer.scheduleAtFixedRate(reduceTimeOutTask, 0, 1000);

        //manages the job timeout
        //all users only have a spcecific amount of time for completing a job
        //if they dont complete it in the stipulated time
        //they wont be awarded any money
        TimerTask checkJobsTimeOut = new TimerTask() {
            @Override
            public void run() {
                //for each job in the list of currently running jobs,
                for (Job job : currentWorkingJobsList) {
                    if (job.isTimeOver()) {
                        //if times up, tell the user the times up
                        job.getWorkingChannel().createEmbed((embed) -> {
                            embed.setColor(Color.DISCORD_WHITE);
                            embed.setDescription("Terrible job " +
                                    job.getWorkingUser().getUsername() + ". You are too slow.\n" +
                                    "I will not be paying you anything for that.");
                            embed.setTitle("Work Application");
                        }).block();
                        //remove the job from the list of currently running jobs as the job is not "running" anymore
                        currentWorkingJobsList.remove(job);
                    }
                }
            }
        };
        timer.scheduleAtFixedRate(checkJobsTimeOut, 0, 1000);

        //manages the work cooldown timer for each user
        TimerTask reduceWorkTimeOut = new TimerTask() {
            @Override
            public void run() {
                for (Map.Entry<User, AtomicLong> entry : userWorkCooldownTimerMap.entrySet()) {
                    if (entry.getValue().get() != 0) {
                        entry.getValue().decrementAndGet();
                    }
                }
            }
        };
        timer.scheduleAtFixedRate(reduceWorkTimeOut, 0, 1000);

        //when a Message is sent the MessageCreateEvent gets triggered
        client.getEventDispatcher().on(MessageCreateEvent.class)
                .subscribe(event -> {
                    //this is run everytime a MessageCreateEvent is triggered
                    try {
                        //gets the content of the message
                        String content = event.getMessage().getContent();
                        //gets the user who issued the command, wrapped in an optional
                        Optional<User> userOptional = event.getMessage().getAuthor();
                        //if the user is actually present in the optional and is a bot, then end this function
                        //(because we dont take commands from bots!)
                        if (userOptional.isPresent() && userOptional.get().isBot()) {
                            return;
                        }

                        //applies message to the ranking system
                        applyMessageToRankingSystem(event);

                        //applies message to the job system
                        //if the job system does not need the message to be processed any further, the function returns true and we can end the current function
                        if (applyMessageToJobsSystem(event)) {
                            return;
                        }

                        //applies message to BJ system
                        if (applyMessageToBJSystem(event)) {
                            return;
                        }

                        //if the message content starts with the command prefix i.e., it is a command for our bot
                        if (content.startsWith(COMMAND_PREFIX)) {
                            //we get rid of the command prefix so we can focus on the actual command
                            String command = content.substring(COMMAND_PREFIX.length());
                            //the command name is basically the first word in the command string
                            //we split the command by spaces into an array of Strings
                            //so if the command was "help bal"
                            //then it would be split into "help" and "bal". We take the first String from this array
                            //so command will now hold the command name which is "help" here
                            command = command.split(" ")[0];
                            Command commandToRun = commandFactory.getCommandByName(command);
                            if (commandToRun != null) {
                                //if the key (which is the name of the command) is equal to the command issued here
                                //we now need to find the arguments to the command
                                //we first take the content and rip it off the command prefix
                                String arguments = content.substring(COMMAND_PREFIX.length());
                                if (arguments.length() == command.length()) {
                                    //if there is only the command and no other arguments
                                    arguments = "";
                                } else {
                                    //if there are arguments, we need to rip the arguments string off the command name
                                    arguments = arguments.substring(command.length() + 1);
                                }
                                //this executes the command and supplies it with the event, arguments as an array and arguments as a String
                                //divideIntoArgs() divides the String into a String array of arguments
                                commandToRun.execute(event, divideIntoArgs(arguments), arguments);
                                //we have found the command and have executed the command, so the job is now over.
                                //we dont have to go further in this function and the function is ended.
                                return;
                            }
                            //if we reach here, it means that the command was not found and is therefore an invalid command
                            String finalCommand = command;
                            event.getMessage().getChannel().block().createEmbed((embed) -> {
                                embed.setColor(Color.CINNABAR);
                                embed.setDescription("Invalid command: " + finalCommand);
                                embed.setTitle("Invalid Command");
                            }).block();
                        }
                    } catch (Exception e) {
                        //in case of any exception it is caught here, preventing a complete crash
                        e.printStackTrace();
                    }
                });

        //when a member is kicked (or banned) or leaves a server which the bot is in, the MemberLeaveEvent is triggered
        client.getEventDispatcher().on(MemberLeaveEvent.class)
                .subscribe(event -> {
                    try {
                        //we get the guildId and serverId
                        String guildId = event.getGuildId().asString();
                        String userId = event.getUser().getId().asString();
                        try {
                            //removes the user from the xp system of the server as he or she has left or been kicked
                            psqlManager.removeUserFromGuildXpSystem(userId, guildId);
                        } catch (SQLException ignored) {
                        }
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                });

        //when a server is deleted, or the bot is kicked (or banned) from the server, a GuildDeleteEvent is
        client.getEventDispatcher().on(GuildDeleteEvent.class)
                .subscribe(event -> {
                    try {
                        if (!event.isUnavailable()) {
                            //if the event was not triggered only because there was an outage (because this event could get triggered in the case of an outage as well)
                            String guildId = event.getGuildId().asString();
                            try {
                                //removes guild from xp system because the guild is essentially "deleted" and continuing to keep it will just waste space
                                psqlManager.removeGuildFromXpSystem(guildId);
                            } catch (SQLException ignored) {
                            }
                        }
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                });

        client.onDisconnect().block();
    }

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
