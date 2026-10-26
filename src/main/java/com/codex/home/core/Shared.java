package com.codex.home.core;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;
import java.util.Map;

/**
 * What the other Codex apps have shared on this account, read from the keys they publish for one
 * another. Home only ever reads these; it never writes another app's settings. Each part is null
 * when its app is missing, has not saved yet, or saved something Home cannot make sense of, so a
 * broken or future save shows as "nothing yet" rather than a wrong figure.
 */
public final class Shared
{
	public static final String NAVIGATOR = "codexnavigator";
	public static final String LEDGER = "codexledger";
	public static final String CHRONICLE = "codexsessionchronicle";
	public static final String STEWARD = "codexbanksteward";
	public static final String QUARTERMASTER = "codexquartermaster";
	public static final String ROADMAP = "codexgearroadmap";
	public static final String GUARDIAN = "codexguardian";
	public static final String NOMAD = "codexnomad";

	static final long DAY_MS = 24L * 60 * 60 * 1000;
	static final long WEEK_MS = 7 * DAY_MS;

	/** Reads one saved value for the current account, or null. */
	public interface Source
	{
		String get(String group, String key);
	}

	/** The goal pinned as Navigator's focus, or its first open goal. */
	public static final class Focus
	{
		public final String title;
		/** Navigator's next step towards it, or null. */
		public final String next;
		public final double progress;
		/** Navigator's estimate of hours left, or a negative number when it has none. */
		public final double hoursLeft;
		public final int open;
		public final int done;

		Focus(String title, String next, double progress, double hoursLeft, int open, int done)
		{
			this.title = title;
			this.next = next;
			this.progress = progress;
			this.hoursLeft = hoursLeft;
			this.open = open;
			this.done = done;
		}
	}

	public static final class Worth
	{
		public final long total;
		/** How far worth moved over the last seven days, or since Ledger started if that is sooner. */
		public final long change;
		/** How many days the change covers, at most seven. */
		public final int days;

		Worth(long total, long change, int days)
		{
			this.total = total;
			this.change = change;
			this.days = days;
		}
	}

	public static final class Session
	{
		public final long endedAt;
		public final long playedMs;
		/** What the session was mostly spent on, such as "Vorkath", or null. */
		public final String activity;
		public final long xp;
		/** Worth gained or lost, when Chronicle knew it. */
		public final Long gp;
		public final int sessions;

		Session(long endedAt, long playedMs, String activity, long xp, Long gp, int sessions)
		{
			this.endedAt = endedAt;
			this.playedMs = playedMs;
			this.activity = activity;
			this.xp = xp;
			this.gp = gp;
			this.sessions = sessions;
		}
	}

	public static final class Bank
	{
		public final long savedAt;
		/** How many kinds of item it holds. */
		public final int kinds;

		Bank(long savedAt, int kinds)
		{
			this.savedAt = savedAt;
			this.kinds = kinds;
		}
	}

	public static final class Kit
	{
		/** The chosen kit's name, or null when none is chosen yet. */
		public final String name;
		/** Whole trips the bank covers, or -1 when Quartermaster has not counted them. */
		public final int trips;
		public final int kits;

		Kit(String name, int trips, int kits)
		{
			this.name = name;
			this.trips = trips;
			this.kits = kits;
		}

		public boolean chosen()
		{
			return name != null;
		}
	}

	public final Focus focus;
	public final Worth worth;
	public final Session session;
	public final Bank bank;
	public final Kit kit;
	/** What Gear Roadmap is saving towards, or null. */
	public final String gearGoal;
	/** Whether Gear Roadmap has shared the gear in your bank. */
	public final boolean gearBank;
	public final boolean oneLife;
	/** How many kinds of item Nomad tracks across an Ultimate Ironman's stores, or -1. */
	public final int nomadKinds;

	private Shared(Focus focus, Worth worth, Session session, Bank bank, Kit kit, String gearGoal, boolean gearBank, boolean oneLife,
		int nomadKinds)
	{
		this.focus = focus;
		this.worth = worth;
		this.session = session;
		this.bank = bank;
		this.kit = kit;
		this.gearGoal = gearGoal;
		this.gearBank = gearBank;
		this.oneLife = oneLife;
		this.nomadKinds = nomadKinds;
	}

	public static final Shared NONE = new Shared(null, null, null, null, null, null, false, false, -1);

