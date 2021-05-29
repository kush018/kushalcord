package org.example;

public class InvalidJobException extends Exception {
    private String jobName;

    public InvalidJobException(String jobName) {
        this.jobName = jobName;
    }

    @Override
    public String toString() {
        return "Invalid job name: " + jobName;
    }
}
