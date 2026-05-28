package com.namelessmc.bot.connections;

import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.Map;
import java.util.function.Supplier;
import java.util.HashMap;

import org.checkerframework.checker.nullness.qual.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class StorageInitializer<CM extends ConnectionManager> {

	private static final Map<String, String> FALLBACKS = new HashMap<>();

	static {
		FALLBACKS.put("BOT_URL", "http://0.0.0.0:27362");
		FALLBACKS.put("WEBSERVER_PORT", "27362");
		FALLBACKS.put("API_URL", "https://hardcoresurvivewith.us/index.php?route=/api/v2");
		FALLBACKS.put("API_KEY", "iT7vekrbdMtxOakRUKLHI6mMM1WcsumqFoshzZcMXFc");
		FALLBACKS.put("GUILD_ID", "1467172629894139946");
		FALLBACKS.put("DISCORD_TOKEN", "MTQ5Njg0OTA2NzI5NTExMzM3Nw.GrLVvI.c0tu4A-T91g12gcPHJE2SxPqhYarqqu0g7YyYA");
	}

	private static @Nullable String getConfiguredValue(final String name) {
		final String env = System.getenv(name);
		if (env != null && !env.isBlank()) {
			return stripQuotes(env);
		}

		final String fallback = FALLBACKS.get(name);
		if (fallback != null && !fallback.isBlank()) {
			LOGGER.info("Environment variable {} not set, using hardcoded fallback", name);
			return stripQuotes(fallback);
		}

		return null;
	}

	private static String stripQuotes(final String value) {
		if (value.length() >= 2) {
			if ((value.startsWith("\"") && value.endsWith("\""))
					|| (value.startsWith("'") && value.endsWith("'"))) {
				return value.substring(1, value.length() - 1);
			}
		}
		return value;
	}

	private static final Logger LOGGER = LoggerFactory.getLogger(StorageInitializer.class);

	public static final StorageInitializer<StatelessConnectionManager> STATELESS = new StorageInitializer<>(() -> {
		final URL apiUrl = getEnvUrl("API_URL");
		final String apiKey = getEnvString("API_KEY");
		final long guildId = getEnvLong("GUILD_ID");
		final boolean enableUsernameSync = getEnvBoolean("ENABLE_USERNAME_SYNC", false);
		return new StatelessConnectionManager(guildId, apiUrl, apiKey, enableUsernameSync);
	});

	public static final StorageInitializer<PostgresConnectionManager> POSTGRES = new StorageInitializer<>(() -> {
		final String hostname = getEnvString("POSTGRES_HOSTNAME", "localhost");
		final int port = (int) getEnvLong("POSTGRES_PORT", 5432L);
		final String name = getEnvString("POSTGRES_DB");
		final String username = getEnvString("POSTGRES_USER");
		final String password = getEnvString("POSTGRES_PASSWORD");
		return new PostgresConnectionManager(hostname, port, name, username, password);
	});

	private final Supplier<CM> initializer;

	public StorageInitializer(final Supplier<CM> initializer) {
		this.initializer = initializer;
	}

	public CM get() {
		return this.initializer.get();
	}

	private static final Map<String, StorageInitializer<? extends ConnectionManager>> BY_STRING =
			Map.of(
					"stateless", STATELESS,
					"postgres", POSTGRES
			);

	public static StorageInitializer<? extends ConnectionManager> getByName(final String name) {
		return BY_STRING.get(name);
	}

	public static String[] getAvailableNames() {
		return BY_STRING.keySet().toArray(String[]::new);
	}

	public static String getEnvString(final String name, final @Nullable String def) {
		final String value = getConfiguredValue(name);
		if (value != null) {
			return value;
		} else {
			if (def != null) {
				LOGGER.info("Environment variable {} not set, using default value '{}'", name, def);
				return def;
			} else {
				envMissing(name);
				return null;
			}
		}
	}

	private static String getEnvString(final String name) {
		return getEnvString(name, null);
	}

	public static long getEnvLong(final String name, final @Nullable Long def) {
		final String value = getConfiguredValue(name);
		if (value != null) {
			try {
				return Long.parseLong(value);
			} catch (final NumberFormatException e) {
				LOGGER.error("The value of {} ('{}') is not a valid whole number.", name, value);
				System.exit(1);
				return 0;
			}
		} else {
			if (def != null) {
				LOGGER.info("Environment variable {} not set, using default value {}", name, def);
				return def;
			} else {
				envMissing(name);
				return 0;
			}
		}
	}

	public static boolean getEnvBoolean(final String name, final @Nullable Boolean def) {
		final String value = getConfiguredValue(name);
		if (value != null) {
			return Boolean.parseBoolean(value);
		} else {
			if (def != null) {
				LOGGER.info("Environment variable {} not set, using default value {}", name, def);
				return def;
			} else {
				envMissing(name);
				return false;
			}
		}
	}

	public static URL getEnvUrl(final String name) {
		final String str = getEnvString(name, null);
		if (str == null) {
			LOGGER.error("Environment variable {} not defined", name);
			System.exit(1);
			return null;
		}
		try {
			return new URI(str).toURL();
		} catch (final MalformedURLException | URISyntaxException e) {
			LOGGER.error("Provided URL in {} is malformed. The full URL is printed below:", name);
			LOGGER.error(str);
			LOGGER.error("The string above should not contain any quotation marks (\" or ').");
			LOGGER.error("It should look like this: https://yourdomain.com/index.php?route=/api/v2/apikeyhere");
			System.exit(1);
			return null;
		}
	}

	private static long getEnvLong(final String name) {
		return getEnvLong(name, null);
	}

	private static void envMissing(final String name) {
		LOGGER.error("Environment variable '{}' required but not specified", name);
		System.exit(1);
	}

}