	public static Shared read(Source source, Gson gson, long now)
	{
		return new Shared(
			focus(parse(gson, source.get(NAVIGATOR, "goalBoard"))),
			worth(parse(gson, source.get(LEDGER, "ledger")), now),
			session(parse(gson, source.get(CHRONICLE, "journal"))),
			bank(parse(gson, source.get(STEWARD, "bankV1"))),
			kit(parse(gson, source.get(QUARTERMASTER, "kitsV1"))),
			gearGoal(parse(gson, source.get(ROADMAP, "goal"))),
			gearBank(parse(gson, source.get(ROADMAP, "bank"))),
			"true".equals(source.get(GUARDIAN, "oneLifeAccount")),
			nomad(parse(gson, source.get(NOMAD, "holdingsV1"))));
	}

	/** Whether the app that publishes a group has shared anything Home could read. */
	public boolean has(String group)
	{
		switch (group)
		{
			case NAVIGATOR:
				// An empty board shares nothing yet: no goal is pinned.
				return focus != null && focus.open + focus.done > 0;
			case LEDGER:
				return worth != null;
			case CHRONICLE:
				return session != null;
			case STEWARD:
				return bank != null;
			case QUARTERMASTER:
				return kit != null;
			case ROADMAP:
				return gearBank || gearGoal != null;
			case GUARDIAN:
				return oneLife;
			case NOMAD:
				return nomadKinds >= 0;
			default:
				return false;
		}
	}

	static JsonElement parse(Gson gson, String json)
	{
		if (json == null || json.isEmpty())
		{
			return null;
		}
		try
		{
			return gson.fromJson(json, JsonElement.class);
		}
		catch (RuntimeException e)
		{
			return null;
		}
	}

	static Focus focus(JsonElement e)
	{
		JsonObject o = object(e);
		if (o == null || num(o, "version", 1) > 1)
		{
			// A breaking change to the board gets a higher version; read it as no board.
			return null;
		}
		JsonArray goals = array(o, "goals");
		if (goals == null)
		{
			return null;
		}
		String focusKey = str(o, "focus");
		JsonObject pick = null;
		int open = 0;
		int done = 0;
		for (JsonElement g : goals)
		{
			JsonObject goal = object(g);
			if (goal == null || str(goal, "title") == null)
			{
				continue;
			}
			if (bool(goal, "complete"))
			{
				done++;
				continue;
			}
			open++;
			if (pick == null || (focusKey != null && focusKey.equals(str(goal, "key"))
				&& !focusKey.equals(str(pick, "key"))))
			{
				pick = goal;
			}
		}
		if (pick == null)
		{
			return open + done == 0 ? new Focus(null, null, 0, -1, 0, 0) : new Focus(null, null, 1, -1, 0, done);
		}
		double hours = num(pick, "hoursLeft", -1);
		return new Focus(str(pick, "title"), str(pick, "next"), clamp(num(pick, "progress", 0)),
			Double.isFinite(hours) ? hours : -1, open, done);
	}

	static Worth worth(JsonElement e, long now)
	{
		JsonObject o = object(e);
		if (o == null)
		{
			return null;
		}
		long total = (long) num(o, "total", 0);
		if (total <= 0)
		{
			return null;
		}
		JsonArray history = array(o, "history");
		long weekAgo = now - WEEK_MS;
		long baseT = -1;
		long baseV = 0;
		long firstT = Long.MAX_VALUE;
		long firstV = 0;
		if (history != null)
		{
			for (JsonElement h : history)
			{
				JsonObject point = object(h);
				if (point == null)
				{
					continue;
				}
				long t = (long) num(point, "t", -1);
				long v = (long) num(point, "v", 0);
				if (t <= 0 || v <= 0)
				{
					continue;
				}
				if (t < firstT)
				{
					firstT = t;
					firstV = v;
				}
				if (t <= weekAgo && t > baseT)
				{
					baseT = t;
					baseV = v;
				}
			}
		}
		if (baseT < 0)
		{
			if (firstT == Long.MAX_VALUE)
			{
				return new Worth(total, 0, 0);
			}
			baseT = firstT;
			baseV = firstV;
		}
		int days = (int) Math.max(0, Math.min(7, (now - Math.max(baseT, weekAgo)) / DAY_MS));
		return new Worth(total, total - baseV, days);
	}

