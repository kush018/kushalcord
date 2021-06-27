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

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.SQLException;
import java.util.*;

import static org.example.Utils.*;

public class Main {
    /**
     * Path to the file containing the API key for the discord bot, relative to the project root folder.
     */
    public static final String CONFIG_FILE_PATH = "config";
    /**
     * Prefix for the bot commands
     */
    public static final String COMMAND_PREFIX = "kc ";

    /**
     * Status of the bot right after login.
     */
    public static final String INITIAL_BOT_STATUS = COMMAND_PREFIX + "help";

    /**
     * This is the DBManager object.
     */
    public static DBManager dbManager;

    public static GatewayDiscordClient client;

    public static HashMap<String, Command> commandsMap;

    public static Timer timer;

    public static ConfParser confParser;

    public static void main(String[] args) {

        /* This part initilises objects */
        timer = new Timer();

        try {
            confParser = new ConfParser(Files.readString(Path.of(CONFIG_FILE_PATH)));
        } catch (IOException e) {
            System.out.println("IOException occured when reading config file");
            return;
        }

        commandsMap = new HashMap<>();

        commandsMap.put("ask", new AskCommand());
        commandsMap.put("bal", new BalanceCommand());
        commandsMap.put("bj", new BlackJackCommand());
        commandsMap.put("daily", new DailyCommand());
        //commandsMap.put("delete", new DeleteCommand());
        commandsMap.put("ping", new PingCommand());
        commandsMap.put("rank", new RankCommand());
        commandsMap.put("say", new SayCommand());
        //commandsMap.put("spam", new SpamCommand());
        commandsMap.put("toprankers", new TopRankersCommand());
        commandsMap.put("transfer", new TransferCommand());
        commandsMap.put("work", new WorkCommand());
        GithubCommand ghCommand = new GithubCommand();
        InviteCommand invCommand = new InviteCommand();
        if (!ghCommand.githubRepoAddr.trim().equals("")) {
            commandsMap.put("github", new GithubCommand());
        }
        if (!invCommand.botInviteLink.trim().equals("")) {
            commandsMap.put("invite", new InviteCommand());
        }

        commandsMap.put("help", new HelpCommand());

        /* Initialises the DBManager object */
        try {
            dbManager = new DBManager();
        } catch (SQLException e) {
            //on creation of the DBManager object, the constructor may throw some exceptions which are caught here
            //on catching these exceptions, the program simply prints the stacktrace to stdout and end the program as the bot cannot function properly without a database
            e.printStackTrace();
            return;
        }

        if (!dbManager.isDbManagerCreatedSuccessfully()) {
            //this happens when the "address", username or password have not been configured correctly, or do not exist
            //in which case the connection to the database cannot be set up and the program terminates
            System.out.println("Unable to set up connection to the database.");
            return;
        }

        dbManager.createBankTable();

        String apiKey = confParser.getConfMap().get("api_key");
        if (apiKey == null) {
            System.out.println("Unable to find attribute api_key in config file");
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
                            dbManager.removeUserFromGuildXpSystem(userId, guildId);
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
                                dbManager.removeGuildFromXpSystem(guildId);
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
