package bguspl.set.ex;

import bguspl.set.Env;

import java.util.LinkedList;
import java.util.NoSuchElementException;
import java.util.Queue;
import java.util.Random;

/**
 * This class manages the players' threads and data
 *
 * @inv id >= 0
 * @inv score >= 0
 */
public class Player implements Runnable {

    /**
     * The game environment object.
     */
    private final Env env;

    /**
     * Game entities.
     */
    private final Table table;

    /**
     * The id of the player (starting from 0).
     */
    public final int id;

    /**
     * The thread representing the current player.
     */
    private Thread playerThread;

    /**
     * The thread of the AI (computer) player (an additional thread used to generate key presses).
     */
    private Thread aiThread;

    /**
     * True iff the player is human (not a computer player).
     */
    private final boolean human;
    private final Dealer dealer;

    /**
     * True iff game should be terminated.
     */
    private volatile boolean terminate;

    /**
     * The current score of the player.
     */
    private int score;
    private int tokensPlaced;

    private final Queue<Integer> slotsTodo;
    private final Queue<Integer> cardsTodo;
    private Random rnd;
    int randomInt;
    /**
     * The class constructor.
     *
     * @param env    - the environment object.
     * @param dealer - the dealer object.
     * @param table  - the table object.
     * @param id     - the id of the player.
     * @param human  - true iff the player is a human player (i.e. input is provided manually, via the keyboard).
     */
    public Player(Env env, Dealer dealer, Table table, int id, boolean human) {
        this.env = env;
        this.table = table;
        this.id = id;
        this.human = human;
        this.dealer=dealer;
        this.tokensPlaced = 0;
        slotsTodo= new LinkedList<>();
        cardsTodo= new LinkedList<>();
        rnd = new Random();
    }

    /**
     * The main player thread of each player starts here (main loop for the player thread).
     */
    @Override
    public void run() {
        playerThread = Thread.currentThread();
        env.logger.info("Thread " + Thread.currentThread().getName() + " starting.");
        //if (!human) createArtificialIntelligence();

        while (!terminate) {
            // TODO implement main player loop
            try {
                if (!human) {createArtificialIntelligence();}
                if (!slotsTodo.isEmpty()) {
                    Integer slot = slotsTodo.remove();
                    Integer card = cardsTodo.remove();

                    if (!table.removeToken(this.id, slot)) {
                        if (tokensPlaced < 3) {
                            if (card != null && table.placeToken(this.id, slot, card)) {
                                increaseToken();
                            }
                            if (tokensPlaced == 3) {
                                if (this.dealer.addPlayerRequest(this.id)) {
                                    point();
                                } else
                                    penalty();
                            }
                        }
                    } else
                        decreaseToken();
                }
            }
            catch (NoSuchElementException e) {
                synchronized (this)
                {
                try {
                    wait();
                } catch (InterruptedException ex) {
                    throw new RuntimeException(ex);
                }
            }
        }
    }
        if (!human) try { aiThread.join(); } catch (InterruptedException ignored) {}
        env.logger.info("Thread " + Thread.currentThread().getName() + " terminated.");
    }

    /**
     * Creates an additional thread for an AI (computer) player. The main loop of this thread repeatedly generates
     * key presses. If the queue of key presses is full, the thread waits until it is not full.
     */
    private void createArtificialIntelligence() {

        // note: this is a very, very smart AI (!)
        aiThread = new Thread(() -> {
            env.logger.info("Thread " + Thread.currentThread().getName() + " starting.");
            while (!terminate) {
                // TODO implement player key press
                randomInt = rnd.nextInt(env.config.tableSize);
                this.keyPressed(randomInt);
                synchronized (this) {
                    try {
                        wait();
                    } catch (InterruptedException ignored) {
                    }
                }
            }
            env.logger.info("Thread " + Thread.currentThread().getName() + " terminated.");
        }, "computer-" + id);
        aiThread.start();
    }

    /**
     * Called when the game should be terminated.
     */
    public void terminate() {
        // TODO implement
        terminate = true;
    }

    /**
     * This method is called when a key is pressed.
     *
     * @param slot - the slot corresponding to the key pressed.
     */
    public void keyPressed(int slot) {
        // TODO implement
            slotsTodo.add(slot);
            cardsTodo.add(table.slotToCard[slot]);
    }

    /**
     * Award a point to a player and perform other related actions.
     *
     * @post - the player's score is increased by 1.
     * @post - the player's score is updated in the ui.
     */
    public void point() {
        // TODO implement
        env.ui.setScore(id, ++score);
        removeTokens();
    }

    /**
     * Penalize a player and perform other related actions.
     */
    public void penalty() {
        // TODO implement
        slotsTodo.clear();
        cardsTodo.clear();
    }

    public int score() {
        return score;
    }
    public void decreaseToken()
    {
        if (tokensPlaced>0)
            tokensPlaced--;
    }
    public void increaseToken()
    {
        if(tokensPlaced<3)
            tokensPlaced++;
    }

    public void removeTokens()
    {
        tokensPlaced=0;
    }
}
