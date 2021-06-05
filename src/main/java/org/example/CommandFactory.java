package org.example;

import org.example.commands.*;

import java.util.HashMap;

public class CommandFactory {
    private HashMap<String, Command> commandsMap;

    public CommandFactory() {
        commandsMap = new HashMap<>();

        commandsMap.put("ask", new AskCommand());
        commandsMap.put("bal", new BalanceCommand());
        commandsMap.put("bj", new BlackJackCommand());
        commandsMap.put("daily", new DailyCommand());
        commandsMap.put("delete", new DeleteCommand());
        commandsMap.put("help", new HelpCommand());
        commandsMap.put("ping", new PingCommand());
        commandsMap.put("rank", new RankCommand());
        commandsMap.put("say", new SayCommand());
        commandsMap.put("spam", new SpamCommand());
        commandsMap.put("toprankers", new TopRankersCommand());
        commandsMap.put("transfer", new TransferCommand());
        commandsMap.put("work", new WorkCommand());
    }

    public Command getCommandByName(String commandName) {
        return commandsMap.get(commandName);
    }
}
