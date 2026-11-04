package com.codex.home;

import com.codex.home.core.Account;
import com.codex.home.core.Advice;
import com.codex.home.core.App;
import com.codex.home.core.Format;
import com.codex.home.core.Roster;
import com.codex.home.core.Sailing;
import com.codex.home.core.Shared;
import com.codex.home.core.Status;
import com.codex.home.core.Watch;
import com.codex.home.ui.Actions;
import com.codex.home.ui.HomePanel;
import com.codex.home.ui.Sigil;
import com.codex.home.ui.Tab;
import com.codex.home.ui.View;
import com.google.gson.Gson;
import com.google.inject.Provides;
import java.awt.Color;
import java.util.Collections;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import javax.inject.Inject;
import javax.swing.SwingUtilities;
import net.runelite.api.ChatMessageType;
import net.runelite.api.Client;
import net.runelite.api.GameState;
import net.runelite.api.Skill;
import net.runelite.api.events.GameStateChanged;
import net.runelite.api.events.GameTick;
import net.runelite.api.gameval.VarPlayerID;
import net.runelite.api.gameval.VarbitID;
import net.runelite.client.callback.ClientThread;
import net.runelite.client.chat.ChatColorType;
import net.runelite.client.chat.ChatMessageBuilder;
import net.runelite.client.chat.ChatMessageManager;
import net.runelite.client.chat.QueuedMessage;
import net.runelite.client.config.ConfigManager;
import net.runelite.client.eventbus.Subscribe;
import net.runelite.client.events.ConfigChanged;
import net.runelite.client.events.ExternalPluginsChanged;
import net.runelite.client.events.PluginChanged;
import net.runelite.client.events.RuneScapeProfileChanged;
import net.runelite.client.plugins.Plugin;
import net.runelite.client.plugins.PluginDescriptor;
import net.runelite.client.plugins.PluginManager;
import net.runelite.client.ui.ClientToolbar;
import net.runelite.client.ui.NavigationButton;
import net.runelite.client.util.LinkBrowser;

/**
 * The front door of the Codex suite. Home shows which Codex apps you have and what each is doing,
 * pulls what they share about the account into one view (your next goal, worth, last session,
 * bank and supply kit), and suggests the steps and apps that make the rest work better together.
 * It only reads: it never changes another plugin's settings, never switches plugins on or off and
 * sends nothing off the computer. The only thing it keeps is the tab you last looked at.
 */
@PluginDescriptor(
	name = "Codex Home",
	description = "One panel for the whole Codex suite: your next step, worth, last session and bank, which Codex apps you have and what to add",
	tags = {"codex", "suite", "home", "dashboard", "overview", "account", "goal", "worth"}
)
public class CodexHomePlugin extends Plugin
{
	private static final String HUB_PAGE = "https://runelite.net/plugin-hub/show/";
	private static final String BUILD_PAGE = "https://github.com/Nubles/OSRS-";
	/** Ticks after login before the summary line, so the other apps have read the account first. */
	private static final int LOGIN_LINE_TICKS = 8;
	/** How often the "ago" times are brought up to date while the panel is open. */
	private static final long CLOCK_SECONDS = 60;

	@Inject
	private Client client;

	@Inject
	private ClientThread clientThread;

	@Inject
	private ClientToolbar clientToolbar;

	@Inject
	private ConfigManager configManager;

	@Inject
	private PluginManager pluginManager;

	@Inject
	private ChatMessageManager chatMessageManager;

	@Inject
	private ScheduledExecutorService executor;

	@Inject
	private Gson gson;

	@Inject
	private CodexHomeConfig config;

	private HomePanel panel;
	private NavigationButton navButton;
	private ScheduledFuture<?> clock;

	private volatile Account account = Account.UNKNOWN;
	private volatile boolean loggedIn;
	/** Sailing as last read from the game, or null while logged out. */
	private volatile Sailing sailing;
	private int loginTicks = -1;

	@Provides
	CodexHomeConfig provideConfig(ConfigManager configManager)
	{
		return configManager.getConfig(CodexHomeConfig.class);
	}

