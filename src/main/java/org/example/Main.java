package org.example;

import discord4j.core.DiscordClientBuilder;
import discord4j.core.GatewayDiscordClient;
import discord4j.core.event.domain.guild.GuildDeleteEvent;
import discord4j.core.event.domain.guild.MemberLeaveEvent;
import discord4j.core.event.domain.lifecycle.ReadyEvent;
import discord4j.core.event.domain.message.MessageCreateEvent;
import discord4j.core.object.entity.User;
import discord4j.core.object.presence.Activity;
import discord4j.core.object.presence.Presence;
import discord4j.rest.util.Color;
import org.example.commands.*;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.sql.SQLException;
import java.util.*;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicLong;

import static org.example.Utils.*;

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

    public static HashMap<String, Command> commandsMap;

    public static void main(String[] args) {

        /* This part initilises objects */
        userWorkCooldownTimerMap = new HashMap<>();
        userSpamCooldownTimerMap = new HashMap<>();
        currentWorkingJobsList = new CopyOnWriteArrayList<>();
        runningBJGamesList = new ArrayList<>();

        commandsMap = new HashMap<>();

        commandsMap.put("ask", new AskCommand());
        commandsMap.put("bal", new BalanceCommand());
        commandsMap.put("bj", new BlackJackCommand());
        commandsMap.put("daily", new DailyCommand());
        commandsMap.put("delete", new DeleteCommand());
        commandsMap.put("ping", new PingCommand());
        commandsMap.put("rank", new RankCommand());
        commandsMap.put("say", new SayCommand());
        commandsMap.put("spam", new SpamCommand());
        commandsMap.put("toprankers", new TopRankersCommand());
        commandsMap.put("transfer", new TransferCommand());
        commandsMap.put("work", new WorkCommand());

        commandsMap.put("help", new HelpCommand());

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
                            Command commandToRun = commandsMap.get(command);
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
}
