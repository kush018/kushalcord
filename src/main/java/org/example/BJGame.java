package org.example;

import discord4j.core.event.domain.message.MessageCreateEvent;
import discord4j.core.object.entity.User;
import discord4j.core.object.entity.channel.MessageChannel;

public class BJGame {

    public static final int STATUS_NORMAL = 0;
    public static final int STATUS_WIN = 1;
    public static final int STATUS_LOSE = -1;
    public static final int STATUS_TIE = -2;

    private User gamblingUser;
    private MessageChannel gamblingChannel;
    private int userCards, dealerCards;
    private int gameStatus;

    private long toGamble;

    private int getRandomCard() {
        return (int) (Math.random() * 10 + 1);
    }

    public BJGame(MessageCreateEvent event, long toGamble) {
        this.gamblingUser = event.getMessage().getAuthor().get();
        this.gamblingChannel = event.getMessage().getChannel().block();
        this.userCards = getRandomCard() + getRandomCard();
        this.dealerCards = getRandomCard() + getRandomCard();
        this.gameStatus = STATUS_NORMAL;
        this.toGamble = toGamble;
    }

    public void hit() {
        this.userCards += getRandomCard();
        this.dealerCards += getRandomCard();
        if (userCards == 21) {
            if (dealerCards == 21) {
                gameStatus = STATUS_TIE;
            } else {
                gameStatus = STATUS_WIN;
            }
        } else if (dealerCards == 21) {
            gameStatus = STATUS_LOSE;
        } else if (userCards > 21) {
            if (dealerCards > 21) {
                gameStatus = STATUS_TIE;
            } else {
                gameStatus = STATUS_LOSE;
            }
        }
    }

    public void stand() {
        if (dealerCards > 21) {
            if (userCards > 21) {
                gameStatus = STATUS_TIE;
            } else {
                gameStatus = STATUS_WIN;
            }
        } else if (userCards > dealerCards) {
            gameStatus = STATUS_WIN;
        } else if (dealerCards > userCards) {
            gameStatus = STATUS_LOSE;
        } else {
            gameStatus = STATUS_TIE;
        }
    }

    public boolean checkChannel(MessageCreateEvent event) {
        return gamblingChannel.equals(event.getMessage().getChannel().block());
    }

    public boolean checkUser(MessageCreateEvent event) {
        return gamblingUser.equals(event.getMessage().getAuthor().get());
    }

    public int getUserCards() {
        return userCards;
    }

    public int getDealerCards() {
        return dealerCards;
    }

    public int getGameStatus() {
        return gameStatus;
    }

    public long getToGamble() {
        return toGamble;
    }
}
