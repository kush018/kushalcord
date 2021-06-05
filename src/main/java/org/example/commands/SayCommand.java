package org.example.commands;

import discord4j.core.event.domain.message.MessageCreateEvent;
import discord4j.core.object.entity.Message;
import org.example.Command;

public class SayCommand implements Command {

    @Override
    public void execute(MessageCreateEvent event, String[] argv, String argvStr) {
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
    }
}
