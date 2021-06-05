package org.example.commands;

import discord4j.core.event.domain.message.MessageCreateEvent;
import discord4j.core.object.entity.channel.TextChannel;
import discord4j.rest.util.Color;
import org.example.Command;

public class DeleteCommand implements Command {

    @Override
    public void execute(MessageCreateEvent event, String[] argv, String argvStr) {
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
    }
}
