package com.codex.home.core;

/** The one live line each app shows in Home, from what it has shared, or null when it has shared nothing. */
public final class Status
{
	private Status()
	{
	}

	public static String of(App app, Shared shared, long now)
	{
		switch (app)
		{
			case NAVIGATOR:
				return focus(shared.focus);
			case LEDGER:
				return shared.worth == null ? null : "Worth " + Format.number(shared.worth.total) + week(shared.worth);
			case SESSION_CHRONICLE:
				return session(shared.session, now);
			case BANK_STEWARD:
				return shared.bank == null ? null : "Bank read " + ago(shared.bank.savedAt, now);
			case QUARTERMASTER:
				return kit(shared.kit);
			case GEAR_ROADMAP:
				return shared.gearGoal == null ? null : "Saving for " + shared.gearGoal;
			case GUARDIAN:
				return shared.oneLife ? "Guarding a one-life account" : null;
			case AUTOPILOT:
				return shared.plan == null || shared.plan.isEmpty() ? null : "Next: " + shared.plan.get(0).headline();
			case WAYFINDER:
				return shared.teleports < 0 ? null : Format.plural(shared.teleports, "teleport", "teleports") + " in your atlas";
			case MONEY_PILOT:
				return shared.moneyRuns <= 0 ? null : Format.plural(shared.moneyRuns, "run", "runs") + " clocked";
			case BOSS_PILOT:
				return shared.boss == null ? null : "Pinned: " + shared.boss;
			case NOMAD:
				return shared.nomadKinds < 0 ? null : "Tracking " + Format.plural(shared.nomadKinds, "kind", "kinds") + " of item";
			default:
				return null;
		}
	}

	static String focus(Shared.Focus f)
	{
		if (f == null)
		{
			return null;
		}
		if (f.title == null)
		{
			return f.done > 0 ? "All " + Format.plural(f.done, "goal", "goals") + " done" : "No goals pinned";
		}
		return f.title + ", " + Math.round(Math.floor(f.progress * 100)) + "%";
	}

	static String week(Shared.Worth w)
	{
		if (w.days == 0 || w.change == 0)
		{
			return "";
		}
		return ", " + signed(w.change) + (w.days >= 7 ? " this week" : " in " + Format.plural(w.days, "day", "days"));
	}

	static String session(Shared.Session s, long now)
	{
		if (s == null)
		{
			return null;
		}
		String what = s.activity == null ? "Last session" : s.activity;
		return what + ", " + Format.duration(s.playedMs / 1000) + ", " + ago(s.endedAt, now);
	}

	static String kit(Shared.Kit k)
	{
		if (k == null)
		{
			return null;
		}
		if (!k.chosen())
		{
			return "No kit chosen yet";
		}
		return k.trips < 0 ? k.name + " kit" : k.name + " kit, " + Format.plural(k.trips, "trip", "trips") + " banked";
	}

	/** "+1.2M", "-840k". */
	public static String signed(long n)
	{
		return (n > 0 ? "+" : "") + Format.number(n);
	}

	/** "just now", "4 min ago", "2 days ago". */
	public static String ago(long at, long now)
	{
		long s = (now - at) / 1000;
		if (s < 60)
		{
			return "just now";
		}
		return Format.duration(s).replaceAll(" \\d+ (min|hr)$", "") + " ago";
	}
}
