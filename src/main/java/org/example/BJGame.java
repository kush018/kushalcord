package org.example;

import discord4j.core.event.domain.message.MessageCreateEvent;
import discord4j.core.object.entity.User;
import discord4j.core.object.entity.channel.MessageChannel;

/**
 * Representing a game of blackjack in the kushalCord discord bot
 * 
 * @author Kushal Galrani
 */
public class BJGame {

    /**
     * If nothing special happened
     */
    public static final int STATUS_NORMAL = 0;
    /**
     * The player won
     */
    public static final int STATUS_WIN = 1;
    /**
     * The player lost
     */
    public static final int STATUS_LOSE = -1;
    /**
     * It was a tie
     */
    public static final int STATUS_TIE = -2;

    /**
     * The user who is gambling
     */
    private User gamblingUser;
    /**
     * The MessageChannel in which the user is gambling
     */
    private MessageChannel gamblingChannel;
    /**
     * The "sum" of the cards the user has
     */
    private int userCards;
    /**
     * The "sum" of the cards the dealer has
     */
    private int dealerCards;
    /**
     * The status of the game. It can be any one of these four status indicators: STATUS_NORMAL, STATUS_WIN, STATUS_LOSE and STATUS_TIE
     */
    private int gameStatus;

    /**
     * The money the user wishes to gamble
     */
    private long toGamble;

    /**
     * Gets the value of a random card
     * 
     * @return The value of the random card
     */
    private int getRandomCard() {
        //returns a random value from 1 (ACE) and 10 (KING, QUEEN, JACK)
        return (int) (Math.random() * 10 + 1);
    }

    /**
     * @param event - The MessageCreateEvent which initiated the gambling game
     * @param toGamble - The amount to gamble
     */
    public BJGame(MessageCreateEvent event, long toGamble) {
        //gets the user who sent the message - he or she is the gambling user
        this.gamblingUser = event.getMessage().getAuthor().get();
        //gets the channel in which the message was sent
        this.gamblingChannel = event.getMessage().getChannel().block();
        //gives the user two cards - the sum of cards are stored in the variable userCards
        this.userCards = getRandomCard() + getRandomCard();
        this.dealerCards = getRandomCard() + getRandomCard();
        //initial status
        this.gameStatus = STATUS_NORMAL;
        this.toGamble = toGamble;
    }

    /**
     * The user chooses to "hit" 
     */
    public void hit() {
        //adds a card to the user and dealer
        this.userCards += getRandomCard();
        this.dealerCards += getRandomCard();
        if (userCards == 21) {
            //if the sum of the cards of the user is 21
            if (dealerCards == 21) {
                //if both the dealer and user have 21 cards its a tie
                gameStatus = STATUS_TIE;
            } else {
                //if only the user has 21 cards, the user wins
                gameStatus = STATUS_WIN;
            }
        } else if (dealerCards == 21) {
            //if the dealer has 21 but the user doesnt the user loses
            gameStatus = STATUS_LOSE;
        } else if (userCards > 21) {
            //if the user's card sum is more than 21
            if (dealerCards > 21) {
                //if both have more than 21, its a tie
                gameStatus = STATUS_TIE;
            } else {
                //if only the user has more than 21, the user loses
                gameStatus = STATUS_LOSE;
            }
        }
    }

    /**
     * The user chooses to "stand" 
     */
    public void stand() {
        if (dealerCards > 21) {
            //if the dealer's cards add up to more than 21 cards
            if (userCards > 21) {
                //if both the dealer and the user have more than 21 cards, its a tie
                gameStatus = STATUS_TIE;
            } else {
                //if only the dealer has more than 21 cards, its a win for the user
                gameStatus = STATUS_WIN;
            }
        } else if (userCards > dealerCards) {
            //if the user has more cards than the dealer
            gameStatus = STATUS_WIN;
        } else if (dealerCards > userCards) {
            //if the dealer has more cards than the user
            gameStatus = STATUS_LOSE;
        } else {
            //if both the dealer and the user have the same number of cards
            gameStatus = STATUS_TIE;
        }
    }

    /**
     * Checks if the channel of a MessageCreateEvent and the gamblingChannel are the same
     * 
     * @param event - The MessageCreateEvent
     * @return true if the channel of the MessageCreateEvent and the gamblingChannel are the same
     */
    public boolean checkChannel(MessageCreateEvent event) {
        return gamblingChannel.equals(event.getMessage().getChannel().block());
    }

    /**
     * Checks if the author (the user who created) a MessageCreateEvent and the gamblingUser are the same
     * 
     * @param event - The MessageCreateEvent
     * @return true if the creator of the MessageCreateEvent and the gamblingUser are the same
     */
    public boolean checkUser(MessageCreateEvent event) {
        return gamblingUser.equals(event.getMessage().getAuthor().get());
    }

    /**
     * Returns the sum of the cards of the user
     * 
     * @return the sum of the cards of the user
     */
    public int getUserCards() {
        return userCards;
    }

    /**
     * Returns the sum of the cards of the dealer
     * 
     * @return the sum of the cards of the dealer
     */
    public int getDealerCards() {
        return dealerCards;
    }

    /**
     * Returns the status of the game
     * 
     * The "status" of the game can be either of the four status indicators: STATUS_NORMAL (0), STATUS_WIN (1), STATUS_LOSE (-1) and STATUS_TIE (-2)
     * 
     * @return the status of the game
     */
    public int getGameStatus() {
        return gameStatus;
    }

    /**
     * Returns the amount of the money the user is gambling
     * 
     * @return the amount of the money the user is gambling
     */
    public long getToGamble() {
        return toGamble;
    }
}
