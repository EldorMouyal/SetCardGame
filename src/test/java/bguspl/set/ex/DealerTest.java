package bguspl.set.ex;

import bguspl.set.Config;
import bguspl.set.Env;
import bguspl.set.UserInterface;
import bguspl.set.Util;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.logging.Logger;

import static org.junit.jupiter.api.Assertions.*;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;


@ExtendWith(MockitoExtension.class)
class DealerTest {
	@Mock
	Player player1;
	@Mock
	Player player2;
	@Mock
	Util util;
	@Mock
	private UserInterface ui;
	@Mock
	private Table table;

	private Dealer dealer;

	@Mock
	private Logger logger;

	@BeforeEach
	void setUp() {
		Env env = new Env(logger, new Config(logger, (String) null), ui, util);
		Player[] players = new Player[2];
		dealer = new Dealer(env,table,players);
		player1 = new Player(env,dealer,table,0,true);
		player2 = new Player(env,dealer,table,1,true);
		players[0] = player1;
		players[1] = player2;
		table = new Table(env);
	}

	@Test
	void addPlayerRequest() {
		assertEquals(false,dealer.addPlayerRequest(1));
		when(util.testSet(null)).thenReturn(true);
		assertEquals(true,dealer.addPlayerRequest(0));
		
	}
}