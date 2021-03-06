package low.handler;

import java.io.IOException;

import javax.servlet.http.HttpServletResponse;

import low.annotation.GameKey;
import low.message.Message;
import low.service.GameService;

import com.google.appengine.api.datastore.Key;
import com.google.inject.Inject;
import com.google.inject.Provider;

/**
 * Handles the requests to leave games.
 */
public class LeaveGameHandler implements MessageHandler {

	private final @GameKey Provider<Key> gameKeyProvider;
	private final GameService gameService;
	
	@Inject
	public LeaveGameHandler(
			@GameKey Provider<Key> gameKeyProvider,
			GameService gameService) {
		this.gameKeyProvider = gameKeyProvider;
		this.gameService = gameService;
	}
	
	@Override
	public Message handle(Message message, HttpServletResponse res)
			throws IOException {
		gameService.leaveGame(gameKeyProvider.get());
		return null;
	}
}
