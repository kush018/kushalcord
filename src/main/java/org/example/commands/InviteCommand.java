package org.example.commands;

import discord4j.core.event.domain.message.MessageCreateEvent;
import discord4j.rest.util.Color;
import org.example.Command;
import org.example.Main;

public class InviteCommand implements Command {
    public String botInviteLink;

    public InviteCommand() {
        botInviteLink = "";
        botInviteLink = Main.confParser.getConfMap().get("bot_inv");
        if (botInviteLink == null) {
            System.out.println("Unable to find attribute bot_inv in config file.");
            System.out.println("Bot invite link will not be shown");
            botInviteLink = "";
        }
    }

    @Override
    public void execute(MessageCreateEvent event, String[] argv, String argvStr) {
        event.getMessage().getChannel().block().createEmbed((embed) -> {
            embed.setColor(Color.HOKI);
            embed.setDescription("Link: " + botInviteLink);
            embed.setTitle("Bot Invite link");
        }).block();
    }

    @Override
    public String getHelpString() {
        return "invite - get the link to invite this bot to other servers :)";
    }
}
