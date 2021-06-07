package org.example.commands;

import discord4j.core.event.domain.message.MessageCreateEvent;
import discord4j.rest.util.Color;
import org.example.Command;
import org.example.InvalidJobException;
import org.example.Job;

import java.util.concurrent.atomic.AtomicLong;

import static org.example.Main.*;

public class WorkCommand implements Command {

    @Override
    public void execute(MessageCreateEvent event, String[] argv, String argvStr) {
        //from the userWorkCooldownTimerMap we get the associated entry of the number of seconds left in the cooldown with the user who issued the command
        //we check if the cooldown has reached zero, or if its null, it means the user has not worked since last discord bot start
        if (userWorkCooldownTimerMap.get(event.getMessage().getAuthor().get()) == null
                || userWorkCooldownTimerMap.get(event.getMessage().getAuthor().get()).get() == 0) {
            //if cooldown is over, then we set the cooldown to how much time is specified in USER_WORK_TIME_OUT
            //and continue as normal
            userWorkCooldownTimerMap.put(event.getMessage().getAuthor().get(), new AtomicLong(USER_WORK_TIME_OUT));
        } else {
            //if the cooldown is not over, then we just end the function because the user cannot work until the cooldown gets over
            event.getMessage().getChannel().block().createEmbed((embed) -> {
                embed.setColor(Color.DISCORD_WHITE);
                embed.setDescription("You've already finished your one-hour shift. Take a break!");
                embed.setTitle("Work Application");
            }).block();
            return;
        }
        try {
            //the job which the user wants to work
            String jobName = argv[0];
            //we create a new job object to handle the job
            Job job = new Job(event, jobName);
            //we add this job object to the currentWorkingJobsList so that we have a list of currently running jobs
            currentWorkingJobsList.add(job);
        } catch (ArrayIndexOutOfBoundsException e) {
            //if an argument was not mentioned for the job, then it will throw an ArrayIndexOutOfBoundsException
            userWorkCooldownTimerMap.put(event.getMessage().getAuthor().get(), new AtomicLong(0));
            event.getMessage().getChannel().block().createEmbed((embed) -> {
                embed.setColor(Color.DISCORD_WHITE);
                embed.setDescription("You haven't told me what job you want to do!");
                embed.setTitle("Work Application");
            }).block();
        } catch (InvalidJobException e) {
            //if an invalid job was entered
            userWorkCooldownTimerMap.put(event.getMessage().getAuthor().get(), new AtomicLong(0));
            event.getMessage().getChannel().block().createEmbed((embed) -> {
                embed.setColor(Color.DISCORD_WHITE);
                embed.setDescription("Ain't nobody got no time for that kind of a job!");
                embed.setTitle("Work Application");
            }).block();
        }
    }

    @Override
    public String getHelpString() {
        return "work <job> - work for a job\n" +
                "currently available jobs: hacker, paanwala";
    }
}
