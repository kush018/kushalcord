package org.example;

import discord4j.core.event.domain.message.MessageCreateEvent;
import discord4j.core.object.entity.User;
import discord4j.core.object.entity.channel.MessageChannel;
import discord4j.rest.util.Color;

import java.util.Objects;

/**
 * A class representing a Job to be done by a user
 * 
 * @author Kushal Galrani
 */
public class Job {
    /**
     * The channel in which the user is doing his or her job
     */
    private MessageChannel workingChannel;

    /**
     * The user that is doing the job
     */
    private User workingUser;

    /**
     * The content of the message that has to by typed by the user to complete the job
     */
    private String answer;

    /**
     * The name of the job
     */
    private String jobType;

    /**
     * The message shown to the user, telling him or her about what needs to be done in the job
     */
    private String userQuery;

    /**
     * When this job object was created
     */
    private long timeOfCreation;

    /**
     * Time the user is given to finish the job from timeOfCreation
     */
    private long jobTimeOut;

    /**
     * The money given to the user after completing the job successfully in time
     */
    private long wage = 3000;

    /**
     * One second in milliseconds
     */
    private static final long ONE_SECOND = 1000;
    /**
     * One minute in milliseconds
     */
    private static final long ONE_MINUTE = 60 * ONE_SECOND;
    /**
     * One hour in milliseconds
     */
    private static final long ONE_HOUR = 60 * ONE_MINUTE;
    /**
     * One day (24 hours) in milliseconds
     */
    private static final long ONE_DAY = 24 * ONE_HOUR;

    /**
     * @param event - The MessageCreateEvent that creates the job
     * @param jobType - The name of the job
     * @throws InvalidJobException
     */  
    public Job(MessageCreateEvent event, String jobType) throws InvalidJobException {
        //gets the channel of the MessageCreateEvent
        this.workingChannel = event.getMessage().getChannel().block();
        //gets the user of the MessageCreateEvent
        this.workingUser = event.getMessage().getAuthor().get();
        this.jobType = jobType;
        //sets the timeOfCreation to the current system time (as a long which is equal to the milliseconds passed after UNIX EPOCH)
        this.timeOfCreation = System.currentTimeMillis();
        setAnswerAndQuery();
        //creates a message in the workingChannel, telling the user about his or her newly assigned job
        event.getMessage().getChannel().block().createEmbed((embed) -> {
            embed.setColor(Color.DISCORD_WHITE);
            embed.setDescription("Your job description\n" + userQuery);
            embed.setTitle("Work Application");
        }).block();
    }

    /**
     * Sets the values of answer and userQuery
     * 
     * @throws InvalidJobException
     */
    private void setAnswerAndQuery() throws InvalidJobException {
        if (jobType.equals("hacker")) {
            //if the jobType is "hacker"
            //picks a random number between 1 and 10^8 and stores its hexadecimal (base 16) representation in userQuery
            userQuery = Integer.toString( (int)(Math.random() * (1E8 - 1) + 1), 16);
            //sets answer to userQuery
            answer = userQuery;
            userQuery = "Enter the *secret* code: " + userQuery;
            //sets job timeout
            jobTimeOut = 7 * ONE_SECOND;
        } else if (jobType.equals("robber")) {
            String[] userQueries = {"send cash pls",
            "give cash or else",
            "cocks gun",
            "pulls out knife"};
            //answer is a random String picked from the array userQueries
            answer = userQueries[(int)(Math.random() * userQueries.length)];
            userQuery = "Type the following: " + answer;
            jobTimeOut = 15 * ONE_SECOND;
        } else {
            //if jobType is invalid
            throw new InvalidJobException(jobType);
        }
    }

    /**
     * Returns the message shown to the user, telling him or her about what needs to be done in the job
     * 
     * @return the message shown to the user, telling him or her about what needs to be done in the job
     */
    public String getUserQuery() {
        return this.userQuery;
    }

    /**
     * Helps find out if this job is timed out
     * 
     * @return true if this job is timed out
     */
    public boolean isTimeOver() {
        return !( timeOfCreation + jobTimeOut > System.currentTimeMillis() );
    }

    /**
     * If the MessageCreateEvent representing a user's message fulfills the job requirements
     * 
     * @param event - The corresponding MessageCreateEvent
     * @return true if event representing a user's message fulfills the job requirements
     */
    public boolean checkJob(MessageCreateEvent event) {
        if (event.getMessage().getContent().equalsIgnoreCase(answer)) {
            return true;
        }
        return false;
    }

    /**
     * If the channel of a MessageCreateEvent representing a user's message is same as the workingChannel in this Job object
     * 
     * @param event - The MessageCreateEvent
     * @return true if the channel of event representing a user's message is same as the workingChannel in this Job object
     */
    public boolean checkChannel(MessageCreateEvent event) {
        return event.getMessage().getChannel().block().equals(workingChannel);
    }

    /**
     * If the author of a message, is the same as the user who has to do this Job (workingUser)
     * 
     * @param event - The MessageCreateEvent representing the message to be tested
     * @return true if the user of the MessageCreateEvent is the same was the workingUser
     */
    public boolean checkUser(MessageCreateEvent event) {
        return event.getMessage().getAuthor().get().equals(workingUser);
    }

    /**
     * Returns the wage to be earned by the user if the job is completed successfully and in time
     * 
     * @return the wage to be earned by the user if the job is completed successfully and in time
     */
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

    /**
     * Returns the workingChannel
     * 
     * @return the workingChannel
     */
    public MessageChannel getWorkingChannel() {
        return this.workingChannel;
    }

    /**
     * Returns the workingUser
     * 
     * @return the workingUser
     */
    public User getWorkingUser() {
        return this.workingUser;
    }
}
