package com.codex.home.core;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/**
 * Every app in the Codex suite, in the order the Suite tab lists them. Each knows the name its
 * plugin registers with RuneLite, so Home can tell whether it is installed and switched on, the
 * Plugin Hub name to link to, and the two letters on its seal.
 */
public enum App
{
	NAVIGATOR("Codex Navigator", "codex-navigator", "NV", Family.PLAN, Hub.LIVE,
		"Pin any quest, diary or skill goal and see the next step towards it."),
	AUTOPILOT("Codex Autopilot", "codex-autopilot", "AP", Family.PLAN, Hub.LIVE,
		"One next step for the whole account: chores that are ready, dailies and your goal."),
	GEAR_ROADMAP("Codex Gear Roadmap", "codex-gear-roadmap", "GR", Family.PLAN, Hub.LIVE,
		"Ranks the gear upgrades worth buying next by damage gained per coin."),
	LOG_INSIGHT("Codex Log Insight", "codex-log-insight", "LI", Family.PLAN, Hub.LIVE,
		"Collection log odds, how dry you are and the fastest slots to fill next."),
	WAYFINDER("Codex Wayfinder", "codex-wayfinder", "WF", Family.PLAN, Hub.SOON,
		"The fastest way anywhere with the teleports, rings and trees this account has."),

	LEDGER("Codex Ledger", "codex-ledger", "LD", Family.WEALTH, Hub.LIVE,
		"Your worth across bank, Grand Exchange and storage, day by day."),
	BANK_STEWARD("Codex Bank Steward", "codex-bank-steward", "BS", Family.WEALTH, Hub.LIVE,
		"What your bank still needs to keep and what you can safely clear out."),
	QUARTERMASTER("Codex Quartermaster", "codex-quartermaster", "QM", Family.WEALTH, Hub.LIVE,
		"Supply kits for your trips: how many you can do and what to restock."),
	PROVISIONER("Codex Provisioner", "codex-provisioner", "PV", Family.WEALTH, Hub.LIVE,
		"Walks you through gathering and making a kit's supplies, one step at a time."),
	MONEY_PILOT("Codex Money Pilot", "codex-money-pilot", "MO", Family.WEALTH, Hub.SOON,
		"Money makers you can do now, ranked by what they pay you an hour."),

	SESSION_CHRONICLE("Codex Session Chronicle", "codex-session-chronicle", "SC", Family.STORY, Hub.LIVE,
		"A recap of every session: XP, loot, kills and time, kept as a journal."),
	ANNALS("Codex Annals", "codex-annals", "AN", Family.STORY, Hub.LIVE,
		"Your account's year as a timeline of milestones, drops and goals."),

	ROUNDS("Codex Rounds", "codex-rounds", "RD", Family.GUIDES, Hub.LIVE,
		"A timed round of bird houses, seaweed, special trees, dailies and your kingdom."),
	BOSS_PILOT("Codex Boss Pilot", "codex-boss-pilot", "BP", Family.GUIDES, Hub.SOON,
		"The next boss for your account, your best setup from the bank and the kit to take."),
	MINIGAME_PILOT("Codex Minigame Pilot", "codex-minigame-pilot", "MP", Family.GUIDES, Hub.LIVE,
		"One next step in skilling minigames, and the points still needed for your reward."),
	PLUNDER_PILOT("Codex Plunder Pilot", "codex-plunder-pilot", "PL", Family.GUIDES, Hub.LIVE,
		"Pyramid Plunder: the room to work next, the urns worth it and your sceptre odds."),
	PEST_CONTROL_PILOT("Codex Pest Control Pilot", "codex-pest-control-pilot", "PC", Family.GUIDES, Hub.LIVE,
		"Which portal to work next and how many games until your Void reward."),
	FIGHT_CAVES_PILOT("Codex Fight Caves Pilot", "codex-fight-caves-pilot", "FC", Family.GUIDES, Hub.SOON,
		"The Fight Caves rotation, where to stand and what is coming next wave."),
	ARCHIVE_SPLITS("Codex Archive Splits", "codex-archive-splits", "AS", Family.GUIDES, Hub.PAUSED,
		"Room splits, points and purples for the Fractured Archive."),

	GUARDIAN("Codex Guardian", "codex-guardian", "GD", Family.ACCOUNTS, Hub.REVIEW,
		"Warns before risky places and fights on a one-life account, and keeps your close calls."),
	IRONWORKS("Codex Ironworks", "codex-ironworks", "IW", Family.ACCOUNTS, Hub.LIVE,
		"An ironman's stores, makes, sources and shop runs, with a guided route."),
	NOMAD("Codex Nomad", "codex-nomad", "NM", Family.ACCOUNTS, Hub.LIVE,
		"For Ultimate Ironmen: every pack, sack, deathpile and store in one place.");

	/** The groups the Suite tab is split into. */
	public enum Family
	{
		PLAN("Plan"),
		WEALTH("Wealth & bank"),
		STORY("Story"),
		GUIDES("Guides"),
		ACCOUNTS("Account types");

		private final String title;

		Family(String title)
		{
			this.title = title;
		}

		public String getTitle()
		{
			return title;
		}
	}

	/** Where the app stands on the Plugin Hub. */
	public enum Hub
	{
		/** On the Hub and installable. */
		LIVE,
		/** Submitted and waiting for the Hub's review. */
		REVIEW,
		/** Built, not yet submitted. */
		SOON,
		/** On the Hub but switched off there for now. */
		PAUSED
	}

	private static final List<App> ALL = Collections.unmodifiableList(Arrays.asList(values()));

	private final String pluginName;
	private final String hubName;
	private final String seal;
	private final Family family;
	private final Hub hub;
	private final String blurb;

	App(String pluginName, String hubName, String seal, Family family, Hub hub, String blurb)
	{
		this.pluginName = pluginName;
		this.hubName = hubName;
		this.seal = seal;
		this.family = family;
		this.hub = hub;
		this.blurb = blurb;
	}

	public static List<App> all()
	{
		return ALL;
	}

	/** The app a RuneLite plugin name belongs to, or null if it is not part of the suite. */
	public static App byPluginName(String name)
	{
		for (App app : ALL)
		{
			if (app.pluginName.equals(name))
			{
				return app;
			}
		}
		return null;
	}

	/** "Navigator": the name without the suite's prefix, for tight spaces. */
	public String shortName()
	{
		return pluginName.substring("Codex ".length());
	}

	/** Whether someone without it can install it today. */
	public boolean installable()
	{
		return hub == Hub.LIVE;
	}

	public String getPluginName()
	{
		return pluginName;
	}

	public String getHubName()
	{
		return hubName;
	}

	public String getSeal()
	{
		return seal;
	}

	public Family getFamily()
	{
		return family;
	}

	public Hub getHub()
	{
		return hub;
	}

	public String getBlurb()
	{
		return blurb;
	}
}