	@Override
	protected void startUp()
	{
		panel = new HomePanel(new PanelActions());
		panel.show(rememberedTab());
		navButton = NavigationButton.builder()
			.tooltip("Codex Home")
			.icon(Sigil.icon(16))
			.priority(4)
			.panel(panel)
			.build();
		clientToolbar.addNavigation(navButton);
		clock = executor.scheduleWithFixedDelay(this::refresh, CLOCK_SECONDS, CLOCK_SECONDS, TimeUnit.SECONDS);
		clientThread.invokeLater(() ->
		{
			readAccount();
			refresh();
		});
	}

	@Override
	protected void shutDown()
	{
		if (clock != null)
		{
			clock.cancel(false);
			clock = null;
		}
		clientToolbar.removeNavigation(navButton);
		panel = null;
		account = Account.UNKNOWN;
		loggedIn = false;
		loginTicks = -1;
		sailing = null;
	}

	// ---- Events ----

	@Subscribe
	public void onGameStateChanged(GameStateChanged event)
	{
		if (event.getGameState() == GameState.LOGGED_IN)
		{
			if (!loggedIn)
			{
				loginTicks = 0;
			}
			readAccount();
			refresh();
		}
		else if (event.getGameState() == GameState.LOGIN_SCREEN)
		{
			loggedIn = false;
			loginTicks = -1;
			account = Account.UNKNOWN;
			sailing = null;
			refresh();
		}
	}

	@Subscribe
	public void onGameTick(GameTick tick)
	{
		if (account == Account.UNKNOWN)
		{
			// The ironman varbit can arrive a tick or two after login.
			readAccount();
			if (account != Account.UNKNOWN)
			{
				refresh();
			}
		}
		if (readSailing())
		{
			refresh();
		}
		if (loginTicks >= 0 && ++loginTicks >= LOGIN_LINE_TICKS)
		{
			loginTicks = -1;
			if (config.loginLine())
			{
				executor.execute(this::sayHello);
			}
		}
	}

	@Subscribe
	public void onRuneScapeProfileChanged(RuneScapeProfileChanged event)
	{
		refresh();
	}

	@Subscribe
	public void onConfigChanged(ConfigChanged event)
	{
		String group = event.getGroup();
		if (CodexHomeConfig.GROUP.equals(group) && CodexHomeConfig.TAB_KEY.equals(event.getKey()))
		{
			return;
		}
		// Every Codex app keeps its settings and shared data in a group named codex-something,
		// and RuneLite notes a plugin being switched on or off in the "runelite" group.
		if (group.startsWith("codex") || "runelite".equals(group))
		{
			refresh();
		}
	}

	@Subscribe
	public void onPluginChanged(PluginChanged event)
	{
		refresh();
	}

	@Subscribe
	public void onExternalPluginsChanged(ExternalPluginsChanged event)
	{
		refresh();
	}

	// ---- Reading ----

	/** On the client thread. */
	private void readAccount()
	{
		loggedIn = client.getGameState() == GameState.LOGGED_IN;
		if (!loggedIn)
		{
			account = Account.UNKNOWN;
			return;
		}
		if (client.getLocalPlayer() == null)
		{
			return;
		}
		account = Account.of(client.getVarbitValue(VarbitID.IRONMAN));
	}

	/** On the client thread: reads Sailing and answers whether it changed. A few reads a tick. */
	private boolean readSailing()
	{
		if (client.getGameState() != GameState.LOGGED_IN || client.getLocalPlayer() == null)
		{
			return false;
		}
		Sailing now = new Sailing(client.getRealSkillLevel(Skill.SAILING), client.getSkillExperience(Skill.SAILING),
			client.getVarpValue(VarPlayerID.PORT_TASKS_COMPLETED));
		Sailing was = sailing;
		sailing = now;
		// Redraw on a level, a port task or a whole percent, not on every drop of experience.
		return was == null || was.level != now.level || was.portTasks != now.portTasks
			|| (int) (was.progress() * 100) != (int) (now.progress() * 100);
	}

	private Roster roster()
	{
		Map<App, Roster.State> states = new EnumMap<>(App.class);
		for (Plugin plugin : pluginManager.getPlugins())
		{
			PluginDescriptor d = plugin.getClass().getAnnotation(PluginDescriptor.class);
			App app = d == null ? null : App.byPluginName(d.name());
			if (app != null)
			{
				states.put(app, pluginManager.isPluginEnabled(plugin) ? Roster.State.ON : Roster.State.OFF);
			}
		}
		return new Roster(states);
	}

