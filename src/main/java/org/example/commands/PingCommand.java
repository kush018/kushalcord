package org.example.commands;

import discord4j.core.event.domain.message.MessageCreateEvent;
import org.example.Command;

public class PingCommand implements Command {

    @Override
    public void execute(MessageCreateEvent event, String[] argv, String argvStr) {
        event.getMessage().getChannel().block()
                .createMessage("Pong!").block();
    }

    @Override
    public String getHelpString() {
        return "ping - causes bot to reply with Pong!\n" +
                "(used to test if bot is running properly or not)";
    }
}
