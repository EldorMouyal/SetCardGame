package bguspl.set.ex;

import bguspl.set.Env;
import bguspl.set.ThreadLogger;

import java.util.*;
import java.util.logging.Logger;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

/**
 * This class manages the dealer's threads and data
 */
public class Dealer implements Runnable {

    /**
     * The game environment object.
     */
    private final Env env;

    /**
     * Game entities.
     */
    private final Table table;
    private final Player[] players;

    /**
     * The list of card ids that are left in the dealer's deck.
     */
    private final List<Integer> deck;

    /**
     * True iff game should be terminated.
     */
    private volatile boolean terminate;

    /**
     * The time when the dealer needs to reshuffle the deck due to turn timeout.
     */
    private long reshuffleTime = Long.MAX_VALUE;
    private LinkedList<int[]> cardsToRemove;

    public Dealer(Env env, Table table, Player[] players) {
        this.env = env;
        this.table = table;
        this.players = players;
        deck = IntStream.range(0, env.config.deckSize).boxed().collect(Collectors.toList());
        cardsToRemove = new LinkedList<>();
    }

    /**
     * The dealer thread starts here (main loop for the dealer thread).
     */
    @Override
    public void run() {
        env.logger.info("Thread " + Thread.currentThread().getName() + " starting.");
        for (Player p: players) {
            Thread t = new Thread(p);
            t.start();
        }
        while (!shouldFinish()) {

            placeCardsOnTable();
            updateTimerDisplay(false);
            timerLoop();
            removeAllCardsFromTable();

        }


        announceWinners();
        env.logger.info("Thread " + Thread.currentThread().getName() + " terminated.");
    }

    /**
     * The inner loop of the dealer thread that runs as long as the countdown did not time out.
     */
    private void timerLoop() {
        while (!terminate && System.currentTimeMillis() < reshuffleTime) {
            sleepUntilWokenOrTimeout();
            updateTimerDisplay(false);
            removeCardsFromTable();
            placeCardsOnTable();
        }
    }

    /**
     * Called when the game should be terminated.
     */
    public void terminate() {
        // TODO implement
    }

    /**
     * Check if the game should be terminated or the game end conditions are met.
     *
     * @return true iff the game should be finished.
     */
    private boolean shouldFinish() {
        return terminate || env.util.findSets(deck, 1).size() == 0;
    }

    /**
     * Checks cards should be removed from the table and removes them.
     */
    private void removeCardsFromTable() {
        // TODO implement
        while (!terminate)
        {
            if (!cardsToRemove.isEmpty()) {

                synchronized (table) {
                    int[] ToRemove = cardsToRemove.getFirst();
                    cardsToRemove.removeFirst();
                    int[] slots = new int[3];
                    for (int i = 0; i < 3; i++) {
                        slots[i] = table.cardToSlot[ToRemove[i]];
                        for (Player p : players) {
                            table.removeCard(slots[i]);
                            table.removeToken(p.id, slots[i]);
                        }

                    }
                    placeCardsOnTable();

                }
            }
        }
    }

    /**
     * Check if any cards can be removed from the deck and placed on the table.
     */
    private void placeCardsOnTable() {
        // TODO implement
        Collections.shuffle(deck);
        for (int i = 0;i<table.slotToCard.length; i++) {
            if(table.slotToCard[i] == null){
                table.placeCard(deck.remove(0),i);
            }
        }
    }

    /**
     * Sleep for a fixed amount of time or until the thread is awakened for some purpose.
     */
    private void sleepUntilWokenOrTimeout() {
        // TODO implement
        try {
            Thread.sleep(env.config.tableDelayMillis);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Reset and/or update the countdown and the countdown display.
     */
    private void updateTimerDisplay(boolean reset) {
        // TODO implement
        if(reset)
            env.ui.setCountdown(env.config.turnTimeoutMillis,false);
    }

    /**
     * Returns all the cards from the table to the deck.
     */
    private void removeAllCardsFromTable() {
        // TODO implement
    }

    /**
     * Check who is/are the winner/s and displays them.
     */
    private void announceWinners() {
        // TODO implement
    }
    public boolean CheckPlayerSet(int playerId)//this methods cheks if a player has a set and calles the appropriate freeze methode
    {
        int[] PlayerCards= table.GetPlayerCards(playerId);
        boolean isSet =  env.util.testSet(PlayerCards);
        if(isSet){
            FreezePlayerForPoint(playerId);
            boolean isOnRemoveList=false;
            for(int[] c: cardsToRemove) {
                if (c[0] == PlayerCards[0] && c[1] == PlayerCards[1] && c[2] == PlayerCards[2])
                    isOnRemoveList = true;
            }
                if (!isOnRemoveList)
                    {cardsToRemove.add(PlayerCards);}
        }
        else{
              FreezePlayerForPenalty(playerId);
            }
        return isSet;
    }

    public void FreezePlayerForPenalty(int playerId)
    {
        try {
            long d = env.config.penaltyFreezeMillis ;
            while (d > 0){
                env.ui.setFreeze(playerId,d);
                Thread.sleep(1000);
                d = d - 1000;
            }
            env.ui.setFreeze(playerId,d);
        } catch (InterruptedException e) {}
    }
    public void FreezePlayerForPoint(int playerId)
    {
        try {
            long d = env.config.pointFreezeMillis ;
            while (d > 0){
                env.ui.setFreeze(playerId,d);
                Thread.sleep(1000);
                d = d - 1000;
            }
            env.ui.setFreeze(playerId,d);
        } catch (InterruptedException e) {}
    }
}