	private Shared shared()
	{
		if (!loggedIn || configManager.getRSProfileKey() == null)
		{
			return Shared.NONE;
		}
		return Shared.read(configManager::getRSProfileConfiguration, gson, System.currentTimeMillis());
	}

	/** Builds the whole view off the client thread and hands it to the panel. */
	private void refresh()
	{
		if (panel == null)
		{
			return;
		}
		executor.execute(() ->
		{
			HomePanel p = panel;
			if (p == null)
			{
				return;
			}
			View view = view();
			SwingUtilities.invokeLater(() -> p.update(view));
		});
	}

	private View view()
	{
		Roster roster = roster();
		Shared shared = shared();
		Account acct = account;
		List<Advice.Step> steps = Advice.start(roster, shared);
		List<Advice.Pick> picks = config.picks() ? Advice.picks(roster, acct) : Collections.emptyList();
		long now = System.currentTimeMillis();
		List<Watch.Item> watch = config.watch() && loggedIn ? Watch.of(shared, config.sailing() ? sailing : null, now)
			: Collections.emptyList();
		return new View(loggedIn, acct, roster, shared, steps, picks, config.startHere(), now, watch);
	}

	/** The one line at login: the next step and how worth moved, from what the other apps share. */
	private void sayHello()
	{
		Shared s = shared();
		String line = hello(s, config.watchLogin() ? Watch.reminder(Watch.of(s, null, System.currentTimeMillis())) : null);
		if (line == null)
		{
			return;
		}
		String message = new ChatMessageBuilder()
			.append(new Color(214, 178, 106), "Codex: ")
			.append(ChatColorType.NORMAL)
			.append(line)
			.build();
		chatMessageManager.queue(QueuedMessage.builder()
			.type(ChatMessageType.GAMEMESSAGE)
			.runeLiteFormattedMessage(message)
			.build());
	}

	static String hello(Shared s)
	{
		return hello(s, null);
	}

	/** The login line, ending with what needs doing before the next trip when there is any. */
	static String hello(Shared s, String warnings)
	{
		StringBuilder sb = new StringBuilder();
		if (s.focus != null && s.focus.title != null)
		{
			sb.append("next, ").append(s.focus.next != null ? s.focus.next + " (" + s.focus.title + ")" : s.focus.title);
		}
		if (s.worth != null)
		{
			if (sb.length() > 0)
			{
				sb.append(". ");
			}
			sb.append(sb.length() == 0 ? "worth " : "Worth ").append(Format.number(s.worth.total));
			if (s.worth.days > 0 && s.worth.change != 0)
			{
				sb.append(" (").append(Status.signed(s.worth.change)).append(s.worth.days >= 7 ? " this week)" : " in " + Format.plural(s.worth.days, "day", "days") + ")");
			}
		}
		if (warnings != null)
		{
			sb.append(sb.length() == 0 ? "" : ". ").append(sb.length() == 0 ? Character.toLowerCase(warnings.charAt(0)) + warnings.substring(1) : warnings);
		}
		return sb.length() == 0 ? null : sb.append(".").toString();
	}

	private Tab rememberedTab()
	{
		String saved = configManager.getConfiguration(CodexHomeConfig.GROUP, CodexHomeConfig.TAB_KEY);
		for (Tab t : Tab.values())
		{
			if (t.name().equals(saved))
			{
				return t;
			}
		}
		return Tab.TODAY;
	}

	private final class PanelActions implements Actions
	{
		@Override
		public void tab(Tab tab)
		{
			if (panel != null)
			{
				panel.show(tab);
			}
			configManager.setConfiguration(CodexHomeConfig.GROUP, CodexHomeConfig.TAB_KEY, tab.name());
		}

		@Override
		public void open(App app)
		{
			LinkBrowser.browse(page(app));
		}
	}

	/** An app's page: the Plugin Hub's when it is installable there, otherwise the place it is built. */
	static String page(App app)
	{
		if (app.getHub() == App.Hub.LIVE)
		{
			return HUB_PAGE + app.getHubName();
		}
		StringBuilder repo = new StringBuilder();
		for (String part : app.getHubName().split("-"))
		{
			repo.append(repo.length() == 0 ? "" : "-").append(Character.toUpperCase(part.charAt(0))).append(part.substring(1));
		}
		return BUILD_PAGE + repo;
	}
}
