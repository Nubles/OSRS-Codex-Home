package com.codex.home.core;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/**
 * The threads that tie the suite together: one app shares something it knows, and others use it
 * to do better. Home shows each thread's state on this account, and which app to add so that the
 * apps you already have get smarter.
 */
public enum Link
{
	BANK(App.BANK_STEWARD, Shared.STEWARD, "Your whole bank",
		"Open your bank once with Bank Steward on",
		App.QUARTERMASTER, App.GEAR_ROADMAP, App.LEDGER, App.AUTOPILOT, App.PROVISIONER, App.PLUNDER_PILOT,
		App.PEST_CONTROL_PILOT, App.MINIGAME_PILOT, App.FIGHT_CAVES_PILOT, App.GUARDIAN, App.IRONWORKS, App.NOMAD),
	GOALS(App.NAVIGATOR, Shared.NAVIGATOR, "Your pinned goals",
		"Pin a goal in Navigator",
		App.AUTOPILOT, App.BANK_STEWARD, App.ANNALS, App.MINIGAME_PILOT, App.PLUNDER_PILOT, App.PEST_CONTROL_PILOT),
	WORTH(App.LEDGER, Shared.LEDGER, "Your worth over time",
		"Let Ledger see your bank and Grand Exchange",
		App.GEAR_ROADMAP, App.SESSION_CHRONICLE, App.ANNALS, App.GUARDIAN, App.AUTOPILOT),
	SESSIONS(App.SESSION_CHRONICLE, Shared.CHRONICLE, "Your play sessions",
		"Play a session with Chronicle on",
		App.NAVIGATOR, App.GEAR_ROADMAP, App.LEDGER),
	KITS(App.QUARTERMASTER, Shared.QUARTERMASTER, "Your supply kits",
		"Choose a kit in Quartermaster",
		App.BANK_STEWARD, App.PROVISIONER, App.GUARDIAN, App.NOMAD, App.AUTOPILOT),
	GEAR(App.GEAR_ROADMAP, Shared.ROADMAP, "Your gear and saving goal",
		"Open your bank with Gear Roadmap on",
		App.QUARTERMASTER, App.BANK_STEWARD, App.PLUNDER_PILOT, App.PEST_CONTROL_PILOT, App.AUTOPILOT),
	TRAVEL(App.WAYFINDER, Shared.WAYFINDER, "Your travel atlas",
		"Log in with Wayfinder on",
		App.AUTOPILOT),
	EARNINGS(App.MONEY_PILOT, Shared.MONEY_PILOT, "Your money makers' rates",
		"Clock a money maker in Money Pilot",
		App.AUTOPILOT),
	BOSSES(App.BOSS_PILOT, Shared.BOSS_PILOT, "Your pinned boss and trips",
		"Pin a boss in Boss Pilot",
		App.AUTOPILOT);

	/** How a thread stands on this account. */
	public enum State
	{
		/** The sharing app is on and has shared something. */
		FLOWING,
		/** The sharing app is installed but has nothing shared yet. */
		WAITING,
		/** The sharing app is installed but switched off. */
		OFF,
		/** The sharing app is not installed. */
		MISSING
	}

	private final App from;
	private final String group;
	private final String what;
	private final String start;
	private final List<App> to;

	Link(App from, String group, String what, String start, App... to)
	{
		this.from = from;
		this.group = group;
		this.what = what;
		this.start = start;
		this.to = Collections.unmodifiableList(Arrays.asList(to));
	}

	public State state(Roster roster, Shared shared)
	{
		Roster.State s = roster.state(from);
		if (s == Roster.State.MISSING)
		{
			return State.MISSING;
		}
		if (s == Roster.State.OFF)
		{
			// Something shared before it was switched off still reaches the others.
			return shared.has(group) ? State.FLOWING : State.OFF;
		}
		return shared.has(group) ? State.FLOWING : State.WAITING;
	}

	/** The apps you have that use this thread. */
	public List<App> users(Roster roster)
	{
		List<App> out = new ArrayList<>();
		for (App app : to)
		{
			if (roster.installed(app))
			{
				out.add(app);
			}
		}
		return out;
	}

	public App getFrom()
	{
		return from;
	}

	public String getGroup()
	{
		return group;
	}

	public String getWhat()
	{
		return what;
	}

	/** The one thing to do to start this thread flowing. */
	public String getStart()
	{
		return start;
	}

	public List<App> getTo()
	{
		return to;
	}
}
