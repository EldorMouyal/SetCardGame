package bguspl.set.ex;

import bguspl.set.Env;

import java.util.*;
import java.util.stream.Collectors;

/**
 * This class contains the data that is visible to the player.
 *
 * @inv slotToCard[x] == y iff cardToSlot[y] == x
 */
public class Table {

    /**
     * The game environment object.
     */
    private final Env env;

    /**
     * Mapping between a slot and the card placed in it (null if none).
     */
    protected final Integer[] slotToCard; // card per slot (if any)

    /**
     * Mapping between a card and the slot it is in (null if none).
     */
    protected final Integer[] cardToSlot; // slot per card (if any)
    protected final List<Integer>[] slotToTokens;
    private int setSize;
    /**
     * Constructor for testing.
     *
     * @param env        - the game environment objects.
     * @param slotToCard - mapping between a slot and the card placed in it (null if none).
     * @param cardToSlot - mapping between a card and the slot it is in (null if none).
     */
    public Table(Env env, Integer[] slotToCard, Integer[] cardToSlot) {

        this.env = env;
        this.setSize = env.config.featureSize;
        this.slotToCard = slotToCard;
        this.cardToSlot = cardToSlot;
        slotToTokens = new LinkedList[slotToCard.length];
        for(int i = 0; i< slotToTokens.length;i++){
            slotToTokens[i] = new LinkedList<>();
        }
    }

    /**
     * Constructor for actual usage.
     *
     * @param env - the game environment objects.
     */
    public Table(Env env) {

        this(env, new Integer[env.config.tableSize], new Integer[env.config.deckSize]);
    }

    /**
     * This method prints all possible legal sets of cards that are currently on the table.
     */
    public void hints() {
        List<Integer> deck = Arrays.stream(slotToCard).filter(Objects::nonNull).collect(Collectors.toList());
        env.util.findSets(deck, Integer.MAX_VALUE).forEach(set -> {
            StringBuilder sb = new StringBuilder().append("Hint: Set found: ");
            List<Integer> slots = Arrays.stream(set).mapToObj(card -> cardToSlot[card]).sorted().collect(Collectors.toList());
            int[][] features = env.util.cardsToFeatures(set);
            System.out.println(sb.append("slots: ").append(slots).append(" features: ").append(Arrays.deepToString(features)));
        });
    }

    /**
     * Count the number of cards currently on the table.
     *
     * @return - the number of cards on the table.
     */
    public synchronized int countCards() {
        int cards = 0;
        for (Integer card : slotToCard)
            if (card != null)
                ++cards;
        synchronized (this){notifyAll();}//############################################################
        return cards;
    }

    /**
     * Places a card on the table in a grid slot.
     * @param card - the card id to place in the slot.
     * @param slot - the slot in which the card should be placed.
     *
     * @post - the card placed is on the table, in the assigned slot.
     */
    public synchronized void placeCard(int card, int slot) {
        try {
            Thread.sleep(env.config.tableDelayMillis);
        } catch (InterruptedException ignored) {}
        synchronized (this){
        cardToSlot[card] = slot;
        slotToCard[slot] = card;

        // TODO implement
        env.ui.placeCard(card,slot);
        notifyAll();}//############################################################

    }

    /**
     * Removes a card from a grid slot on the table.
     * @param slot - the slot from which to remove the card.
     */
    public void removeCard(int slot) {
        try {
            Thread.sleep(env.config.tableDelayMillis);
        } catch (InterruptedException ignored) {}

        // TODO implement
        if (slotToCard[slot] != null) {
            int card = slotToCard[slot];
            env.ui.removeCard(slot);
            cardToSlot[card] = null;
            slotToCard[slot] = null;
        }
        synchronized (this){notifyAll();}//############################################################

    }

    /**
     * Places a player token on a grid slot.
     * @param player - the player the token belongs to.
     * @param slot   - the slot on which to place the token.
     */
    public void placeToken(int player, int slot) {
        // TODO implement
        if (slotToCard[slot]!= null) {
            slotToTokens[slot].add(player);
            env.ui.placeToken(player, slot);
        }
    }

    public boolean placeToken(int player,int slot, int card)
    {
        boolean placed = false;
        synchronized (this) {
            if (slotToCard[slot] != null && card == slotToCard[slot]) {
                int counter=0;
                for(int i=0; i<slotToTokens.length & counter<setSize; i++)
                {
                    if (slotToTokens[i].contains(player))
                        counter++;
                }
                if (counter < setSize){
                    placed = true;
                    placeToken(player, slot);}
            }
            notifyAll();//############################################################

        }
        return placed;
    }

    /**
     * Removes a token of a player from a grid slot.
     * @param player - the player the token belongs to.
     * @param slot   - the slot from which to remove the token.
     * @return       - true iff a token was successfully removed.
     */
    public boolean removeToken(int player, int slot) {
        // TODO implement
        synchronized (this){
        for (int i=0; i < slotToTokens[slot].size(); i++)
        {
            if (slotToTokens[slot].get(i)==player) {
                slotToTokens[slot].remove(i);
                env.ui.removeToken(player,slot);
                return true;
            }
        }
        notifyAll();
        }
        return false;
    }
    public synchronized int[] GetPlayerCards(int playerId)
    {
        int[] cards = new int[setSize];
        int cardIndex = 0;
        synchronized (slotToTokens){
        List<Integer>[] copyOfSlotToTokens = slotToTokens.clone();
            for (int i = 0; i < copyOfSlotToTokens.length; i++) {
                for (int j : copyOfSlotToTokens[i]) {
                    if (j == playerId)
                        cards[cardIndex++] = slotToCard[i];
                }
            }

        notifyAll();}//############################################################
        return cards;
    }

    public void removeAllTokens() {
        for(int i = 0; i< slotToTokens.length; i++) {
            slotToTokens[i].clear();
        }
        env.ui.removeTokens();
    }
}

