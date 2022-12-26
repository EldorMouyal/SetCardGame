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
    private  long roundStartTime;
    private  long roundCurrentTime;
    private  final LinkedList<int[]> cardsToRemove;

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
            updateTimerDisplay(true);
            timerLoop();
            removeAllCardsFromTable();
        }
        //if the game is finished then we announce winners.
        announceWinners();
        env.logger.info("Thread " + Thread.currentThread().getName() + " terminated.");
        terminate();
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
        for (Player p:players
             ) {p.terminate();
        }
            this.terminate = true;
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
            if (!cardsToRemove.isEmpty()) {
                synchronized (table) {//avoiding letting a player to place tokens while no cards on table
                    int[] ToRemove = cardsToRemove.removeFirst();//making an array of the first set that need to get removed
                    int[] slots = new int[3];
                    for (int i = 0; i < 3; i++) {//removing the set and its tokens
                        slots[i] = table.cardToSlot[ToRemove[i]];
                        for (Player p : players) {
                            table.removeCard(slots[i]);
                            table.removeToken(p.id, slots[i]);
                            p.decreaseToken();
                        }
                    }
                }
                updateTimerDisplay(true);
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
        if(reset){
            roundStartTime = System.currentTimeMillis();
            reshuffleTime = roundStartTime + env.config.turnTimeoutMillis + 900 ;
        }
        roundCurrentTime = (reshuffleTime - System.currentTimeMillis());
        if(roundCurrentTime <= 0){
            roundCurrentTime = 0;
        }
        env.ui.setCountdown(roundCurrentTime, roundCurrentTime <= env.config.turnTimeoutWarningMillis);
    }

    /**
     * Returns all the cards from the table to the deck.
     */
    private void removeAllCardsFromTable() {
        // TODO implement
        synchronized (table) {
            for (int i = 0; i < env.config.tableSize; i++) {
                deck.add(table.slotToCard[i]);
                env.ui.removeCard(i);
                table.slotToCard[i] = null;
                table.cardToSlot[deck.get(0)] = null;
            }
            for (Player p:players) {
                p.removeTokens();
            }
            table.removeAllTokens();
        }
    }

    /**
     * Check who is/are the winner/s and displays them.
     */
    private synchronized void announceWinners() {
        // TODO implement
        int[] playerIds= new int[players.length];
        int i=0;
        for(Player p:players)
        {
            playerIds[i]=p.id;
        }
        env.ui.announceWinner(playerIds);
    }
    public boolean CheckPlayerSet(int playerId)//this methods checks if a player has a set and calls the appropriate freeze methode
    {
        int[] PlayerCards= table.GetPlayerCards(playerId);
        boolean isSet =  false;
        boolean isSetBefore =  env.util.testSet(PlayerCards);
        boolean isOnRemoveList=false;

        synchronized (cardsToRemove){
            synchronized (table){
                for(int[] c: cardsToRemove) {
                    for(int i=0;i<c.length;i++) {
                        if(c[i]==PlayerCards[0] || c[i]==PlayerCards[1] || c[i]==PlayerCards[2])
                            isOnRemoveList = true;
                    }
                }
            }
                if (!isOnRemoveList){
                   isSet= env.util.testSet(PlayerCards);
                    if (isSet)
                        cardsToRemove.add(PlayerCards);
                }
        }
        if (isSet)
            FreezePlayerForPoint(playerId);
        else if(!isSetBefore){
            FreezePlayerForPenalty(playerId);
        }
        return isSet;
    }

    private void FreezePlayerForPenalty(int playerId)
    {
        try {
            long d = env.config.penaltyFreezeMillis ;
            while (d > 0){
                env.ui.setFreeze(playerId,d);
                Thread.sleep(1000);//@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@
                d = d - 1000;
            }
            env.ui.setFreeze(playerId,d);
        } catch (InterruptedException e) {}
    }
    private void FreezePlayerForPoint(int playerId)
    {
        try {
            long d = env.config.pointFreezeMillis ;
            while (d > 0){
                env.ui.setFreeze(playerId,d);
                Thread.sleep(1000);//@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@
                d = d - 1000;
            }
            env.ui.setFreeze(playerId,d);
        } catch (InterruptedException e) {}
    }
}
