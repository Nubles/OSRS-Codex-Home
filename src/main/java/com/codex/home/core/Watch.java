package com.codex.home.core;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * Home's watch list: the few things across the suite worth a glance before you set off, each from
 * the app that knows it. A charged weapon running dry (Boss Pilot), a Hunter rumour's rare part to
 * hand in (Autopilot), your last Tempoross game and the Guardians of the Rift searches banked
 * (Minigame Pilot), and how your Sailing stands (read from the game). What needs doing comes first.
 */
public final class Watch
{
	/** Fewer kills left than this, at the player's own pace, is running low. */
	public static final int LOW_KILLS = 10;
	/** With no pace learnt, an item under this share of full is running low. */
	public static final double LOW_SHARE = 0.10;
	/** Charged items listed when none is low. */
	static final int CALM_CHARGES = 1;
	/** Low charged items listed at most. */
	static final int LOW_CHARGES = 2;

	private Watch()
	{
	}

	/** How much an item asks of the player. */
	public enum Tone
	{
		/** Do something about it before the next trip. */
		ALERT,
		/** Something is waiting to be done or spent. */
		READY,
		/** Good to know. */
		STEADY
	}

	public static final class Item
	{
		/** A stable name for tests and the panel: "charges:blowpipe", "rumour", "sailing". */
		public final String key;
		/** The app it comes from, or null when Home reads it from the game. */
		public final App from;
		/** "SAILING", "HUNTER RUMOUR". */
		public final String label;
		public final String title;
		public final String detail;
		public final Tone tone;
		/** A bar to draw, 0 to 1, or -1 for none. */
		public final double progress;

		Item(String key, App from, String label, String title, String detail, Tone tone, double progress)
		{
			this.key = key;
			this.from = from;
			this.label = label;
			this.title = title;
			this.detail = detail;
			this.tone = tone;
			this.progress = progress;
		}

		@Override
		public String toString()
		{
			return key + ": " + title + (detail == null ? "" : " (" + detail + ")");
		}
	}

	public static List<Item> of(Shared s, Sailing sailing, long now)
	{
		List<Item> out = new ArrayList<>();
		charges(out, s);
		rumour(out, s.rumour);
		rift(out, s.rift);
		storm(out, s.storm, now);
		sailing(out, sailing);
		out.sort((a, b) -> Integer.compare(a.tone.ordinal(), b.tone.ordinal()));
		return out;
	}

	/** Whether a charged item is running low. */
	public static boolean low(Shared.Charge c)
	{
		if (c.left <= 0)
		{
			return true;
		}
		if (c.kills >= 0)
		{
			return c.kills < LOW_KILLS;
		}
		return c.max > 0 && c.left < c.max * LOW_SHARE;
	}

	private static void charges(List<Item> out, Shared s)
	{
		int low = 0;
		int calm = 0;
		for (Shared.Charge c : s.charges)
		{
			boolean isLow = low(c);
			if (isLow ? low >= LOW_CHARGES : low > 0 || calm >= CALM_CHARGES)
			{
				continue;
			}
			if (isLow)
			{
				low++;
			}
			else
			{
				calm++;
			}
			String amount = (c.estimated() ? "~" : "") + Format.number(c.left) + " " + c.pool;
			String title;
			if (c.left <= 0)
			{
				title = c.name + ": out of " + c.pool;
			}
			else if (c.kills >= 0)
			{
				long k = Math.max(0, (long) Math.floor(c.kills));
				title = c.name + ": " + (k < 1 ? "under 1 kill" : "~" + Format.plural(k, "kill", "kills")) + " left";
			}
			else
			{
				title = c.name + ": " + amount;
			}
			StringBuilder d = new StringBuilder();
			if (c.kills >= 0 && c.left > 0)
			{
				d.append(amount).append(c.boss == null ? ", at your pace" : ", at your " + c.boss + " pace");
			}
			else if (c.killsSince > 0)
			{
				d.append(Format.plural(c.killsSince, "kill", "kills")).append(" since you checked it");
			}
			else
			{
				d.append("As the game said when you checked it");
			}
			if (isLow)
			{
				d.append(". Recharge before your next trip");
			}
			double share = c.max > 0 ? Math.min(1, c.left / (double) c.max) : -1;
			out.add(new Item("charges:" + c.key, App.BOSS_PILOT, isLow ? "CHARGES LOW" : "CHARGES", title, d.toString(),
				isLow ? Tone.ALERT : Tone.STEADY, share));
		}
	}

