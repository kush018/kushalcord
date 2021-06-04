package org.example;

import discord4j.core.event.domain.message.MessageCreateEvent;

/**
 * An interface representing a command (a task for the bot which can be run)
 * 
 * @author Kushal Galrani
 */
public interface Command {
    /**
     * The function is executed when the command is to be run
     * @param event - The MessageCreateEvent which called this command
     * @param args - The arguments to this command eg: "arg1", "arg2"
     * @param argumentsString - All the arguments to this command as a String eg: "arg1 arg2"
     */
    void execute(MessageCreateEvent event, String[] args, String argumentsString);
}