	static Session session(JsonElement e)
	{
		if (e == null || !e.isJsonArray())
		{
			return null;
		}
		JsonObject last = null;
		long lastStart = Long.MIN_VALUE;
		int sessions = 0;
		for (JsonElement s : e.getAsJsonArray())
		{
			JsonObject o = object(s);
			if (o == null)
			{
				continue;
			}
			sessions++;
			long start = (long) num(o, "startedAt", 0);
			if (start > lastStart)
			{
				lastStart = start;
				last = o;
			}
		}
		if (last == null)
		{
			return null;
		}
		long ended = (long) num(last, "endedAt", 0);
		if (ended <= 0)
		{
			ended = (long) num(last, "lastSeenAt", lastStart);
		}
		long xp = 0;
		JsonObject skills = last.has("skills") ? object(last.get("skills")) : null;
		if (skills != null)
		{
			for (Map.Entry<String, JsonElement> skill : skills.entrySet())
			{
				JsonObject sk = object(skill.getValue());
				if (sk != null)
				{
					xp += Math.max(0, (long) num(sk, "xp", 0) - (long) num(sk, "startXp", 0));
				}
			}
		}
		Long gp = null;
		if (last.has("worthStart") && last.has("worthNow"))
		{
			long start = (long) num(last, "worthStart", 0);
			long end = (long) num(last, "worthNow", 0);
			if (start > 0 && end > 0)
			{
				gp = end - start;
			}
		}
		return new Session(ended, Math.max(0, (long) num(last, "playedMs", 0)), str(last, "activity"), xp, gp, sessions);
	}

	static Bank bank(JsonElement e)
	{
		JsonObject o = object(e);
		if (o == null || num(o, "version", 1) > 1)
		{
			return null;
		}
		JsonObject items = o.has("items") ? object(o.get("items")) : null;
		long savedAt = (long) num(o, "savedAt", 0);
		if (items == null || savedAt <= 0)
		{
			return null;
		}
		return new Bank(savedAt, items.size());
	}

	static Kit kit(JsonElement e)
	{
		JsonObject o = object(e);
		if (o == null || num(o, "version", 1) > 1)
		{
			return null;
		}
		JsonArray kits = array(o, "kits");
		if (kits == null)
		{
			return null;
		}
		// The current Quartermaster leaves "active" out until a kit is chosen.
		String active = str(o, "active");
		for (JsonElement k : kits)
		{
			JsonObject kit = object(k);
			if (kit != null && active != null && active.equals(str(kit, "id")))
			{
				return new Kit(str(kit, "name"), (int) num(kit, "tripsLeft", -1), kits.size());
			}
		}
		return new Kit(null, -1, kits.size());
	}

	static String gearGoal(JsonElement e)
	{
		JsonObject o = object(e);
		String name = o == null ? null : str(o, "name");
		return name == null || name.trim().isEmpty() ? null : name.trim();
	}

	static boolean gearBank(JsonElement e)
	{
		JsonObject o = object(e);
		return o != null && array(o, "items") != null;
	}

	static int nomad(JsonElement e)
	{
		JsonObject o = object(e);
		JsonObject items = o != null && o.has("items") ? object(o.get("items")) : null;
		return items == null ? -1 : items.size();
	}

	private static JsonObject object(JsonElement e)
	{
		return e != null && e.isJsonObject() ? e.getAsJsonObject() : null;
	}

	private static JsonArray array(JsonObject o, String key)
	{
		JsonElement e = o.get(key);
		return e != null && e.isJsonArray() ? e.getAsJsonArray() : null;
	}

	private static String str(JsonObject o, String key)
	{
		JsonElement e = o.get(key);
		if (e == null || !e.isJsonPrimitive() || !e.getAsJsonPrimitive().isString())
		{
			return null;
		}
		String s = e.getAsString();
		return s.isEmpty() ? null : s;
	}

	private static double num(JsonObject o, String key, double fallback)
	{
		JsonElement e = o.get(key);
		if (e == null || !e.isJsonPrimitive())
		{
			return fallback;
		}
		JsonPrimitive p = e.getAsJsonPrimitive();
		if (!p.isNumber())
		{
			return fallback;
		}
		double d = p.getAsDouble();
		return Double.isNaN(d) ? fallback : d;
	}

	private static boolean bool(JsonObject o, String key)
	{
		JsonElement e = o.get(key);
		return e != null && e.isJsonPrimitive() && e.getAsJsonPrimitive().isBoolean() && e.getAsBoolean();
	}

	private static double clamp(double v)
	{
		return Math.max(0, Math.min(1, v));
	}
}
