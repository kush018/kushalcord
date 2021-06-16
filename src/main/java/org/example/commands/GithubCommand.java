package org.example.commands;

import discord4j.core.event.domain.message.MessageCreateEvent;
import discord4j.rest.util.Color;
import org.example.Command;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

public class GithubCommand implements Command {

    String githubRepoAddr;

    public static final String GH_REPO_LINK_FILE = "conf/gh_repo";

    public GithubCommand() {
        githubRepoAddr = "";
        try {
            githubRepoAddr = Files.readString(Path.of(GH_REPO_LINK_FILE), StandardCharsets.UTF_16);
        } catch (IOException e) {
            System.out.println("There was an IOException while reading file: " + GH_REPO_LINK_FILE + ". Thus, the bot invite link cant be displayed");
        }
    }

    @Override
    public void execute(MessageCreateEvent event, String[] argv, String argvStr) {
        event.getMessage().getChannel().block().createEmbed((embed) -> {
            embed.setColor(Color.DEEP_LILAC);
            embed.setDescription("Link: " + githubRepoAddr);
            embed.setTitle("Bot Github repository link");
        }).block();
    }

    @Override
    public String getHelpString() {
        return "github - get the link the the github repository containing the code for this bot";
    }
}
