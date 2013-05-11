package low.module;

import low.annotation.GameKey;
import low.provider.GameKeyProvider;
import low.service.GameService;

import com.google.inject.AbstractModule;

public class WaterdeepGuiceModule extends AbstractModule {

	@Override
	protected void configure() {
		
		bind(String.class).annotatedWith(GameKey.class)
			.toProvider(GameKeyProvider.class);
		
		bind(GameService.class);
	}
}
