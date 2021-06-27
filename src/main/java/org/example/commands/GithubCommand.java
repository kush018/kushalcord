package org.example.commands;

import discord4j.core.event.domain.message.MessageCreateEvent;
import discord4j.rest.util.Color;
import org.example.Command;
import org.example.Main;

public class GithubCommand implements Command {

    public String githubRepoAddr;

    public GithubCommand() {
        githubRepoAddr = "";
        githubRepoAddr = Main.confParser.getConfMap().get("gh_repo");
        if (githubRepoAddr == null) {
            System.out.println("Unable to find attribute gh_repo in config file.");
            System.out.println("Bot invite link will not be shown");
            githubRepoAddr = "";
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
