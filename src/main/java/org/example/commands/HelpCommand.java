package org.example.commands;

import discord4j.core.event.domain.message.MessageCreateEvent;
import discord4j.rest.util.Color;
import org.example.Command;

import static org.example.Main.helpCommandsMap;
import static org.example.Main.helpMenu;

public class HelpCommand implements Command {

    @Override
    public void execute(MessageCreateEvent event, String[] argv, String argvStr) {
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
    }
}
