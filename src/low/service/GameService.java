package low.service;

import java.util.logging.Logger;

import javax.annotation.Nullable;

import low.annotation.ClientId;
import low.message.JoinGameResponse;
import low.message.JoinGameResponse.Result;
import low.message.PlayerJoinedMessage;
import low.message.PlayerLeftMessage;
import low.message.StartGameNotification;
import low.model.Game;
import low.model.Player;
import low.model.Player.Color;

import com.google.appengine.api.datastore.Key;
import com.google.appengine.api.datastore.KeyFactory;
import com.google.code.twig.ObjectDatastore;
import com.google.inject.Inject;
import com.google.inject.Provider;

public class GameService {
	
	private static final Logger logger = Logger.getLogger(GameService.class.getName());
	
	private final Provider<ObjectDatastore> datastoreProvider;
	private final Provider<String> clientIdProvider;
	private final MessageService messageService;
	
	@Inject
	public GameService(
			Provider<ObjectDatastore> datastoreProvider,
			@ClientId Provider<String> clientIdProvider,
			MessageService messageService) {
		this.datastoreProvider = datastoreProvider;
		this.clientIdProvider = clientIdProvider;
		this.messageService = messageService;
	}
	
	/**
	 * Creates a new game and immediate stores it in the database.
	 * @param moderatorName The name of the moderator.
	 * @param color The moderator's color.
	 * @return The new game reference.
	 */
	public Game newGame(String moderatorName, Color color) {
		ObjectDatastore datastore = datastoreProvider.get();
		String clientId = clientIdProvider.get();
		
		Game game = new Game();
		game.addPlayer(new Player(clientId, moderatorName, color, true));
		
		datastore.store().instance(game).now();
		Key key = datastore.associatedKey(game);
		game.setKey(KeyFactory.keyToString(key));
		return game;
	}
	
	/**
	 * Gets the game for the given key.
	 * @param key The game's key.
	 * @return The game, if found.
	 */
	@Nullable
	public Game getGame(Key key) {
		ObjectDatastore datastore = datastoreProvider.get();
		Game game = datastore.load(key);
		if (game != null) {
			game.setKey(KeyFactory.keyToString(key));			
		}
		return game;
	}
	
	/**
	 * Joins the game with the given name and color.
	 * @param key The key of the game.
	 * @param name The name of the player.
	 * @param color Their color.
	 * @return The result of the join game attempt.
	 */
	public JoinGameResponse joinGame(Key key, String name, Color color) {
		logger.info(name + " is attempting to join this game: " + key);
		Game game = getGame(key);
		if (game == null) {
			return new JoinGameResponse(Result.NOT_FOUND);
		}
		
		// Make sure this client isn't already in this game.
		String clientId = clientIdProvider.get();
		for (Player player : game.getPlayers()) {
			if (player.getClientId().equals(clientId)) {
				return new JoinGameResponse(Result.ALREADY_JOINED);
			}
		}
		
		// Make sure the game isn't full.
		if (game.isFull()) {
			return new JoinGameResponse(Result.GAME_FULL);
		}
		
		// Make sure the color isn't taken.
		for (Player player : game.getPlayers()) {
			if (player.getColor() == color) {
				return new JoinGameResponse(Result.COLOR_TAKEN);
			}
		}
		
		// Update the game.
		Player player = new Player(clientId, name, color, false);
		game.addPlayer(player);
		ObjectDatastore datastore = datastoreProvider.get();
		datastore.update(game);
		
		// Notify all other players.
		messageService.broadcast(game, new PlayerJoinedMessage(player));
		
		return new JoinGameResponse(Result.SUCCESS, game);
	}
	
	/**
	 * Leaves the game identified by the key.
	 * @param key
	 */
	public void leaveGame(Key key) {
		
		// Make sure the game exists.
		Game game = getGame(key);
		if (game == null) {
			logger.severe("No game found.");
			return;
		}
		
		// Remove the player from the game.
		String clientId = clientIdProvider.get();
		Player player = game.removePlayer(clientId);
		if (player == null) {
			logger.severe(clientId + " was not in the game.");
			return;
		}
		logger.info(clientId + " left the game.");
		
		// The game needs to be deleted if there are no more players.
		ObjectDatastore datastore = datastoreProvider.get();
		if (game.getPlayers().size() == 0) {
			logger.info("Deleting the now empty game.");
			datastore.delete(game);
		} else {
			
			// If the moderator left, pass the buck to a random player.
			Player moderator = game.getModerator();
			if (player.isModerator()) {
				moderator = game.getPlayers().get(0);
				moderator.setModerator(true);
			}
			
			// Update and notify everyone.
			datastore.update(game);
			messageService.broadcast(game, new PlayerLeftMessage(player, moderator));
		}
	}
	
	/**
	 * Starts the game and notifies everyone.
	 * @param key
	 */
	public void startGame(Key key) {
		
		// Make sure the game exists.
		Game game = getGame(key);
		if (game == null) {
			logger.severe("No game found.");
			return;
		}
		
		// Make sure the moderator is the one starting the game.
		String clientId = clientIdProvider.get();
		if (!clientId.equals(game.getModerator().getClientId())) {
			logger.severe("Non moderator tried to start this game: " + key);
			return;
		}
		
		// Make sure there are at least two players.
		if (game.getPlayers().size() < 2) {
			logger.severe("Cannot start a game with less than 2 players.");
			return;
		}
		
		// Pick one of the players to have the First Player marker.
		int numPlayers = game.getPlayers().size();
		int randomIndex = (int) Math.floor(Math.random() * numPlayers);
		game.setFirstPlayer(game.getPlayers().get(randomIndex));
		
		// Give all the players some money according to their starting position.
		for (int i = 0; i < numPlayers; i++) {
			int index = (i + randomIndex) % numPlayers;
			Player player = game.getPlayers().get(index);
			player.getTavern().setGold(i + 4);
		}
		
		// Mark the game as started.
		game.setStarted(true);
		ObjectDatastore datastore = datastoreProvider.get();
		datastore.update(game);
		
		// Notify everyone that the game started.
		messageService.broadcast(game, new StartGameNotification(game), true);
	}
}
