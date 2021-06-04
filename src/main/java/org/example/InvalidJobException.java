package org.example;

/**
 * Thrown when an invalid job is specified in the Job class
 * 
 * @author Kushal Galrani
 */
public class InvalidJobException extends Exception {
    /**
     * The name of the invalid job
     */
    private String jobName;

    /**
     * @param jobName - The name of the invalid job
     */
    public InvalidJobException(String jobName) {
        this.jobName = jobName;
    }

    @Override
    public String toString() {
        return "Invalid job name: " + jobName;
    }
}