	private static void rumour(List<Item> out, Shared.Rumour r)
	{
		if (r == null)
		{
			return;
		}
		String count = r.done >= 0 ? Format.plural(r.done, "rumour", "rumours") + " done" : null;
		if (r.creature == null)
		{
			out.add(new Item("rumour", App.AUTOPILOT, "HUNTER RUMOUR", "No rumour held",
				"Ask a guild hunter for your next" + (count == null ? "" : "; " + count), Tone.READY, -1));
			return;
		}
		String forWhom = r.master == null ? "" : " for " + r.master;
		if (r.part)
		{
			out.add(new Item("rumour", App.AUTOPILOT, "HUNTER RUMOUR", "Hand in the rare part",
				"Your " + r.creature.toLowerCase(Locale.ROOT) + " rumour" + forWhom + " is done: take the part to the Hunter Guild",
				Tone.ALERT, -1));
			return;
		}
		out.add(new Item("rumour", App.AUTOPILOT, "HUNTER RUMOUR", "Hunt " + article(r.creature),
			(forWhom.isEmpty() ? "" : capital(forWhom.trim())) + (count == null ? "" : (forWhom.isEmpty() ? "" : "; ") + capital(count)),
			Tone.STEADY, -1));
	}

	private static void rift(List<Item> out, Shared.Rift r)
	{
		if (r == null)
		{
			return;
		}
		int searches = r.searches();
		String detail = Format.number(r.elemental) + " elemental, " + Format.number(r.catalytic) + " catalytic points";
		int gap = Math.abs(r.elemental - r.catalytic);
		if (gap > 0)
		{
			detail += "; " + (r.elemental < r.catalytic ? "elemental" : "catalytic") + " is " + Format.number(gap) + " behind";
		}
		out.add(new Item("rift", App.MINIGAME_PILOT, "RIFT POINTS",
			searches == 0 ? "No searches banked" : Format.plural(searches, "search", "searches") + " banked",
			detail, searches > 0 ? Tone.READY : Tone.STEADY, -1));
	}

	private static void storm(List<Item> out, Shared.Storm g, long now)
	{
		if (g == null)
		{
			return;
		}
		String title = g.won ? "Last game: " + Format.plural(g.permits, "permit", "permits")
			: "Last game: washed away";
		StringBuilder d = new StringBuilder();
		if (g.won)
		{
			d.append(Format.number(g.points)).append(" points, ");
		}
		d.append(Status.ago(g.endedAt, now));
		if (g.permitsPerHour >= 0 && g.games > 1)
		{
			d.append("; ").append(String.format(Locale.ROOT, "%.1f", g.permitsPerHour)).append(" permits an hour over ")
				.append(g.games).append(" games");
		}
		out.add(new Item("tempoross", App.MINIGAME_PILOT, "TEMPOROSS", title, d.toString(), Tone.STEADY, -1));
	}

	private static void sailing(List<Item> out, Sailing s)
	{
		if (s == null)
		{
			return;
		}
		String title = s.level >= 99 ? "Level 99" : "Level " + s.level + ", " + Format.number(s.toNext()) + " xp to " + (s.level + 1);
		StringBuilder d = new StringBuilder();
		if (s.portTasks > 0)
		{
			d.append(Format.plural(s.portTasks, "port task", "port tasks")).append(" done; ");
		}
		d.append("you can hold ").append(s.slots()).append(" at once");
		int next = s.nextSlotLevel();
		if (next > 0)
		{
			d.append(", ").append(s.slots() + 1).append(" from level ").append(next);
		}
		out.add(new Item("sailing", null, "SAILING", title, d.toString(), Tone.STEADY, s.progress()));
	}

	/** The one line for the chat at login about what needs doing, or null. */
	public static String reminder(List<Item> items)
	{
		List<String> parts = new ArrayList<>();
		for (Item i : items)
		{
			if (i.tone == Tone.ALERT)
			{
				parts.add(i.title);
			}
		}
		return parts.isEmpty() ? null : String.join(". ", parts);
	}

	static String article(String name)
	{
		String lower = name.toLowerCase(Locale.ROOT);
		return ("aeiou".indexOf(lower.charAt(0)) >= 0 ? "an " : "a ") + lower;
	}

	static String capital(String s)
	{
		return s.isEmpty() ? s : Character.toUpperCase(s.charAt(0)) + s.substring(1);
	}
}
