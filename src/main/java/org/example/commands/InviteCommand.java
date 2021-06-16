package org.example.commands;

import discord4j.core.event.domain.message.MessageCreateEvent;
import discord4j.rest.util.Color;
import org.example.Command;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

public class InviteCommand implements Command {
    String botInviteLink;

    public static final String BOT_INVITE_LINK_FILE = "conf/bot_inv";

    public InviteCommand() {
        botInviteLink = "";
        try {
            botInviteLink = Files.readString(Path.of(BOT_INVITE_LINK_FILE), StandardCharsets.UTF_16);
        } catch (IOException e) {
            System.out.println("There was an IOException while reading file: " + BOT_INVITE_LINK_FILE + ". Thus, the bot invite link cant be displayed");
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
