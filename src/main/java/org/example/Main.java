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

    public static final String APIKEY_FILE = "conf/api_key";

    public static final long SPAM_TIME_OUT_MIN = 15;
    public static final long SPAM_TIME_OUT_MAX = 120;

    private static Map<String, Command> commands;

    private static Map<User, AtomicLong> userTimeOutMap;

    private static PSQLManager psqlManager;

    private static final long ONE_SECOND = 1000;
    private static final long ONE_MINUTE = 60 * ONE_SECOND;
    private static final long ONE_HOUR = 60 * ONE_MINUTE;
    private static final long ONE_DAY = 24 * ONE_HOUR;

    private static final long DAILY_ALLOWANCE = 10000;

    private static final int MAX_RANKS = 15;

    private static CopyOnWriteArrayList<Job> currentJobs;

    private static HashMap<User, AtomicLong> userWorkTimeOutMap;

    private static final long USER_WORK_TIME_OUT = ONE_HOUR / 1000;

    private static ArrayList<BJGame> BJGamesList;

    public static void main(String[] args) {

        BJGamesList = new ArrayList<>();

        userWorkTimeOutMap = new HashMap<>();

        currentJobs = new CopyOnWriteArrayList<>();

        try {
            psqlManager = new PSQLManager();
        } catch (ClassNotFoundException | SQLException e) {
            e.printStackTrace();
        }

        if (!psqlManager.isPsqlManagerCreatedSuccessfully()) {
            System.out.println("Unable to set up connection to the database.");
            return;
        }

        userTimeOutMap = new HashMap<>();

        String helpMenu = "Use prefix: kc\n" +
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

        String prefix = "kc ";

        FileReader fileReader;
        try {
            fileReader = new FileReader(APIKEY_FILE);
        } catch (FileNotFoundException e) {
            System.out.println("File: " + APIKEY_FILE + " not found. Please make the file and type the discord bot's API key in it.");
            return;
        }
        BufferedReader reader = new BufferedReader(fileReader);
        String apiKey;
        try {
            apiKey = reader.readLine();
        } catch (IOException e) {
            System.out.println("An IO exception occurred while reading file: " + APIKEY_FILE);
            return;
        }

        GatewayDiscordClient client = DiscordClientBuilder.create(apiKey)
                .build().login().block();

        client.getEventDispatcher().on(ReadyEvent.class)
                .subscribe(event -> {
                    User self = event.getSelf();
                    System.out.printf(
                            "Logged in as %s#%s%n", self.getUsername(), self.getDiscriminator()
                    );
                    client.updatePresence(Presence.online(Activity.watching(prefix + "help"))).block();
                });

        commands = new HashMap<>();

        commands.put("help", (event, argv, argvStr) -> {
            if (argv.length == 0) {
                createMessageWithEmbed(event.getMessage().getChannel().block(), "Help Menu", helpMenu, Color.DISCORD_BLACK);
            } else {
                String helpMessage = helpCommandsMap.get(argv[0]);
                if (helpMessage == null) {
                    createMessageWithEmbed(event.getMessage().getChannel().block(), "Help Menu", "Not a valid command", Color.DISCORD_BLACK);
                } else {
                    createMessageWithEmbed(event.getMessage().getChannel().block(), "Help Menu", helpMessage, Color.DISCORD_BLACK);
                }
            }
        });

        commands.put("ping", (event, argv, argvStr) -> event.getMessage()
                .getChannel().block()
                .createMessage("Pong!").block());

        commands.put("ask", (event, argv, argvStr) -> {
             if (argvStr.length() == 0) {
                 createMessageWithEmbed(event.getMessage().getChannel().block(), "Ask kushalCord", "You didn't ask a question, genius.", Color.CYAN);
                 return;
             }
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
             String answer = answers[(int)(Math.random() * answers.length)];
             createMessageWithEmbed(event.getMessage().getChannel().block(), "Ask kushalCord", answer, Color.CYAN);
        });

        commands.put("say", (event, argv, argvStr) -> {
            Message msg = event.getMessage();
            if (argv.length == 0) {
                msg.getChannel().block().createMessage("I can't just say nothing. What are you trying to do?").block();
            } else {
                msg.getChannel().block().createMessage(argvStr).block();
                msg.delete().block();
            }
        });

        commands.put("spam", (event, argv, argvStr) -> {
            MessageChannel channel = event.getMessage().getChannel().block();
            User spammer = event.getMessage().getAuthor().get();
            try {
                if (userTimeOutMap.get(spammer) == null || userTimeOutMap.get(spammer).get() == 0) {
                    int times = Integer.parseInt(argv[0]);
                    if (times > 20) {
                        createMessageWithEmbed(channel, "Spammer", "Woah! Hold on there! You don't wanna spam **too** much now do you?", Color.PINK);
                        return;
                    }
                    String toSpam = argvStr.substring(argv[0].length());
                    if (argv.length == 1) {
                        createMessageWithEmbed(channel, "Spammer", "You didn't tell me what to spam", Color.PINK);
                        return;
                    }
                    for (int i = 0; i < times; i++) {
                        channel.createMessage(toSpam).block();
                    }
                    userTimeOutMap.put(spammer, new AtomicLong((long)(Math.random() * (SPAM_TIME_OUT_MAX - SPAM_TIME_OUT_MIN) + SPAM_TIME_OUT_MIN)));
                } else {
                    createMessageWithEmbed(channel, "Spammer", "Stop spamming so much. You'll annoy everyone!", Color.PINK);
                }
            } catch (NumberFormatException e) {
                int times = 5;
                if (userTimeOutMap.get(spammer) == null || userTimeOutMap.get(spammer).get() == 0) {
                    String toSpam = argvStr;
                    if (argv.length == 0) {
                        createMessageWithEmbed(channel, "Spammer", "You didn't tell me what to spam", Color.PINK);
                        return;
                    }
                    for (int i = 0; i < times; i++) {
                        channel.createMessage(toSpam).block();
                    }
                    userTimeOutMap.put(spammer, new AtomicLong((long)(Math.random() * (SPAM_TIME_OUT_MAX - SPAM_TIME_OUT_MIN) + SPAM_TIME_OUT_MIN)));
                } else {
                    createMessageWithEmbed(channel, "Spammer", "Stop spamming so much. You'll annoy everyone!", Color.PINK);
                }
            } catch (ArrayIndexOutOfBoundsException e) {
                createMessageWithEmbed(channel, "Spammer", "Invalid arguments", Color.PINK);
            }
        });

        commands.put("delete", (event, argv, argvStr) -> {
            TextChannel channel = (TextChannel) event.getMessage().getChannel().block();
            try {
                int n = Integer.parseInt(argv[0]);
                if (n < 1) {
                    createMessageWithEmbed(channel, "Deleter", "Bruh, lol what is your problem?", Color.BROWN);
                    return;
                }
                channel.bulkDelete(channel.getMessagesBefore(channel.getLastMessageId().get())
                        .take(n)
                        .map(message -> message.getId())
                ).blockLast();
                event.getMessage().delete().block();
                if (n == 1) {
                    createMessageWithEmbed(channel, "Deleter", "1 message deleted", Color.BROWN);
                } else {
                    createMessageWithEmbed(channel, "Deleter", n + " messages deleted", Color.BROWN);
                }
            } catch (NumberFormatException e) {
                createMessageWithEmbed(channel, "Deleter", "Bruh, thats not a valid number", Color.BROWN);
            }
        });

        commands.put("bal", (event, argv, argvStr) -> {
            Set<Snowflake> userMentionSet = event.getMessage().getUserMentionIds();
            if (userMentionSet.size() == 0) {
                //if no one was mentioned
                User user = event.getMessage().getAuthor().get();
                try {
                    long bal = psqlManager.getBalance(user.getId().asString());
                    if (!psqlManager.isOperationSuccessful()) {
                        //if the user does not exist already
                        //then we must create the user
                        psqlManager.addAccount(user.getId().asString());
                    }
                    createMessageWithEmbed(event.getMessage().getChannel().block(), "Bank balance", "Your bank balance: " + bal, Color.GRAY_CHATEAU);
                } catch (SQLException throwables) {
                    throwables.printStackTrace();
                    return;
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
                        createMessageWithEmbed(event.getMessage().getChannel().block(), "Bank balance", currentUser.getUsername() + "'s bank balance: " + bal, Color.GRAY_CHATEAU);
                    } catch (SQLException e) {
                        e.printStackTrace();
                    }
                }
            }
        });

        commands.put("rank", (event, argv, argvStr) -> {
            Set<Snowflake> userMentionSet = event.getMessage().getUserMentionIds();
            if (userMentionSet.size() == 0) {
                //if no one was mentioned
                String guildId = event.getMessage().getGuildId().get().asString();
                String userId = event.getMessage().getAuthor().get().getId().asString();
                try {
                    long xp = psqlManager.getXpOfUser(userId, guildId);
                    long level = calculateLevel(xp);
                    long nextLvlXp = calculateXpForLvl(level + 1);
                    createMessageWithEmbed(event.getMessage().getChannel().block(), "Xp Rank", "Ur xp: " + xp + "/" + nextLvlXp +
                            "\n" + "Ur lvl: " + level +
                            "\n" + "Ur rank: " + (psqlManager.getRankOfUser(userId, guildId) + 1) , Color.MAGENTA);
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
                        createMessageWithEmbed(event.getMessage().getChannel().block(), "Xp Rank",
                                "Rank of: " + client.getUserById(userIdSnowflake).block().getUsername() +
                                "\n" + "Ur xp: " + xp + "/" + nextLvlXp +
                                "\n" + "Ur lvl: " + level +
                                "\n" + "Ur rank: " + (psqlManager.getRankOfUser(userId, guildId) + 1)  , Color.MAGENTA);
                    } catch (SQLException e) {
                        e.printStackTrace();
                        return;
                    }
                }
            }
        });

        commands.put("toprankers", (event, argv, argvStr) -> {
            int n = 0;
            try {
                n = Integer.parseInt(argv[0]);
            } catch (NumberFormatException e) {
                createMessageWithEmbed(event.getMessage().getChannel().block(), "Xp Top Rankers", "Invalid number, bruh", Color.MOON_YELLOW);
                return;
            } catch (IndexOutOfBoundsException e) {
                n = 10;
            }
            if (n > MAX_RANKS) {
                //if max ranks have reached.
                createMessageWithEmbed(event.getMessage().getChannel().block(), "Xp Top Rankers", "I cant print that many ranks", Color.MOON_YELLOW);
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
            createMessageWithEmbed(event.getMessage().getChannel().block(), "Xp Top Rankers", "Top " + n + " ranks:\n" + builder.toString(), Color.MOON_YELLOW);
        });

        commands.put("daily", (event, argv, argvStr) -> {
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
                    createMessageWithEmbed(event.getMessage().getChannel().block(), "Get Daily Allowance", "Added " + DAILY_ALLOWANCE + " kc coins to your account", Color.DEEP_LILAC);
                    psqlManager.setDailyTaken(user.getId().asString(), System.currentTimeMillis());
                } else {
                    createMessageWithEmbed(event.getMessage().getChannel().block(), "Get Daily Allowance", "You've already received your daily kc coins allowance. Wait another 24 hours for more coins!", Color.DEEP_LILAC);
                }
            } catch (SQLException throwables) {
                throwables.printStackTrace();
            }
        });

        commands.put("transfer", (event, argv, argvStr) -> {
            long toTransfer = 0;
            try {
                toTransfer = Long.parseLong(argv[0]);
                if (toTransfer < 1) {
                    createMessageWithEmbed(event.getMessage().getChannel().block(), "Bank Transfer Request", "Bruh, what are you trying to do?", Color.DARK_GOLDENROD);
                    return;
                }
            } catch (NumberFormatException e) {
                createMessageWithEmbed(event.getMessage().getChannel().block(), "Bank Transfer Request", "Yo, I can't transfer " + argv[0] + " coins. What are you trying to do?", Color.DARK_GOLDENROD);
                return;
            } catch (ArrayIndexOutOfBoundsException e) {
                createMessageWithEmbed(event.getMessage().getChannel().block(), "Bank Transfer Request", "Invalid usage syntax", Color.DARK_GOLDENROD);
                return;
            }
            User user = event.getMessage().getAuthor().get();
            try {
                if (toTransfer > psqlManager.getBalance(user.getId().asString())) {
                    createMessageWithEmbed(event.getMessage().getChannel().block(), "Bank Transfer Request", "You cant transfer more money than you own!", Color.DARK_GOLDENROD);
                    return;
                }
            } catch (SQLException e) {
                e.printStackTrace();
            }
            Set<Snowflake> mentionIdSet = event.getMessage().getUserMentionIds();
            if (mentionIdSet.size() == 0) {
                createMessageWithEmbed(event.getMessage().getChannel().block(), "Bank Transfer Request", "You haven't told me who to send the money to!", Color.DARK_GOLDENROD);
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
            createMessageWithEmbed(event.getMessage().getChannel().block(), "Bank Transfer Request", "Sucessfully transferred " + toTransfer, Color.DARK_GOLDENROD);
        });

        commands.put("work", (event, argv, argvStr) -> {
            if (userWorkTimeOutMap.get(event.getMessage().getAuthor().get()) == null
                    || userWorkTimeOutMap.get(event.getMessage().getAuthor().get()).get() == 0) {
                userWorkTimeOutMap.put(event.getMessage().getAuthor().get(), new AtomicLong(USER_WORK_TIME_OUT));
            } else {
                createMessageWithEmbed(event.getMessage().getChannel().block(), "Work Application", "You've already finished your one-hour shift. Take a break!", Color.DISCORD_WHITE);
                return;
            }
            try {
                String jobName = argv[0];
                Job job = new Job(event, jobName);
                currentJobs.add(job);
            } catch (ArrayIndexOutOfBoundsException e) {
                userWorkTimeOutMap.put(event.getMessage().getAuthor().get(), new AtomicLong(0));
                createMessageWithEmbed(event.getMessage().getChannel().block(), "Work Application", "You haven't told me what job you want to do!", Color.DISCORD_WHITE);
            } catch (InvalidJobException e) {
                userWorkTimeOutMap.put(event.getMessage().getAuthor().get(), new AtomicLong(0));
                createMessageWithEmbed(event.getMessage().getChannel().block(), "Work Application", "Ain't nobody got no time for that kind of a job!", Color.DISCORD_WHITE);
            }
        });

        commands.put("bj", (event, argv, argvStr) -> {
            MessageChannel messageChannel = event.getMessage().getChannel().block();
            User user = event.getMessage().getAuthor().get();
            if (argv.length == 0) {
                createMessageWithEmbed(event.getMessage().getChannel().block(), "The BlackJack Casino", "U havent told me how much money u wanna gamble", Color.DEEP_SEA);
                return;
            }
            long toGamble;
            try {
                toGamble = Long.parseLong(argv[0]);
            } catch (NumberFormatException e) {
                createMessageWithEmbed(event.getMessage().getChannel().block(), "The BlackJack Casino", "Not a valid number!", Color.DEEP_SEA);
                return;
            }
            if (toGamble < 1) {
                createMessageWithEmbed(messageChannel, "The BlackJack Casino", "What are you trying to do? lol", Color.DEEP_SEA);
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
                createMessageWithEmbed(event.getMessage().getChannel().block(), "The BlackJack Casino", "You cant gamble more than you own!", Color.DEEP_SEA);
                return;
            }
            BJGame bjGame = new BJGame(event, toGamble);
            BJGamesList.add(bjGame);
            createMessageWithEmbed(messageChannel, "The BlackJack Casino", "Ur cards: " + bjGame.getUserCards() +
                    "\nh=hit, s=stand, e=end", Color.DEEP_SEA);
        });
		
		TimerTask reduceTimeOutTask = new TimerTask() {
            @Override
            public void run() {
                for (Map.Entry<User, AtomicLong> entry : userTimeOutMap.entrySet()) {
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
                for (Job job : currentJobs) {
                    if (job.isTimeOver()) {
                        //if times up
                        createMessageWithEmbed(job.getWorkingChannel(), "Work Application", "Terrible job " +
                                job.getWorkingUser().getUsername() + ". You are too slow.\n" +
                                "I will not be paying you anything for that.", Color.DISCORD_WHITE);
                        currentJobs.remove(job);
                    }
                }
            }
        };
        timer.scheduleAtFixedRate(checkJobsTimeOut, 0, 1000);

        TimerTask reduceWorkTimeOut = new TimerTask() {
            @Override
            public void run() {
                for (Map.Entry<User, AtomicLong> entry : userWorkTimeOutMap.entrySet()) {
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
                        rankMessage(event);

                        //for working
                        for (Job job : currentJobs) {
                            //for each job in currentJobs
                            if (job.checkChannel(event) && job.checkUser(event)) {
                                if (job.checkJob(event)) {
                                    createMessageWithEmbed(event.getMessage().getChannel().block(), "Work Application",
                                            "Great job "
                                                    + event.getMessage().getAuthor().get().getUsername()
                                                    + "! You've earned yourself " + job.getWage(), Color.DISCORD_WHITE);
                                    try {
                                        psqlManager.deposit(event.getMessage().getAuthor().get().getId().asString(), job.getWage());
                                    } catch (SQLException ignored) {
                                    }
                                } else {
                                    createMessageWithEmbed(event.getMessage().getChannel().block(), "Work Application",
                                            "Terrible job " +
                                                    event.getMessage().getAuthor().get().getUsername() + ". This is not expected from you.\n" +
                                                    "I will not be paying you anything for that.", Color.DISCORD_WHITE);
                                }
                                currentJobs.remove(job);
                                return;
                            }
                        }
                        //end of for working

                        //for blackjack
                        for (BJGame bjGame : BJGamesList) {
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
                                    createMessageWithEmbed(messageChannel, "The BlackJack Casino", "Blackjack game ended. You wont win or lose anything.\n" +
                                            "But heres your cards: " + bjGame.getUserCards() +
                                            "\nAnd here is the dealers cards: " + bjGame.getDealerCards(), Color.DEEP_SEA);
                                    BJGamesList.remove(bjGame);
                                    return;
                                } else {
                                    createMessageWithEmbed(messageChannel, "The BlackJack Casino", "Invalid command\nUr cards: " + bjGame.getUserCards() +
                                            "\nh=hit, s=stand, e=end", Color.DEEP_SEA);
                                    return;
                                }

                                if (result == BJGame.STATUS_LOSE) {
                                    createMessageWithEmbed(messageChannel, "The BlackJack Casino", "Ur cards: " + bjGame.getUserCards() +
                                            "\nDealers cards: " + bjGame.getDealerCards() +
                                            "\nYou lose! Say bye to " + bjGame.getToGamble(), Color.DEEP_SEA);
                                    try {
                                        psqlManager.withdraw(user.getId().asString(), bjGame.getToGamble());
                                    } catch (SQLException e) {
                                        e.printStackTrace();
                                    }
                                    BJGamesList.remove(bjGame);
                                } else if (result == BJGame.STATUS_WIN) {
                                    createMessageWithEmbed(messageChannel, "The BlackJack Casino", "Ur cards: " + bjGame.getUserCards() +
                                            "\nDealers cards: " + bjGame.getDealerCards() +
                                            "\nYou won! You win " + bjGame.getToGamble(), Color.DEEP_SEA);
                                    try {
                                        psqlManager.deposit(user.getId().asString(), bjGame.getToGamble());
                                    } catch (SQLException e) {
                                        e.printStackTrace();
                                    }
                                    BJGamesList.remove(bjGame);
                                } else if (result == BJGame.STATUS_TIE) {
                                    createMessageWithEmbed(messageChannel, "The BlackJack Casino", "Ur cards: " + bjGame.getUserCards() +
                                            "\nDealers cards: " + bjGame.getDealerCards() +
                                            "\nIts a tie! You dont lose or win any money", Color.DEEP_SEA);
                                    BJGamesList.remove(bjGame);
                                } else {
                                    createMessageWithEmbed(messageChannel, "The BlackJack Casino", "Ur cards: " + bjGame.getUserCards() +
                                            "\nh=hit, s=stand, e=end", Color.DEEP_SEA);
                                }
                                return;
                            }
                        }
                        //end of blackjack

                        if (content.startsWith(prefix)) {
                            String command = content.substring(prefix.length());
                            command = command.split(" ")[0];
                            for (Map.Entry<String, Command> entry : commands.entrySet()) {
                                if (entry.getKey().equals(command)) {
                                    //this is for valid commands
                                    String arguments = content.substring(prefix.length());
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
                            createMessageWithEmbed(event.getMessage().getChannel().block(), "Invalid Command", "Invalid command: " + command, Color.CINNABAR);
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

    public static void rankMessage(MessageCreateEvent event) {
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

    public static void createMessageWithEmbed(MessageChannel channel, String title, String txt, Color color) {
        channel.createEmbed((embed) -> {
            embed.setColor(color);
            embed.setDescription(txt);
            embed.setTitle(title);
        }).block();
    }
}
