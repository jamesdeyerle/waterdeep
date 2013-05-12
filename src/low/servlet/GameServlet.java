package low.servlet;

import java.io.IOException;
import java.util.logging.Logger;

import javax.inject.Inject;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import low.annotation.GameKey;
import low.message.JoinGameRequest;
import low.message.JoinGameResponse;
import low.message.JoinGameResponse.Result;
import low.model.Game;
import low.model.Player.Color;
import low.service.GameService;

import com.google.appengine.api.datastore.Key;
import com.google.gson.Gson;
import com.google.inject.Provider;
import com.google.inject.Singleton;

/**
 * Servlet for interacting with a specific game.
 */
@Singleton
public class GameServlet extends HttpServlet {

	private static final long serialVersionUID = 2537789499822803611L;
	
	private static final Logger logger = Logger.getLogger(GameServlet.class.getName());
	
	private final GameService gameService;
	private final @GameKey Provider<Key> gameKeyProvider;
	
	@Inject
	public GameServlet(
			GameService gameService,
			@GameKey Provider<Key> gameKeyProvider) {
		this.gameService = gameService;
		this.gameKeyProvider = gameKeyProvider;
	}
	
	/**
	 * Attempts to join the specific game.
	 */
	@Override
	protected void doGet(HttpServletRequest req, HttpServletResponse res)
			throws IOException {
		
		logger.info("\n\n" + gameKeyProvider.get() + "\n\n");
		Game game = gameService.getGame(gameKeyProvider.get());
		res.getWriter().write(new Gson().toJson(game));
	}
	
	/**
	 * Attempts to join the specific game.
	 */
	@Override
	protected void doPost(HttpServletRequest req, HttpServletResponse res)
			throws IOException {
		
		Gson gson = new Gson();
		JoinGameRequest joinGameRequest = gson.fromJson(
				req.getReader(), JoinGameRequest.class);
		
		// Validate the moderator name.
		String name = joinGameRequest.getName();
		if (name == null || name.trim().isEmpty()) {
			res.sendError(HttpResponseCode.BAD_REQUEST.getCode());
			return;
		}
		
		// Validate the color.
		String colorString = joinGameRequest.getColor();
		if (colorString == null || colorString.trim().isEmpty()) {
			res.sendError(HttpResponseCode.BAD_REQUEST.getCode());
			return;
		}
		Color color = Color.valueOf(colorString.toUpperCase());
		
		// Try to join the game and send the result.
		boolean success = gameService.joinGame(
				gameKeyProvider.get(), name, color);
		Result result = success ? Result.SUCCESS : Result.COLOR_TAKEN;
		res.getWriter().write(gson.toJson(new JoinGameResponse(result)));
	}
}