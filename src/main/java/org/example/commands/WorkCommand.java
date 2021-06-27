package org.example.commands;

import discord4j.core.event.domain.message.MessageCreateEvent;
import discord4j.core.object.entity.User;
import discord4j.rest.util.Color;
import org.example.Command;
import org.example.InvalidJobException;
import org.example.Job;
import org.example.Main;

import java.util.HashMap;
import java.util.Map;
import java.util.TimerTask;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicLong;

public class WorkCommand implements Command {

    /**
     * One second in milliseconds (used for representing time)
     */
    public static final long ONE_SECOND = 1000;
    /**
     * One minute in milliseconds (used for representing time)
     */
    public static final long ONE_MINUTE = 60 * ONE_SECOND;
    /**
     * One hour in milliseconds (used for representing time)
     */
    public static final long ONE_HOUR = 60 * ONE_MINUTE;
    /**
     * One day in milliseconds (used for representing time)
     */
    public static final long ONE_DAY = 24 * ONE_HOUR;

    /**
     * When a user uses the "work" command, a cooldown is required before he or she can use the command again. This field defines that cooldown (in seconds).
     */
    public static final long USER_WORK_TIME_OUT = ONE_HOUR / 1000;

    /**
     * It is a HashMap which Maps a discord user with his or her work cooldown (time left before cooldown is over). AtomicLong is used to store cooldown to ensure thread-safety. The cooldown value reduces by one every second. When the cooldown is zero the user can work again.
     */
    public static HashMap<User, AtomicLong> userWorkCooldownTimerMap;

    /**
     * This is a list of the currently "running" jobs i.e., the Job objects that represents ongoing jobs - jobs which users are working on. CopyOnWriteArrayList is used instead of plain ArrayList for thread-safety.
     */
    public static CopyOnWriteArrayList<Job> currentWorkingJobsList;

    public WorkCommand() {
        userWorkCooldownTimerMap = new HashMap<>();
        currentWorkingJobsList = new CopyOnWriteArrayList<>();

        //manages the job timeout
        //all users only have a spcecific amount of time for completing a job
        //if they dont complete it in the stipulated time
        //they wont be awarded any money
        TimerTask checkJobsTimeOut = new TimerTask() {
            @Override
            public void run() {
                //for each job in the list of currently running jobs,
                for (Job job : currentWorkingJobsList) {
                    if (job.isTimeOver()) {
                        //if times up, tell the user the times up
                        job.getWorkingChannel().createEmbed((embed) -> {
                            embed.setColor(Color.DISCORD_WHITE);
                            embed.setDescription("Terrible job " +
                                    job.getWorkingUser().getUsername() + ". You are too slow.\n" +
                                    "I will not be paying you anything for that.");
                            embed.setTitle("Work Application");
                        }).block();
                        //remove the job from the list of currently running jobs as the job is not "running" anymore
                        currentWorkingJobsList.remove(job);
                    }
                }
            }
        };
        Main.timer.scheduleAtFixedRate(checkJobsTimeOut, 0, 1000);

        //manages the work cooldown timer for each user
        TimerTask reduceWorkTimeOut = new TimerTask() {
            @Override
            public void run() {
                for (Map.Entry<User, AtomicLong> entry : userWorkCooldownTimerMap.entrySet()) {
                    if (entry.getValue().get() != 0) {
                        entry.getValue().decrementAndGet();
                    }
                }
            }
        };
        Main.timer.scheduleAtFixedRate(reduceWorkTimeOut, 0, 1000);
    }

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
                "currently available jobs: hacker, robber";
    }
}
