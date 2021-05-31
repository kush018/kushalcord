package org.example;

import discord4j.common.util.Snowflake;
import discord4j.core.DiscordClientBuilder;
import discord4j.core.GatewayDiscordClient;
import discord4j.core.event.domain.guild.GuildDeleteEvent;
import discord4j.core.event.domain.guild.MemberLeaveEvent;
import discord4j.core.event.domain.lifecycle.ReadyEvent;
import discord4j.core.event.domain.message.MessageCreateEvent;
import discord4j.core.object.entity.Message;
import discord4j.core.object.entity.User;
import discord4j.core.object.entity.channel.MessageChannel;
import discord4j.core.object.entity.channel.TextChannel;
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
    private static final long ONE_SECOND = 1000;
    /**
     * One minute in milliseconds (used for representing time)
     */
    private static final long ONE_MINUTE = 60 * ONE_SECOND;
    /**
     * One hour in milliseconds (used for representing time)
     */
    private static final long ONE_HOUR = 60 * ONE_MINUTE;
    /**
     * One day in milliseconds (used for representing time)
     */
    private static final long ONE_DAY = 24 * ONE_HOUR;

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
    private static final long USER_WORK_TIME_OUT = ONE_HOUR / 1000;

    /**
     * This is the money the user receives when he or she uses the command daily. (This is the daily bonus - in kc coins)
     */
    private static final long DAILY_ALLOWANCE = 10000;

    /**
     * When a user uses the "toprankers" command, he or she can specify the number of ranks to be displayed (from the message ranking system). This defines the maximum limit of ranks that can be displayed.
     */
    private static final int MAX_RANKS_TO_BE_DISPLAYED = 15;

    /**
     * This is the PSQLManager object.
     */
    private static PSQLManager psqlManager;

    /**
     * This is a HashMap that maps the command names to the "Command" object that the command must trigger.
     */
    private static Map<String, Command> commandsMap;

    /**
     * It is a HashMap which Maps a discord user with his or her spam cooldown (time left before cooldown is over). AtomicLong is used to store cooldown to ensure thread-safety. The cooldown value reduces by one every second. When the cooldown is zero the user can spam again.
     */
    private static HashMap<User, AtomicLong> userSpamCooldownTimerMap;
    /**
     * It is a HashMap which Maps a discord user with his or her work cooldown (time left before cooldown is over). AtomicLong is used to store cooldown to ensure thread-safety. The cooldown value reduces by one every second. When the cooldown is zero the user can work again.
     */
    private static HashMap<User, AtomicLong> userWorkCooldownTimerMap;

    /**
     * This is a list of the currently "running" jobs i.e., the Job objects that represents ongoing jobs - jobs which users are working on. CopyOnWriteArrayList is used instead of plain ArrayList for thread-safety.
     */
    private static CopyOnWriteArrayList<Job> currentWorkingJobsList;
    /**
     * This is a list of the currently "running" blackjack games i.e, the BJGame objects that represents ongoing blackjack games - games which the users are playing.
     */
    private static ArrayList<BJGame> runningBJGamesList;

    public static void main(String[] args) {

        /* This part initilises objects */
        userWorkCooldownTimerMap = new HashMap<>();
        userSpamCooldownTimerMap = new HashMap<>();
        currentWorkingJobsList = new CopyOnWriteArrayList<>();
        runningBJGamesList = new ArrayList<>();

        commandsMap = new HashMap<>();

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
        GatewayDiscordClient client = DiscordClientBuilder.create(apiKey)
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
        String helpMenu = "Use prefix: " + COMMAND_PREFIX + "\n" +
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
        HashMap<String, String> helpCommandsMap = new HashMap<>();
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

        /* The help command */
        commandsMap.put("help", (event, argv, argvStr) -> {
            if (argv.length == 0) {
                /*
                if no arguments were given, then the contents of the String "helpMenu" is simply shown to the user which shows a list
                of valid commands
                event.getMessage returns the Message involved in the MessageCreateEvent
                getChannel() returns the MessageChannel in which the message was sent
                createEmbed() creates a message in the corresponding MessageChannel as an embed
                the EmbedCreateSpec which specifies the details embed, is stored in an object "embed"
                setColor() sets the color of the embed
                setDescription() sets the text of the embed
                setTitle() sets the title of the embed
                */
                event.getMessage().getChannel().block().createEmbed((embed) -> {
                    embed.setColor(Color.DISCORD_BLACK);
                    embed.setDescription(helpMenu);
                    embed.setTitle("Help Menu");
                }).block();
            } else {
                /* 
                If arguments were given i.e., the help command was used like
                help <command>, in which case, the bot must tell the user details about <command>
                Now, we get the description of the corresponding command from HashMap helpCommandsMap using .get(argv[0])
                argv[0] means "first argument"
                */
                String helpMessage = helpCommandsMap.get(argv[0]);
                if (helpMessage == null) {
                    /* If the command does not exist as an entry in the helpCommandsMap, the HashMap will return null when .get() is called
                    and a corresponding message is sent to the user, tell him or her that the command entered is invalid */
                    event.getMessage().getChannel().block().createEmbed((embed) -> {
                        embed.setColor(Color.DISCORD_BLACK);
                        embed.setDescription("Not a valid command");
                        embed.setTitle("Help Menu");
                    }).block();
                } else {
                    event.getMessage().getChannel().block().createEmbed((embed) -> {
                        embed.setColor(Color.DISCORD_BLACK);
                        embed.setDescription(helpMessage);
                        embed.setTitle("Help Menu");
                    }).block();
                }
            }
        });

        /* This is a simple ping command
        When the user types the command ping,
        the bot replies with a simple message "Pong!" (there are no embeds here)*/
        commandsMap.put("ping", (event, argv, argvStr) -> event.getMessage()
                .getChannel().block()
                .createMessage("Pong!").block());

        /* This is a the ask command, also known as 8ball in other bots.
        Syntax: ask <question>. The question is meant to be a yes/no question.
        When this command is called, the bot gives a message to the user, giving a random answer*/
        commandsMap.put("ask", (event, argv, argvStr) -> {
             if (argvStr.length() == 0) {
                 /* If there are no arguments, it means the user didn't ask any question*/
                 event.getMessage().getChannel().block().createEmbed((embed) -> {
                     embed.setColor(Color.CYAN);
                     embed.setDescription("You didn't ask a question, genius");
                     embed.setTitle("Ask kushalCord");
                 }).block();
                 //if the user didnt ask a question, no point in going further so the function can end here.
                 return;
             }
             //the code reaches here only if the user asked a question
             //"answers" is a one dimensional array of Strings which contains all the answers to the question that the bot can give
             String[] answers = {"Yes",
                    "No",
                    "Probably",
                    "Probably not",
                    "lol idk",
                    "maybe ... ? im not sure",
                    "I don't want to answer your stupid questions",
                    "Definitely",
                    "Definitely not",
                    "Well yes, but actually no",
                    "Well no, but actually yes",
                    "It depends",
                    "hmmmmm idk"};
             //"answer" is a String that is picked randomly from the answers array
             String answer = answers[(int)(Math.random() * answers.length)];
             //the selected answer is then shown to the user
             event.getMessage().getChannel().block().createEmbed((embed) -> {
                embed.setColor(Color.CYAN);
                embed.setDescription(answer);
                embed.setTitle("Ask kushalCord");
             }).block();
        });

        /* The say command:
        Syntax: say <something>
        The bot messages <something> as a plain message and then deletes the original message in which the "say" command was issued
        This is a fun command as it makes it look like the bot said something out of its own free will */
        commandsMap.put("say", (event, argv, argvStr) -> {
            Message msg = event.getMessage();
            if (argv.length == 0) {
                //if no arguments are there, it means the user didnt specify what the bot must say
                msg.getChannel().block().createMessage("I can't just say nothing. What are you trying to do?").block();
            } else {
                //argvStr is a String which contains all the arguments in the form of a String
                //eg: if the user typed "say whatever 1 whatever 2" then argvStr = "whatever 1 whatever 2"
                msg.getChannel().block().createMessage(argvStr).block();
                msg.delete().block();
            }
        });

        /* The spam
        Usage: spam <n> <something>
        Sends the message <something> n times in the same channel in which the spam command was issued*/
        commandsMap.put("spam", (event, argv, argvStr) -> {
            MessageChannel channel = event.getMessage().getChannel().block();
            //the User who used the spam command is stored in the form of a User object in "spammer"
            //this is helpful in identifying the spammer, helping in knowing if the spam cooldown was completed.
            User spammer = event.getMessage().getAuthor().get();
            try {
                //gets the amount of seconds left as an AtomicLong for the cooldown to get over
                if (userSpamCooldownTimerMap.get(spammer) == null || userSpamCooldownTimerMap.get(spammer).get() == 0) {
                    //if its zero, that means the cooldown is over
                    //if its null, that means that the user has not even spammed yet
                    //either way, we let the user spam
                    //times stores the number of times the user wants the bot to spam (it is the first argument)
                    int times = Integer.parseInt(argv[0]);
                    if (times > 20) {
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
                    userSpamCooldownTimerMap.put(spammer, new AtomicLong((long)(Math.random() * (SPAM_TIME_OUT_MAX - SPAM_TIME_OUT_MIN) + SPAM_TIME_OUT_MIN)));
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
                if (userSpamCooldownTimerMap.get(spammer) == null || userSpamCooldownTimerMap.get(spammer).get() == 0) {
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
                    userSpamCooldownTimerMap.put(spammer, new AtomicLong((long)(Math.random() * (SPAM_TIME_OUT_MAX - SPAM_TIME_OUT_MIN) + SPAM_TIME_OUT_MIN)));
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
        });

        /* The delete command
        Usage: delete <n>
        Deletes the last n messages sent in the channel */
        commandsMap.put("delete", (event, argv, argvStr) -> {
            //channel contains the channel in which the message is sent
            TextChannel channel = (TextChannel) event.getMessage().getChannel().block();
            try {
                int n = Integer.parseInt(argv[0]);
                if (n < 1) {
                    event.getMessage().getChannel().block().createEmbed((embed) -> {
                        embed.setColor(Color.BROWN);
                        embed.setDescription("Bruh, lol what is your problem?");
                        embed.setTitle("Deleter");
                    }).block();
                    return;
                }
                //bulkdelete() can delete multiple messages at once, useful in our case as it would be inefficient to delete
                //n messages one by one
                //getMessagesBefore() gives us all the messages sent before a particular message
                //getLastMessageId() gets the last message in the channel
                //take(n) makes sure that out of all the messages sent before the message, we only take the last n messages
                //then, bulkdelete() will delete all the required messages
                channel.bulkDelete(channel.getMessagesBefore(channel.getLastMessageId().get())
                        .take(n)
                        .map(message -> message.getId())
                ).blockLast();
                //this deletes the command message.
                //for eg: if I delete delete 10, it will delete the message in which the command delete 10 is there and also delete 10 messages before it
                event.getMessage().delete().block();
                //this part shows a confirmation to the user that the required messages were deleted
                if (n == 1) {
                    //for good grammar
                    event.getMessage().getChannel().block().createEmbed((embed) -> {
                        embed.setColor(Color.BROWN);
                        embed.setDescription("1 message deleted");
                        embed.setTitle("Deleter");
                    }).block();
                } else {
                    event.getMessage().getChannel().block().createEmbed((embed) -> {
                        embed.setColor(Color.BROWN);
                        embed.setDescription(n + " messages deleted");
                        embed.setTitle("Deleter");
                    }).block();
                }
            } catch (NumberFormatException e) {
                event.getMessage().getChannel().block().createEmbed((embed) -> {
                    embed.setColor(Color.BROWN);
                    embed.setDescription("Bruh, thats not a valid number");
                    embed.setTitle("Deleter");
                }).block();
            }
        });

        /* The bal command
        Usage: bal
        OR bal <mention1> <mention2> <mention3> ... <mention n> */
        commandsMap.put("bal", (event, argv, argvStr) -> {
            //gets a Set of users that were mentioned
            Set<Snowflake> userMentionSet = event.getMessage().getUserMentionIds();
            if (userMentionSet.size() == 0) {
                //if no one was mentioned, then we must just print the balance of the user who issued the command
                User user = event.getMessage().getAuthor().get();
                try {
                    long bal = psqlManager.getBalance(user.getId().asString());
                    if (!psqlManager.isOperationSuccessful()) {
                        //if the user does not exist already
                        //then we must create the user
                        psqlManager.addAccount(user.getId().asString());
                    }
                    event.getMessage().getChannel().block().createEmbed((embed) -> {
                        embed.setColor(Color.GRAY_CHATEAU);
                        embed.setDescription("Your bank balance: " + bal);
                        embed.setTitle("Bank balance");
                    }).block();
                } catch (SQLException throwables) {
                    throwables.printStackTrace();
                }
            } else {
                //if someone was mentioned
                for (Snowflake userId : userMentionSet) {
                    try {
                        long bal = psqlManager.getBalance(userId.asString());
                        if (!psqlManager.isOperationSuccessful()) {
                            //if the user does not exist already
                            psqlManager.addAccount(userId.asString());
                        }
                        User currentUser = client.getUserById(userId).block();
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
        });

        commandsMap.put("rank", (event, argv, argvStr) -> {
            Set<Snowflake> userMentionSet = event.getMessage().getUserMentionIds();
            if (userMentionSet.size() == 0) {
                //if no one was mentioned
                String guildId = event.getMessage().getGuildId().get().asString();
                String userId = event.getMessage().getAuthor().get().getId().asString();
                try {
                    long xp = psqlManager.getXpOfUser(userId, guildId);
                    long level = calculateLevel(xp);
                    long nextLvlXp = calculateXpForLvl(level + 1);
                    event.getMessage().getChannel().block().createEmbed((embed) -> {
                        embed.setColor(Color.MAGENTA);
                        try {
                            embed.setDescription("Ur xp: " + xp + "/" + nextLvlXp +
                                    "\n" + "Ur lvl: " + level +
                                    "\n" + "Ur rank: " + (psqlManager.getRankOfUser(userId, guildId) + 1));
                        } catch (SQLException throwables) {
                            throwables.printStackTrace();
                        }
                        embed.setTitle("Xp Rank");
                    }).block();
                } catch (SQLException e) {
                    e.printStackTrace();
                }
            } else {
                //if someone was mentioned
                for (Snowflake userIdSnowflake : userMentionSet) {
                    String guildId = event.getMessage().getGuildId().get().asString();
                    String userId = userIdSnowflake.asString();
                    try {
                        long xp = psqlManager.getXpOfUser(userId, guildId);
                        long level = calculateLevel(xp);
                        long nextLvlXp = calculateXpForLvl(level + 1);
                        event.getMessage().getChannel().block().createEmbed((embed) -> {
                            embed.setColor(Color.MAGENTA);
                            try {
                                embed.setDescription("Rank of: " + client.getUserById(userIdSnowflake).block().getUsername() +
                                        "\n" + "xp: " + xp + "/" + nextLvlXp +
                                        "\n" + "lvl: " + level +
                                        "\n" + "rank: " + (psqlManager.getRankOfUser(userId, guildId) + 1));
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
        });

        commandsMap.put("toprankers", (event, argv, argvStr) -> {
            int n = 0;
            try {
                n = Integer.parseInt(argv[0]);
            } catch (NumberFormatException e) {
                event.getMessage().getChannel().block().createEmbed((embed) -> {
                    embed.setColor(Color.MOON_YELLOW);
                    embed.setDescription("Invalid number, bruh");
                    embed.setTitle("Xp Top Rankers");
                }).block();
                return;
            } catch (IndexOutOfBoundsException e) {
                n = 10;
            }
            if (n > MAX_RANKS_TO_BE_DISPLAYED) {
                //if max ranks have reached.
                event.getMessage().getChannel().block().createEmbed((embed) -> {
                    embed.setColor(Color.MOON_YELLOW);
                    embed.setDescription("I cant print that many ranks");
                    embed.setTitle("Xp Top Rankers");
                }).block();
                return;
            }
            String guildId = event.getMessage().getGuildId().get().asString();
            List<String> topRanksIds = null;
            try {
                topRanksIds = psqlManager.getTopFromGuildXpLeaderboard(guildId, n);
            } catch (SQLException e) {
                e.printStackTrace();
                return;
            }
            StringBuilder builder = new StringBuilder();
            for (int i = 0; i < topRanksIds.size(); i++) {
                String id = topRanksIds.get(i);
                try {
                    builder.append(i + 1).append(") ").append(client.getUserById(Snowflake.of(id)).block().getTag()).append(" - lvl ").append(calculateLevel(psqlManager.getXpOfUser(id, event.getMessage().getGuildId().get().asString()))).append("\n");
                } catch (SQLException ignored) {}
            }
            int finalN = n;
            event.getMessage().getChannel().block().createEmbed((embed) -> {
                embed.setColor(Color.MOON_YELLOW);
                embed.setDescription("Top " + finalN + " ranks:\n" + builder.toString());
                embed.setTitle("Xp Top Rankers");
            }).block();
        });

        commandsMap.put("daily", (event, argv, argvStr) -> {
            User user = event.getMessage().getAuthor().get();
            try {
                long dailyTaken = psqlManager.getDailyTaken(user.getId().asString());
                if (!psqlManager.isOperationSuccessful()) {
                    //if the user does not exist already
                    //then we must create the user
                    psqlManager.addAccount(user.getId().asString());
                }
                if (dailyTaken + ONE_DAY <= System.currentTimeMillis()) {
                    psqlManager.deposit(user.getId().asString(), DAILY_ALLOWANCE);
                    event.getMessage().getChannel().block().createEmbed((embed) -> {
                        embed.setColor(Color.DEEP_LILAC);
                        embed.setDescription("Added " + DAILY_ALLOWANCE + " kc coins to your account");
                        embed.setTitle("Get Daily Allowance");
                    }).block();
                    psqlManager.setDailyTaken(user.getId().asString(), System.currentTimeMillis());
                } else {
                    event.getMessage().getChannel().block().createEmbed((embed) -> {
                        embed.setColor(Color.DEEP_LILAC);
                        embed.setDescription("You've already received your daily kc coins allowance. Wait another 24 hours for more coins!");
                        embed.setTitle("Get Daily Allowance");
                    }).block();
                }
            } catch (SQLException throwables) {
                throwables.printStackTrace();
            }
        });

        commandsMap.put("transfer", (event, argv, argvStr) -> {
            long toTransfer = 0;
            try {
                toTransfer = Long.parseLong(argv[0]);
                if (toTransfer < 1) {
                    event.getMessage().getChannel().block().createEmbed((embed) -> {
                        embed.setColor(Color.DARK_GOLDENROD);
                        embed.setDescription("Bruh, what are you trying to do?");
                        embed.setTitle("Bank Transfer Request");
                    }).block();
                    return;
                }
            } catch (NumberFormatException e) {
                event.getMessage().getChannel().block().createEmbed((embed) -> {
                    embed.setColor(Color.DARK_GOLDENROD);
                    embed.setDescription("Yo, I can't transfer " + argv[0] + " coins. What are you trying to do?");
                    embed.setTitle("Bank Transfer Request");
                }).block();
                return;
            } catch (ArrayIndexOutOfBoundsException e) {
                event.getMessage().getChannel().block().createEmbed((embed) -> {
                    embed.setColor(Color.DARK_GOLDENROD);
                    embed.setDescription("Invalid usage syntax");
                    embed.setTitle("Bank Transfer Request");
                }).block();
                return;
            }
            User user = event.getMessage().getAuthor().get();
            try {
                if (toTransfer > psqlManager.getBalance(user.getId().asString())) {
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
            Set<Snowflake> mentionIdSet = event.getMessage().getUserMentionIds();
            if (mentionIdSet.size() == 0) {
                event.getMessage().getChannel().block().createEmbed((embed) -> {
                    embed.setColor(Color.DARK_GOLDENROD);
                    embed.setDescription("You haven't told me who to send the money to!");
                    embed.setTitle("Bank Transfer Request");
                }).block();
                return;
            }
            Snowflake user2Id = (Snowflake)mentionIdSet.toArray()[0];
            try {
                psqlManager.getBalance(user2Id.asString());
                if (!psqlManager.isOperationSuccessful()) {
                    //if the bank account does not exist, we need to create it.
                    psqlManager.addAccount(user2Id.asString());
                }
            } catch (SQLException e) {
                e.printStackTrace();
                return;
            }
            try {
                psqlManager.withdraw(user.getId().asString(), toTransfer);
                psqlManager.deposit(user2Id.asString(), toTransfer);
            } catch (SQLException e) {
                e.printStackTrace();
            }
            long finalToTransfer = toTransfer;
            event.getMessage().getChannel().block().createEmbed((embed) -> {
                embed.setColor(Color.DARK_GOLDENROD);
                embed.setDescription("Sucessfully transferred " + finalToTransfer);
                embed.setTitle("Bank Transfer Request");
            }).block();
        });

        commandsMap.put("work", (event, argv, argvStr) -> {
            if (userWorkCooldownTimerMap.get(event.getMessage().getAuthor().get()) == null
                    || userWorkCooldownTimerMap.get(event.getMessage().getAuthor().get()).get() == 0) {
                userWorkCooldownTimerMap.put(event.getMessage().getAuthor().get(), new AtomicLong(USER_WORK_TIME_OUT));
            } else {
                event.getMessage().getChannel().block().createEmbed((embed) -> {
                    embed.setColor(Color.DISCORD_WHITE);
                    embed.setDescription("You've already finished your one-hour shift. Take a break!");
                    embed.setTitle("Work Application");
                }).block();
                return;
            }
            try {
                String jobName = argv[0];
                Job job = new Job(event, jobName);
                currentWorkingJobsList.add(job);
            } catch (ArrayIndexOutOfBoundsException e) {
                userWorkCooldownTimerMap.put(event.getMessage().getAuthor().get(), new AtomicLong(0));
                event.getMessage().getChannel().block().createEmbed((embed) -> {
                    embed.setColor(Color.DISCORD_WHITE);
                    embed.setDescription("You haven't told me what job you want to do!");
                    embed.setTitle("Work Application");
                }).block();
            } catch (InvalidJobException e) {
                userWorkCooldownTimerMap.put(event.getMessage().getAuthor().get(), new AtomicLong(0));
                event.getMessage().getChannel().block().createEmbed((embed) -> {
                    embed.setColor(Color.DISCORD_WHITE);
                    embed.setDescription("Ain't nobody got no time for that kind of a job!");
                    embed.setTitle("Work Application");
                }).block();
            }
        });

        commandsMap.put("bj", (event, argv, argvStr) -> {
            MessageChannel messageChannel = event.getMessage().getChannel().block();
            User user = event.getMessage().getAuthor().get();
            if (argv.length == 0) {
                event.getMessage().getChannel().block().createEmbed((embed) -> {
                    embed.setColor(Color.DEEP_SEA);
                    embed.setDescription("U havent told me how much money u wanna gamble");
                    embed.setTitle("The BlackJack Casino");
                }).block();
                return;
            }
            long toGamble;
            try {
                toGamble = Long.parseLong(argv[0]);
            } catch (NumberFormatException e) {
                event.getMessage().getChannel().block().createEmbed((embed) -> {
                    embed.setColor(Color.DEEP_SEA);
                    embed.setDescription("Not a valid number!");
                    embed.setTitle("The BlackJack Casino");
                }).block();
                return;
            }
            if (toGamble < 1) {
                event.getMessage().getChannel().block().createEmbed((embed) -> {
                    embed.setColor(Color.DEEP_SEA);
                    embed.setDescription("What are you trying to do? lol");
                    embed.setTitle("The BlackJack Casino");
                }).block();
                return;
            }
            long balance;
            try {
                balance = psqlManager.getBalance(user.getId().asString());
            } catch (SQLException e) {
                e.printStackTrace();
                return;
            }
            if (toGamble > balance) {
                event.getMessage().getChannel().block().createEmbed((embed) -> {
                    embed.setColor(Color.DEEP_SEA);
                    embed.setDescription("You cant gamble more than you own!");
                    embed.setTitle("The BlackJack Casino");
                }).block();
                return;
            }
            BJGame bjGame = new BJGame(event, toGamble);
            runningBJGamesList.add(bjGame);
            event.getMessage().getChannel().block().createEmbed((embed) -> {
                embed.setColor(Color.DEEP_SEA);
                embed.setDescription("Ur cards: " + bjGame.getUserCards() +
                        "\nh=hit, s=stand, e=end");
                embed.setTitle("The BlackJack Casino");
            }).block();
        });
		
		TimerTask reduceTimeOutTask = new TimerTask() {
            @Override
            public void run() {
                for (Map.Entry<User, AtomicLong> entry : userSpamCooldownTimerMap.entrySet()) {
                    if (entry.getValue().get() != 0) {
                        entry.getValue().decrementAndGet();
                    }
                }
            }
        };
        Timer timer = new Timer();
        timer.scheduleAtFixedRate(reduceTimeOutTask, 0, 1000);

        TimerTask checkJobsTimeOut = new TimerTask() {
            @Override
            public void run() {
                for (Job job : currentWorkingJobsList) {
                    if (job.isTimeOver()) {
                        //if times up
                        job.getWorkingChannel().createEmbed((embed) -> {
                            embed.setColor(Color.DISCORD_WHITE);
                            embed.setDescription("Terrible job " +
                                    job.getWorkingUser().getUsername() + ". You are too slow.\n" +
                                    "I will not be paying you anything for that.");
                            embed.setTitle("Work Application");
                        }).block();
                        currentWorkingJobsList.remove(job);
                    }
                }
            }
        };
        timer.scheduleAtFixedRate(checkJobsTimeOut, 0, 1000);

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

        client.getEventDispatcher().on(MessageCreateEvent.class)
                .subscribe(event -> {
                    try {
                        String content = event.getMessage().getContent();
                        Optional<User> userOptional = event.getMessage().getAuthor();
                        if (userOptional.isPresent() && userOptional.get().isBot()) {
                            return;
                        }

                        applyMessageToRankingSystem(event);

                        if (applyMessageToJobsSystem(event)) {
                            return;
                        }

                        if (applyMessageToBJSystem(event)) {
                            return;
                        }

                        if (content.startsWith(COMMAND_PREFIX)) {
                            String command = content.substring(COMMAND_PREFIX.length());
                            command = command.split(" ")[0];
                            for (Map.Entry<String, Command> entry : commandsMap.entrySet()) {
                                if (entry.getKey().equals(command)) {
                                    //this is for valid commands
                                    String arguments = content.substring(COMMAND_PREFIX.length());
                                    if (arguments.length() == command.length()) {
                                        //if there is only the command and no other arguments
                                        arguments = "";
                                    } else {
                                        arguments = arguments.substring(command.length() + 1);
                                    }
                                    entry.getValue().execute(event, divideIntoArgs(arguments), arguments);
                                    return;
                                }
                            }
                            String finalCommand = command;
                            event.getMessage().getChannel().block().createEmbed((embed) -> {
                                embed.setColor(Color.CINNABAR);
                                embed.setDescription("Invalid command: " + finalCommand);
                                embed.setTitle("Invalid Command");
                            }).block();
                        }
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                });

        client.getEventDispatcher().on(MemberLeaveEvent.class)
                .subscribe(event -> {
                    try {
                        String guildId = event.getGuildId().asString();
                        String userId = event.getUser().getId().asString();
                        try {
                            psqlManager.removeUserFromGuildXpSystem(userId, guildId);
                        } catch (SQLException ignored) {
                        }
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                });

        client.getEventDispatcher().on(GuildDeleteEvent.class)
                .subscribe(event -> {
                    try {
                        if (!event.isUnavailable()) {
                            //if the event was not triggered only because there was an outage.
                            String guildId = event.getGuildId().asString();
                            try {
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

    public static boolean applyMessageToJobsSystem(MessageCreateEvent event) {
        for (Job job : currentWorkingJobsList) {
            //for each job in currentJobs
            if (job.checkChannel(event) && job.checkUser(event)) {
                if (job.checkJob(event)) {
                    event.getMessage().getChannel().block().createEmbed((embed) -> {
                        embed.setColor(Color.DISCORD_WHITE);
                        embed.setDescription("Great job "
                                + event.getMessage().getAuthor().get().getUsername()
                                + "! You've earned yourself " + job.getWage());
                        embed.setTitle("Work Application");
                    }).block();
                    try {
                        psqlManager.deposit(event.getMessage().getAuthor().get().getId().asString(), job.getWage());
                    } catch (SQLException ignored) {
                    }
                } else {
                    event.getMessage().getChannel().block().createEmbed((embed) -> {
                        embed.setColor(Color.DISCORD_WHITE);
                        embed.setDescription("Terrible job " +
                                event.getMessage().getAuthor().get().getUsername() + ". This is not expected from you.\n" +
                                "I will not be paying you anything for that.");
                        embed.setTitle("Work Application");
                    }).block();
                }
                currentWorkingJobsList.remove(job);
                return true;
            }
        }
        return false;
    }

    public static boolean applyMessageToBJSystem(MessageCreateEvent event) {
        for (BJGame bjGame : runningBJGamesList) {
            //for each game in BJGamesList
            User user = event.getMessage().getAuthor().get();
            MessageChannel messageChannel = event.getMessage().getChannel().block();
            if (bjGame.checkChannel(event) && bjGame.checkUser(event)) {
                String message = event.getMessage().getContent();
                int result = 0;
                if (message.equals("h")) {
                    bjGame.hit();
                    result = bjGame.getGameStatus();
                } else if (message.equals("s")) {
                    bjGame.stand();
                    result = bjGame.getGameStatus();
                } else if (message.equals("e")) {
                    event.getMessage().getChannel().block().createEmbed((embed) -> {
                        embed.setColor(Color.DEEP_SEA);
                        embed.setDescription("Blackjack game ended. You wont win or lose anything.\n" +
                                "But heres your cards: " + bjGame.getUserCards() +
                                "\nAnd here is the dealers cards: " + bjGame.getDealerCards());
                        embed.setTitle("The BlackJack Casino");
                    }).block();
                    runningBJGamesList.remove(bjGame);
                    return true;
                } else {
                    event.getMessage().getChannel().block().createEmbed((embed) -> {
                        embed.setColor(Color.DEEP_SEA);
                        embed.setDescription("Invalid command\nUr cards: " + bjGame.getUserCards() +
                                "\nh=hit, s=stand, e=end");
                        embed.setTitle("The BlackJack Casino");
                    }).block();
                    return true;
                }

                if (result == BJGame.STATUS_LOSE) {
                    event.getMessage().getChannel().block().createEmbed((embed) -> {
                        embed.setColor(Color.DEEP_SEA);
                        embed.setDescription("Ur cards: " + bjGame.getUserCards() +
                                "\nDealers cards: " + bjGame.getDealerCards() +
                                "\nYou lose! Say bye to " + bjGame.getToGamble());
                        embed.setTitle("The BlackJack Casino");
                    }).block();
                    try {
                        psqlManager.withdraw(user.getId().asString(), bjGame.getToGamble());
                    } catch (SQLException e) {
                        e.printStackTrace();
                    }
                    runningBJGamesList.remove(bjGame);
                } else if (result == BJGame.STATUS_WIN) {
                    event.getMessage().getChannel().block().createEmbed((embed) -> {
                        embed.setColor(Color.DEEP_SEA);
                        embed.setDescription("Ur cards: " + bjGame.getUserCards() +
                                "\nDealers cards: " + bjGame.getDealerCards() +
                                "\nYou won! You win " + bjGame.getToGamble());
                        embed.setTitle("The BlackJack Casino");
                    }).block();
                    try {
                        psqlManager.deposit(user.getId().asString(), bjGame.getToGamble());
                    } catch (SQLException e) {
                        e.printStackTrace();
                    }
                    runningBJGamesList.remove(bjGame);
                } else if (result == BJGame.STATUS_TIE) {
                    event.getMessage().getChannel().block().createEmbed((embed) -> {
                        embed.setColor(Color.DEEP_SEA);
                        embed.setDescription("Ur cards: " + bjGame.getUserCards() +
                                "\nDealers cards: " + bjGame.getDealerCards() +
                                "\nIts a tie! You dont lose or win any money");
                        embed.setTitle("The BlackJack Casino");
                    }).block();
                    runningBJGamesList.remove(bjGame);
                } else {
                    event.getMessage().getChannel().block().createEmbed((embed) -> {
                        embed.setColor(Color.DEEP_SEA);
                        embed.setDescription("Ur cards: " + bjGame.getUserCards() +
                                "\nh=hit, s=stand, e=end");
                        embed.setTitle("The BlackJack Casino");
                    }).block();
                }
                return true;
            }
        }
        return false;
    }

    public static void applyMessageToRankingSystem(MessageCreateEvent event) {
        long timeOfMessage = System.currentTimeMillis();
        if (!event.getMessage().getAuthor().isPresent()) {return;}
        User messageAuthor = event.getMessage().getAuthor().get();
        String userId = messageAuthor.getId().asString();
        String guildId = event.getMessage().getGuildId().get().asString();
        try {
            //adds guild to the xp system (if present it will do nothing)
            psqlManager.addGuildToXpSystem(guildId);
            //adds used to the guild xp system (if present it will do nothing)
            psqlManager.addUserToGuildXpSystem(userId, guildId);
            //checks last time of message
            long lastMessageSentTime = psqlManager.getLastMessageTime(userId, guildId);
            if (lastMessageSentTime + ONE_MINUTE > timeOfMessage) {
                return;
            }
            psqlManager.setLastMessageTimeOfUser(userId, guildId, timeOfMessage);
            //adds xp to user
            long xpToAdd = (long) (Math.random() * (26 - 15) + 15);
            long newXp = psqlManager.getXpOfUser(userId, guildId) + xpToAdd;
            psqlManager.setXpOfUser(userId, guildId, newXp);
            if (calculateLevel(newXp) != calculateLevel(newXp - xpToAdd)) {
                //the player has increased by one level.
                event.getMessage().getChannel().block().createMessage("Congratulations <@" + userId + ">! Your new level is: " + calculateLevel(newXp)).block();
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public static String[] divideIntoArgs(String s) {
        ArrayList<String> contentArr = new ArrayList<>();
        boolean foundQuotationMarks = false;
        boolean newWord = true;
        for (int i = 0; i < s.length(); i++) {
            if (s.charAt(i) == ' ') {
                if (!foundQuotationMarks) {
                    newWord = true;
                } else {
                    if (newWord) {
                        contentArr.add(Character.toString(s.charAt(i)));
                        newWord = false;
                    } else {
                        contentArr.set(contentArr.size() - 1, contentArr.get(contentArr.size() - 1) + s.charAt(i));
                    }
                }
            } else if (s.charAt(i) == '"') {
                if (!foundQuotationMarks) {
                    foundQuotationMarks = true;
                } else {
                    foundQuotationMarks = false;
                    newWord = true;
                }
            } else {
                if (newWord) {
                    contentArr.add(Character.toString(s.charAt(i)));
                    newWord = false;
                } else {
                    contentArr.set(contentArr.size() - 1, contentArr.get(contentArr.size() - 1) + s.charAt(i));
                }
            }
        }
        String[] argvArr = new String[contentArr.size()];
        for (int i = 0; i < contentArr.size(); i++) {
            argvArr[i] = contentArr.get(i);
        }
        return argvArr;
    }

    public static long calculateLevel(long xp) {
        return (long) Math.floor(
                (Math.sqrt(625 + 100 * xp) - 25)
                /50
        );
    }

    public static long calculateXpForLvl(long lvl) {
        return 25 * lvl * (lvl + 1);
    }
}
