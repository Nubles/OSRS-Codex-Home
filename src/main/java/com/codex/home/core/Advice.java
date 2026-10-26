package com.codex.home.core;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * What Home suggests: the few first steps that get the suite sharing with itself, and the apps
 * worth adding to this account, each with the reason it was picked.
 */
public final class Advice
{
	/** The first steps, in the order to take them. */
	static final List<Link> START = Arrays.asList(Link.BANK, Link.GOALS, Link.WORTH, Link.SESSIONS, Link.KITS);
	static final int PICKS = 3;

	/** Apps worth having on any account, best first, for when nothing more particular applies. */
	private static final List<App> STAPLES = Arrays.asList(App.NAVIGATOR, App.AUTOPILOT, App.BANK_STEWARD, App.LEDGER,
		App.SESSION_CHRONICLE, App.ROUNDS, App.LOG_INSIGHT, App.ANNALS);

	private Advice()
	{
	}

	public static final class Step
	{
		public final Link link;
		public final boolean done;
		/** What to do, or what it gave once done. */
		public final String detail;

		Step(Link link, boolean done, String detail)
		{
			this.link = link;
			this.done = done;
			this.detail = detail;
		}

		public String title()
		{
			return link.getStart();
		}
	}

	public static final class Pick
	{
		public final App app;
		public final String reason;

		Pick(App app, String reason)
		{
			this.app = app;
			this.reason = reason;
		}
	}

	public static List<Step> start(Roster roster, Shared shared)
	{
		List<Step> steps = new ArrayList<>();
		for (Link link : START)
		{
			App from = link.getFrom();
			Link.State state = link.state(roster, shared);
			int users = link.users(roster).size();
			String detail;
			String apps = users == 1 ? "1 of your apps" : users + " of your apps";
			switch (state)
			{
				case FLOWING:
					detail = users == 0 ? "Done." : "Done, shared with " + apps + ".";
					break;
				case MISSING:
					detail = "Add " + from.getPluginName() + (from.installable() ? " from the Plugin Hub." : " once it is on the Plugin Hub.")
						+ (users == 0 ? "" : " " + capital(apps) + " would use it.");
					break;
				case OFF:
					detail = "Switch " + from.getPluginName() + " on in RuneLite's plugin list.";
					break;
				default:
					detail = users == 0 ? "It is installed, so this takes a minute in its panel." : capital(apps) + " will use it.";
					break;
			}
			steps.add(new Step(link, state == Link.State.FLOWING, detail));
		}
		return steps;
	}

	public static int done(List<Step> steps)
	{
		int n = 0;
		for (Step s : steps)
		{
			if (s.done)
			{
				n++;
			}
		}
		return n;
	}

	public static List<Pick> picks(Roster roster, Account account)
	{
		Map<App, String> out = new LinkedHashMap<>();
		if (account == Account.ULTIMATE)
		{
			offer(out, roster, App.NOMAD, "Made for Ultimate Ironmen: every pack, sack and deathpile tracked for you.");
		}
		if (account.iron())
		{
			offer(out, roster, App.IRONWORKS, "Made for ironmen: what you have, what you can make and where to get the rest.");
		}
		if (account == Account.HARDCORE)
		{
			offer(out, roster, App.GUARDIAN, "Watches your risk so a hardcore life ends later, not sooner.");
		}

		// An app that would feed several you already have makes them all better at once.
		List<Link> links = new ArrayList<>(Arrays.asList(Link.values()));
		links.sort(Comparator.comparingInt((Link l) -> l.users(roster).size()).reversed());
		for (Link link : links)
		{
			int users = link.users(roster).size();
			if (users >= 2)
			{
				offer(out, roster, link.getFrom(), "Shares " + link.getWhat().toLowerCase() + " with " + users
					+ " apps you already have.");
			}
		}

		for (App app : STAPLES)
		{
			offer(out, roster, app, app.getBlurb());
		}
		if (!account.iron())
		{
			offer(out, roster, App.GEAR_ROADMAP, App.GEAR_ROADMAP.getBlurb());
		}

		List<Pick> picks = new ArrayList<>();
		for (Map.Entry<App, String> e : out.entrySet())
		{
			if (picks.size() == PICKS)
			{
				break;
			}
			picks.add(new Pick(e.getKey(), e.getValue()));
		}
		return picks;
	}

	private static void offer(Map<App, String> out, Roster roster, App app, String reason)
	{
		if (!roster.installed(app) && app.installable() && !out.containsKey(app))
		{
			out.put(app, reason);
		}
	}

	static String capital(String s)
	{
		return s.isEmpty() ? s : Character.toUpperCase(s.charAt(0)) + s.substring(1);
	}
}
