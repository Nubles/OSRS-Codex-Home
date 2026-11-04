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
	public static final String AUTOPILOT = "codexautopilot";
	public static final String WAYFINDER = "codexwayfinder";
	public static final String MONEY_PILOT = "codexmoneypilot";
	public static final String BOSS_PILOT = "codexbosspilot";
	public static final String MINIGAME_PILOT = "codexminigamepilot";

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

	/**
	 * One of Codex Autopilot's top steps, from {@code codexautopilot/planV1}:
	 * {@code {"version":1,"savedAt":ms,"steps":[{"key","kind","tier","over","title","why","minutes","travel","itemId"}],"fedBy":[...]}}.
	 */
	public static final class Step
	{
		public final String key;
		/** WASTING, READY, FOCUS or GOAL. */
		public final String tier;
		/** The small line above, such as "Herb run" or "Toward Twisted bow". */
		public final String over;
		public final String title;
		public final String why;
		/** Minutes, the journey there included, or 0 when not given. */
		public final int minutes;
		/** How to get there, such as "via Ardougne cloak, ~40s", or null. */
		public final String travel;

		Step(String key, String tier, String over, String title, String why, int minutes, String travel)
		{
			this.key = key;
			this.tier = tier;
			this.over = over;
			this.title = title;
			this.why = why;
			this.minutes = minutes;
			this.travel = travel;
		}

		/** "Herb run: 4 ready". */
		public String headline()
		{
			return over == null ? title : over + ": " + title;
		}
	}

	/**
	 * The Hunter Guild rumour as Codex Autopilot last read it, from {@code codexautopilot/rumourV1}:
	 * {@code {"version":1,"master":"CERVUS","creature":"GREY_CHINCHOMPA","part":false,"done":12,"seenAt":ms,...}}.
	 */
	public static final class Rumour
	{
		/** The guild hunter who gave it, by name, or null. */
		public final String master;
		/** The creature to hunt, by name, or null when no rumour is held. */
		public final String creature;
		/** Whether the rare part has been found: it waits to be handed in. */
		public final boolean part;
		/** Rumours completed as the game last said, or -1. */
		public final int done;
		public final long seenAt;

		Rumour(String master, String creature, boolean part, int done, long seenAt)
		{
			this.master = master;
			this.creature = creature;
			this.part = part;
			this.done = done;
			this.seenAt = seenAt;
		}
	}

	/**
	 * One charged item as Codex Boss Pilot last knew it, from {@code codexbosspilot/chargesV1}: the
	 * pool that runs out first, what is thought left of it and, when Boss Pilot has learnt the
	 * player's own pace at the boss it was used at, the kills that is good for.
	 */
	public static final class Charge
	{
		public final String key;
		/** "Toxic blowpipe". */
		public final String name;
		/** "darts", "scales", "charges". */
		public final String pool;
		public final long left;
		/** The most the item holds, or -1 for an item Home does not know. */
		public final int max;
		/** Kills since the game last said: the count is an estimate when above zero. */
		public final int killsSince;
		/** Kills it is good for at the player's own pace, or -1 when Boss Pilot has learnt none. */
		public final double kills;
		/** The boss the kills were at, by name, or null. */
		public final String boss;
		public final long checkedAt;

		Charge(String key, String name, String pool, long left, int max, int killsSince, double kills, String boss, long checkedAt)
		{
			this.key = key;
			this.name = name;
			this.pool = pool;
			this.left = left;
			this.max = max;
			this.killsSince = killsSince;
			this.kills = kills;
			this.boss = boss;
			this.checkedAt = checkedAt;
		}

		/** Whether what is left is Home's guess from the kills since, not what the game said. */
		public boolean estimated()
		{
			return killsSince > 0;
		}
	}

	/** Codex Minigame Pilot's Tempoross games, from {@code codexminigamepilot/tempoross.games}. */
	public static final class Storm
	{
		public final long endedAt;
		public final int points;
		public final int permits;
		/** False when the storm washed the player away. */
		public final boolean won;
		/** Games kept. */
		public final int games;
		/** Reward permits an hour over the timed games kept, or -1. */
		public final double permitsPerHour;

		Storm(long endedAt, int points, int permits, boolean won, int games, double permitsPerHour)
		{
			this.endedAt = endedAt;
			this.points = points;
			this.permits = permits;
			this.won = won;
			this.games = games;
			this.permitsPerHour = permitsPerHour;
		}
	}

	/** Guardians of the Rift points banked, from Codex Minigame Pilot's {@code codexminigamepilot/rift.points}. */
	public static final class Rift
	{
		public final int elemental;
		public final int catalytic;

		Rift(int elemental, int catalytic)
		{
			this.elemental = elemental;
			this.catalytic = catalytic;
		}

		/** Searches of the Rewards Guardian banked: each takes one point of each kind (OSRS Wiki, Rewards Guardian). */
		public int searches()
		{
			return Math.min(elemental, catalytic);
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
	/** Codex Autopilot's top steps, best first, or null when it has shared none. */
	public final java.util.List<Step> plan;
	/** When Autopilot saved them, or 0. */
	public final long planSavedAt;
	/** The Codex apps whose data is in Autopilot's plan. */
	public final java.util.List<String> fedBy;
	/** Teleports in Codex Wayfinder's shared atlas, or -1 when it has shared none. */
	public final int teleports;
	/** Money making runs Codex Money Pilot has clocked, or -1 when there are none to read. */
	public final int moneyRuns;
	/** The boss pinned in Codex Boss Pilot, by name, or null. */
	public final String boss;
	/** Autopilot's Hunter Guild rumour, or null when it has read none. */
	public final Rumour rumour;
	/** Boss Pilot's charged items, the fewest kills or least left first; empty when none are checked. */
	public final java.util.List<Charge> charges;
	/** Minigame Pilot's Tempoross games, or null with none. */
	public final Storm storm;
	/** Minigame Pilot's Guardians of the Rift points, or null when not known. */
	public final Rift rift;

	private Shared(Focus focus, Worth worth, Session session, Bank bank, Kit kit, String gearGoal, boolean gearBank, boolean oneLife,
		int nomadKinds)
	{
		this(focus, worth, session, bank, kit, gearGoal, gearBank, oneLife, nomadKinds, null, 0, java.util.Collections.emptyList(), -1, -1,
			null, null, java.util.Collections.emptyList(), null, null);
	}

	private Shared(Focus focus, Worth worth, Session session, Bank bank, Kit kit, String gearGoal, boolean gearBank, boolean oneLife,
		int nomadKinds, java.util.List<Step> plan, long planSavedAt, java.util.List<String> fedBy, int teleports, int moneyRuns, String boss,
		Rumour rumour, java.util.List<Charge> charges, Storm storm, Rift rift)
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
		this.plan = plan;
		this.planSavedAt = planSavedAt;
		this.fedBy = fedBy;
		this.teleports = teleports;
		this.moneyRuns = moneyRuns;
		this.boss = boss;
		this.rumour = rumour;
		this.charges = charges;
		this.storm = storm;
		this.rift = rift;
	}

	public static final Shared NONE = new Shared(null, null, null, null, null, null, false, false, -1);

	public static Shared read(Source source, Gson gson, long now)
	{
		JsonObject plan = planObject(parse(gson, source.get(AUTOPILOT, "planV1")));
		return new Shared(
			focus(parse(gson, source.get(NAVIGATOR, "goalBoard"))),
			worth(parse(gson, source.get(LEDGER, "ledger")), now),
			session(parse(gson, source.get(CHRONICLE, "journal"))),
			bank(parse(gson, source.get(STEWARD, "bankV1"))),
			kit(parse(gson, source.get(QUARTERMASTER, "kitsV1"))),
			gearGoal(parse(gson, source.get(ROADMAP, "goal"))),
			gearBank(parse(gson, source.get(ROADMAP, "bank"))),
			"true".equals(source.get(GUARDIAN, "oneLifeAccount")),
			nomad(parse(gson, source.get(NOMAD, "holdingsV1"))),
			plan(plan),
			plan == null ? 0 : (long) num(plan, "savedAt", 0),
			fedBy(plan),
			teleports(parse(gson, source.get(WAYFINDER, "atlasV1"))),
			runs(parse(gson, source.get(MONEY_PILOT, "runs"))),
			boss(source.get(BOSS_PILOT, "pinned")),
			rumour(parse(gson, source.get(AUTOPILOT, "rumourV1"))),
			charges(parse(gson, source.get(BOSS_PILOT, "chargesV1"))),
			storm(source.get(MINIGAME_PILOT, "tempoross.games")),
			rift(source.get(MINIGAME_PILOT, "rift.points")));
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
			case AUTOPILOT:
				return plan != null;
			case WAYFINDER:
				return teleports >= 0;
			case MONEY_PILOT:
				return moneyRuns > 0;
			case BOSS_PILOT:
				return boss != null;
			case MINIGAME_PILOT:
				return storm != null || rift != null;
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

	/** Autopilot's shared plan, or null when it is missing or a version this reader does not know. */
	static JsonObject planObject(JsonElement e)
	{
		JsonObject o = object(e);
		return o == null || num(o, "version", 1) != 1 || array(o, "steps") == null ? null : o;
	}

	/** Autopilot's steps, best first; null without a readable plan. A step without a title is left out. */
	static java.util.List<Step> plan(JsonObject o)
	{
		if (o == null)
		{
			return null;
		}
		java.util.List<Step> out = new java.util.ArrayList<>();
		for (JsonElement e : array(o, "steps"))
		{
			JsonObject s = object(e);
			if (s == null || str(s, "title") == null)
			{
				continue;
			}
			double minutes = num(s, "minutes", 0);
			out.add(new Step(str(s, "key"), str(s, "tier"), str(s, "over"), str(s, "title"), str(s, "why"),
				(int) Math.max(0, Math.min(10_000, Double.isFinite(minutes) ? minutes : 0)), str(s, "travel")));
		}
		return out;
	}

	static java.util.List<String> fedBy(JsonObject o)
	{
		java.util.List<String> out = new java.util.ArrayList<>();
		JsonArray a = o == null ? null : array(o, "fedBy");
		if (a != null)
		{
			for (JsonElement e : a)
			{
				if (e != null && e.isJsonPrimitive() && e.getAsJsonPrimitive().isString() && !e.getAsString().isEmpty())
				{
					out.add(e.getAsString());
				}
			}
		}
		return out;
	}

	/** Teleports in Wayfinder's atlas ({@code codexwayfinder/atlasV1}), or -1. */
	static int teleports(JsonElement e)
	{
		JsonObject o = object(e);
		JsonArray t = o == null || num(o, "version", 1) != 1 ? null : array(o, "teleports");
		return t == null ? -1 : t.size();
	}

	/** Runs in Money Pilot's list ({@code codexmoneypilot/runs}), or -1 when it is not a list. */
	static int runs(JsonElement e)
	{
		return e != null && e.isJsonArray() ? e.getAsJsonArray().size() : -1;
	}

	/** The boss pinned in Boss Pilot ({@code codexbosspilot/pinned}, its key as plain text), named for show. */
	static String boss(String key)
	{
		if (key == null || key.trim().isEmpty())
		{
			return null;
		}
		String k = key.trim();
		switch (k)
		{
			case "mole":
				return "Giant Mole";
			case "kq":
				return "Kalphite Queen";
			case "kbd":
				return "King Black Dragon";
			case "dks":
				return "Dagannoth Kings";
			case "thermy":
				return "Thermonuclear smoke devil";
			case "gg":
				return "Grotesque Guardians";
			case "graardor":
				return "General Graardor";
			case "zilyana":
				return "Commander Zilyana";
			case "kreearra":
				return "Kree'arra";
			case "kril":
				return "K'ril Tsutsaroth";
			default:
				return Character.toUpperCase(k.charAt(0)) + k.substring(1);
		}
	}

	// ---- Hunter rumour, charges and minigames ----

	/** Autopilot's rumour book, or null when it is missing, another version or holds nothing to show. */
	static Rumour rumour(JsonElement e)
	{
		JsonObject o = object(e);
		if (o == null || num(o, "version", -1) != 1)
		{
			return null;
		}
		String creature = creatureName(str(o, "creature"));
		int done = (int) Math.max(-1, num(o, "done", -1));
		if (creature == null && done < 0)
		{
			return null;
		}
		return new Rumour(enumName(str(o, "master")), creature, creature != null && bool(o, "part"), done,
			(long) Math.max(0, num(o, "seenAt", 0)));
	}

	/**
	 * A creature as Autopilot names it ({@code Rumour.Creature}), from its saved key. The few with
	 * hyphens are written out; any other is read from its key, so a creature added later still shows.
	 */
	static String creatureName(String key)
	{
		if (key == null)
		{
			return null;
		}
		switch (key)
		{
			case "BARB_TAILED_KEBBIT":
				return "Barb-tailed kebbit";
			case "RAZOR_BACKED_KEBBIT":
				return "Razor-backed kebbit";
			case "SABRE_TOOTHED_KEBBIT":
				return "Sabre-toothed kebbit";
			case "SABRE_TOOTHED_KYATT":
				return "Sabre-toothed kyatt";
			default:
				return enumName(key);
		}
	}

	/** "GREY_CHINCHOMPA" as "Grey chinchompa", or null when it is not a saved name. */
	static String enumName(String key)
	{
		if (key == null || !key.matches("[A-Z][A-Z0-9_]*"))
		{
			return null;
		}
		String s = key.replace('_', ' ').toLowerCase(java.util.Locale.ROOT);
		return Character.toUpperCase(s.charAt(0)) + s.substring(1);
	}

	/**
	 * The charged items Boss Pilot counts ({@code Charged}), by the key it saves them under: name and,
	 * pool by pool, what each holds and the most it can. Boss Pilot takes the limits from the OSRS
	 * Wiki; they are copied here only to say how full an item is.
	 */
	private static final String[][] CHARGED = {
		{"blowpipe", "Toxic blowpipe", "darts", "16383", "scales", "16383"},
		{"seas", "Trident of the seas", "charges", "2500"},
		{"seas-e", "Trident of the seas (e)", "charges", "20000"},
		{"swamp", "Trident of the swamp", "charges", "2500"},
		{"swamp-e", "Trident of the swamp (e)", "charges", "20000"},
		{"sang", "Sanguinesti staff", "charges", "20000"},
		{"holy-sang", "Holy sanguinesti staff", "charges", "20000"},
		{"shadow", "Tumeken's shadow", "charges", "20000"},
		{"bowfa", "Bow of faerdhinen", "charges", "20000"},
		{"serp", "Serpentine helm", "scales", "11000"},
		{"crystal-helm", "Crystal helm", "charges", "20000"},
		{"crystal-body", "Crystal body", "charges", "20000"},
		{"crystal-legs", "Crystal legs", "charges", "20000"},
	};

	/**
	 * Boss Pilot's charge book: {@code {"version":1,"items":{"blowpipe":{"checked":[300,2000],"at":ms,"kills":3,
	 * "used":[90.0,60.0],"blind":0,"boss":"zulrah"}},"paces":{"zulrah/blowpipe":{"perKill":[30.0,20.0],"samples":2}}}}.
	 * For each item: the pool that runs out first, by kills where a pace is learnt, else by how full it is.
	 */
	static java.util.List<Charge> charges(JsonElement e)
	{
		java.util.List<Charge> out = new java.util.ArrayList<>();
		JsonObject o = object(e);
		JsonObject items = o == null || num(o, "version", 1) != 1 || !o.has("items") ? null : object(o.get("items"));
		if (items == null)
		{
			return out;
		}
		JsonObject paces = o.has("paces") ? object(o.get("paces")) : null;
		for (Map.Entry<String, JsonElement> entry : items.entrySet())
		{
			JsonObject k = object(entry.getValue());
			JsonArray checked = k == null ? null : array(k, "checked");
			if (checked == null || checked.size() == 0)
			{
				continue;
			}
			String[] known = null;
			for (String[] c : CHARGED)
			{
				if (c[0].equals(entry.getKey()))
				{
					known = c;
				}
			}
			JsonArray used = array(k, "used");
			String bossKey = str(k, "boss");
			boolean oneBoss = bossKey != null && !"*".equals(bossKey);
			JsonArray perKill = null;
			if (oneBoss && paces != null && paces.has(bossKey + "/" + entry.getKey()))
			{
				JsonObject pace = object(paces.get(bossKey + "/" + entry.getKey()));
				perKill = pace == null ? null : array(pace, "perKill");
			}
			Charge pick = null;
			double pickRank = Double.MAX_VALUE;
			for (int i = 0; i < checked.size(); i++)
			{
				double said = numAt(checked, i, -1);
				if (said < 0)
				{
					continue;
				}
				double left = Math.max(0, said - Math.max(0, used == null ? 0 : numAt(used, i, 0)));
				double per = perKill == null || perKill.size() != checked.size() ? -1 : numAt(perKill, i, -1);
				double kills = per > 0 ? left / per : -1;
				String pool = known != null && known.length > 2 + i * 2 ? known[2 + i * 2] : "charges";
				int max = known != null && known.length > 3 + i * 2 ? Integer.parseInt(known[3 + i * 2]) : -1;
				// Rank pools by kills where known, else by how full they are.
				double rank = kills >= 0 ? kills : max > 0 ? 1_000_000 + left / max : 2_000_000 + left;
				if (rank < pickRank)
				{
					pickRank = rank;
					pick = new Charge(entry.getKey(), known != null ? known[1] : enumName(entry.getKey().toUpperCase(java.util.Locale.ROOT)
						.replace('-', '_')), pool, Math.round(Math.floor(left)), max, (int) Math.max(0, num(k, "kills", 0)), kills,
						oneBoss ? boss(bossKey) : null, (long) Math.max(0, num(k, "at", 0)));
				}
			}
			if (pick != null && pick.name != null)
			{
				out.add(pick);
			}
		}
		out.sort(java.util.Comparator.comparingDouble(Shared::chargeRank));
		return out;
	}

	/** Fewest kills first, then the emptiest. */
	static double chargeRank(Charge c)
	{
		if (c.kills >= 0)
		{
			return c.kills;
		}
		return c.max > 0 ? 1_000_000 + (double) c.left / c.max : 2_000_000 + c.left;
	}

	private static double numAt(JsonArray a, int i, double fallback)
	{
		if (i >= a.size())
		{
			return fallback;
		}
		JsonElement e = a.get(i);
		if (e == null || !e.isJsonPrimitive() || !e.getAsJsonPrimitive().isNumber())
		{
			return fallback;
		}
		double d = e.getAsDouble();
		return Double.isFinite(d) ? d : fallback;
	}

	/** Minigame Pilot's Tempoross games, "1;endedAt,ticks,points,permits,won;...", or null with none. */
	static Storm storm(String saved)
	{
		if (saved == null)
		{
			return null;
		}
		String[] parts = saved.split(";");
		if (parts.length < 2 || !"1".equals(parts[0].trim()))
		{
			return null;
		}
		long[] last = null;
		int games = 0;
		long ticks = 0;
		long permits = 0;
		for (int i = 1; i < parts.length; i++)
		{
			String[] f = parts[i].split(",");
			if (f.length < 5)
			{
				continue;
			}
			try
			{
				long[] g = {Long.parseLong(f[0].trim()), Long.parseLong(f[1].trim()), Long.parseLong(f[2].trim()),
					Long.parseLong(f[3].trim()), Long.parseLong(f[4].trim())};
				if (g[0] < 0 || g[1] < 0 || g[2] < 0 || g[3] < 0 || g[4] < 0 || g[4] > 1)
				{
					continue;
				}
				games++;
				if (g[1] > 0)
				{
					ticks += g[1];
					permits += g[3];
				}
				last = g;
			}
			catch (NumberFormatException e)
			{
				// Left out, as Minigame Pilot leaves it out.
			}
		}
		if (last == null)
		{
			return null;
		}
		// A game tick is 0.6 seconds (OSRS Wiki, Game tick).
		double perHour = ticks == 0 ? -1 : permits * 3600.0 / (ticks * 0.6);
		return new Storm(last[0], (int) last[2], (int) last[3], last[4] == 1, games, perHour);
	}

	/** Minigame Pilot's banked Guardians of the Rift points, "elemental:catalytic", or null. */
	static Rift rift(String saved)
	{
		if (saved == null)
		{
			return null;
		}
		String[] p = saved.split(":");
		if (p.length != 2)
		{
			return null;
		}
		try
		{
			int e = Integer.parseInt(p[0].trim());
			int c = Integer.parseInt(p[1].trim());
			return e < 0 || c < 0 ? null : new Rift(e, c);
		}
		catch (NumberFormatException ex)
		{
			return null;
		}
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
