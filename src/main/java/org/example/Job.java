package org.example;

import discord4j.core.event.domain.message.MessageCreateEvent;
import discord4j.core.object.entity.User;
import discord4j.core.object.entity.channel.MessageChannel;
import discord4j.rest.util.Color;

import java.util.Objects;

public class Job {
    private MessageChannel workingChannel;

    private User workingUser;

    private String answer;

    private String jobType;

    private String userQuery;

    private long timeOfCreation;

    private long jobTimeOut;

    private long wage = 3000;

    private static final long ONE_SECOND = 1000;
    private static final long ONE_MINUTE = 60 * ONE_SECOND;
    private static final long ONE_HOUR = 60 * ONE_MINUTE;
    private static final long ONE_DAY = 24 * ONE_HOUR;

    public Job(MessageCreateEvent event, String jobType) throws InvalidJobException {
        this.workingChannel = event.getMessage().getChannel().block();
        this.workingUser = event.getMessage().getAuthor().get();
        this.jobType = jobType;
        this.timeOfCreation = System.currentTimeMillis();
        setAnswerAndQuery();
        Main.createMessageWithEmbed(event.getMessage().getChannel().block(), "Work Application",
                "Your job description\n" + userQuery, Color.DISCORD_WHITE);
    }

    private void setAnswerAndQuery() throws InvalidJobException {
        if (jobType.equals("hacker")) {
            userQuery = Integer.toString( (int)(Math.random() * (1E8 - 1) + 1), 16);
            answer = userQuery;
            userQuery = "Enter the *secret* code: " + userQuery;
            jobTimeOut = 7 * ONE_SECOND;
        } else if (jobType.equals("paanwala")) {
            String[] userQueries = {"Spits paan on the road",
            "Paan4life",
            "Red teeth",
            "bolo zubaan kesari",
            "vimal"};
            answer = userQueries[(int)(Math.random() * userQueries.length)];
            userQuery = "Type the following: " + answer;
            jobTimeOut = 15 * ONE_SECOND;
        } else {
            throw new InvalidJobException(jobType);
        }
    }

    public String getUserQuery() {
        return this.userQuery;
    }

    public boolean isTimeOver() {
        return !( timeOfCreation + jobTimeOut > System.currentTimeMillis() );
    }

    public boolean checkJob(MessageCreateEvent event) {
        if (event.getMessage().getContent().equalsIgnoreCase(answer)) {
            return true;
        }
        return false;
    }

    public boolean checkChannel(MessageCreateEvent event) {
        return event.getMessage().getChannel().block().equals(workingChannel);
    }

    public boolean checkUser(MessageCreateEvent event) {
        return event.getMessage().getAuthor().get().equals(workingUser);
    }

    public long getWage() {
        return this.wage;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Job job = (Job) o;
        return timeOfCreation == job.timeOfCreation &&
                jobTimeOut == job.jobTimeOut &&
                wage == job.wage &&
                workingChannel.equals(job.workingChannel) &&
                workingUser.equals(job.workingUser) &&
                answer.equals(job.answer) &&
                jobType.equals(job.jobType) &&
                userQuery.equals(job.userQuery);
    }

    @Override
    public int hashCode() {
        return Objects.hash(workingChannel, workingUser, answer, jobType, userQuery, timeOfCreation, jobTimeOut, wage);
    }

    public MessageChannel getWorkingChannel() {
        return this.workingChannel;
    }

    public User getWorkingUser() {
        return this.workingUser;
    }
}
