/**
 * Service for fetching existing games.
 */

goog.provide('low.service.Games');

goog.require('goog.Uri');
goog.require('goog.array');
goog.require('goog.events.EventTarget');
goog.require('goog.log');
goog.require('low.ServletPath');
goog.require('low.model.Game');
goog.require('low.service.Xhr');



/**
 * @constructor
 * @extends {goog.events.EventTarget}
 */
low.service.Games = function() {
  goog.base(this);

  /** @protected {goog.log.Logger} */
  this.logger = goog.log.getLogger('low.service.Games');

  /** @private {!low.service.Xhr} */
  this.xhrService_ = low.service.Xhr.getInstance();
};
goog.inherits(low.service.Games, goog.events.EventTarget);
goog.addSingletonGetter(low.service.Games);


/**
 * @return {!Array.<!low.model.Game>}
 */
low.service.Games.prototype.getGames = function() {
  return this.games_;
};


/**
 * Loads the games from the server.
 * @return {!goog.async.Deferred}
 */
low.service.Games.prototype.loadGames = function() {
  goog.log.info(this.logger, 'Loading games.');

  // Create the request URL.
  var uri = new goog.Uri();
  uri.setPath(low.ServletPath.GAMES);

  // Send the request.
  var deferred = this.xhrService_.get(uri, true);

  // Handle the response.
  deferred.addCallback(this.onGamesLoaded_, this);

  return deferred;
};


/**
 * Called when the request for games completes successfully.
 * @param {Object} gamesJson The JSON response.
 * @return {!Array.<!low.model.Game>} The parsed game objects from the response.
 * @private
 */
low.service.Games.prototype.onGamesLoaded_ = function(gamesJson) {
  goog.log.info(this.logger, 'Games just loaded.');

  if (!goog.isArray(gamesJson)) {
    var errorMsg = 'JSON was not in array format.';
    goog.log.error(this.logger, errorMsg);
    throw Error(errorMsg);
  }

  // Convert all the JSON into model objects.
  this.games_ = [];
  goog.array.forEach(gamesJson, function(gameJson) {
    try {
      this.games_.push(low.model.Game.fromJson(gameJson));
    } catch (e) {
      // Catch the error so other games still make it through.
      goog.log.error(
          this.logger, 'Failed to build game from this JSON: ' + gameJson);
    }
  }, this);

  // Games is what is now passed in the deferred chain.
  return this.games_;
};
