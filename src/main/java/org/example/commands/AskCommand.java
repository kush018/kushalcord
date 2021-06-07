package org.example.commands;

import org.example.Command;

import discord4j.core.event.domain.message.MessageCreateEvent;
import discord4j.rest.util.Color;

public class AskCommand implements Command {

    @Override
    public void execute(MessageCreateEvent event, String[] argv, String argvStr) {
        if (argvStr.length() == 0) {
            /* If there are no arguments, it means the user didn't ask any question*/
            event.getMessage().getChannel().block().createEmbed((embed) -> {
                embed.setColor(Color.CYAN);
                embed.setDescription("You didn't ask a question, genius");
                embed.setTitle("Ask kushalCord");
            }).block();
            //if the user didnt ask a question, no point in going further so the function can end here.
            return;
        }
        //the code reaches here only if the user asked a question
        //"answers" is a one dimensional array of Strings which contains all the answers to the question that the bot can give
        String[] answers = {"Yes",
               "No",
               "Probably",
               "Probably not",
               "lol idk",
               "maybe ... ? im not sure",
               "I don't want to answer your stupid questions",
               "Definitely",
               "Definitely not",
               "Well yes, but actually no",
               "Well no, but actually yes",
               "It depends",
               "hmmmmm idk"};
        //"answer" is a String that is picked randomly from the answers array
        String answer = answers[(int)(Math.random() * answers.length)];
        //the selected answer is then shown to the user
        event.getMessage().getChannel().block().createEmbed((embed) -> {
           embed.setColor(Color.CYAN);
           embed.setDescription(answer);
           embed.setTitle("Ask kushalCord");
        }).block();
    }

    @Override
    public String getHelpString() {
        return "ask <question> - ask a yes/no type question to the bot to hear the answer\n" +
                "(its not real its just for fun. the bot only gives random answers)";
    }
}
