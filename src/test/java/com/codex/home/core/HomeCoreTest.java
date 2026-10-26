package com.codex.home.core;

import com.google.gson.Gson;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import org.junit.Test;

/**
 * Home's reading of what the other Codex apps share. The JSON files are byte copies of what each
 * writer saves today (the same copies its other readers keep), so a change to a writer's shape
 * that Home cannot follow fails here.
 */
public class HomeCoreTest
{
	static final Gson GSON = new Gson();
	/** A moment after every fixture was written. */
	static final long NOW = 1_790_100_000_000L;

	static String fixture(String name)
	{
		try (InputStream in = HomeCoreTest.class.getResourceAsStream(name))
		{
			assertNotNull(name, in);
			return new String(in.readAllBytes(), StandardCharsets.UTF_8);
		}
		catch (IOException e)
		{
			throw new AssertionError(e);
		}
	}

	static Shared read(Map<String, String> saved, long now)
	{
		return Shared.read((group, key) -> saved.get(group + "/" + key), GSON, now);
	}

	static Map<String, String> everything()
	{
		Map<String, String> saved = new HashMap<>();
		saved.put("codexnavigator/goalBoard", fixture("navigator-goalBoard.json"));
		saved.put("codexledger/ledger", fixture("ledger-seen.json"));
		saved.put("codexsessionchronicle/journal", fixture("chronicle-journal.json"));
		saved.put("codexbanksteward/bankV1", fixture("bank-steward-bankV1.json"));
		saved.put("codexquartermaster/kitsV1", fixture("quartermaster-saved-kitsV1.json"));
		return saved;
	}

	static Roster roster(App... on)
	{
		Map<App, Roster.State> states = new EnumMap<>(App.class);
		for (App app : on)
		{
			states.put(app, Roster.State.ON);
		}
		return new Roster(states);
	}

	@Test
	public void readsWhatEachWriterSavesToday()
	{
		Shared s = read(everything(), NOW);

		assertEquals("Dragon Slayer II", s.focus.title);
		assertEquals("Start Shield of Arrav", s.focus.next);
		assertEquals(109.5, s.focus.hoursLeft, 1e-9);
		assertTrue(s.focus.open >= 2);

		assertEquals(3_500_000, s.worth.total);
		// Both history points are within the week, so the change runs from the first of them.
		assertEquals(1_000_000, s.worth.change);
		assertEquals(1, s.worth.days);

		assertEquals(2, s.session.sessions);
		assertEquals("Mining", s.session.activity);
		assertEquals(3_600_000, s.session.playedMs);
		assertEquals(60_000, s.session.xp);
		assertNull("the Mining session predates worth tracking", s.session.gp);

		assertEquals(1_760_000_000_000L, s.bank.savedAt);
		assertEquals(6, s.bank.kinds);

		assertTrue(s.kit.chosen());
		assertEquals("Slayer \"Konar\"", s.kit.name);
		assertEquals(0, s.kit.trips);
	}

	@Test
	public void anUnchosenKitIsNotAChoice()
	{
		Map<String, String> saved = new HashMap<>();
		saved.put("codexquartermaster/kitsV1", fixture("quartermaster-unchosen-kitsV1.json"));
		Shared s = read(saved, NOW);
		assertNotNull(s.kit);
		assertFalse(s.kit.chosen());
		assertEquals("No kit chosen yet", Status.of(App.QUARTERMASTER, s, NOW));
	}

	@Test
	public void theNavigatorFocusWinsOverTheFirstGoal()
	{
		Map<String, String> saved = new HashMap<>();
		saved.put("codexnavigator/goalBoard", "{\"version\":1,\"focus\":\"diary:Varrock Hard\",\"goals\":["
			+ "{\"key\":\"quest:A\",\"title\":\"A\",\"complete\":false,\"progress\":0.5,\"next\":\"Do A\"},"
			+ "{\"key\":\"quest:B\",\"title\":\"B\",\"complete\":true,\"progress\":1},"
			+ "{\"key\":\"diary:Varrock Hard\",\"title\":\"Varrock Hard Diary\",\"complete\":false,\"progress\":0.25,\"next\":\"Do V\"}]}");
		Shared s = read(saved, NOW);
		assertEquals("Varrock Hard Diary", s.focus.title);
		assertEquals("Do V", s.focus.next);
		assertEquals(2, s.focus.open);
		assertEquals(1, s.focus.done);
		assertEquals(-1, s.focus.hoursLeft, 0);
		assertEquals("Varrock Hard Diary, 25%", Status.of(App.NAVIGATOR, s, NOW));
	}

	@Test
	public void finishedAndEmptyBoards()
	{
		Map<String, String> saved = new HashMap<>();
		saved.put("codexnavigator/goalBoard", "{\"version\":1,\"goals\":[{\"key\":\"q\",\"title\":\"Q\",\"complete\":true}]}");
		assertEquals("All 1 goal done", Status.of(App.NAVIGATOR, read(saved, NOW), NOW));
		saved.put("codexnavigator/goalBoard", "{\"version\":1,\"goals\":[]}");
		Shared empty = read(saved, NOW);
		assertNotNull("an empty board still shows Navigator is sharing", empty.focus);
		assertNull(empty.focus.title);
		assertEquals("No goals pinned", Status.of(App.NAVIGATOR, empty, NOW));
	}

	@Test
	public void aLaterVersionOfASharedShapeReadsAsNothing()
	{
		Map<String, String> saved = new HashMap<>();
		saved.put("codexnavigator/goalBoard", "{\"version\":2,\"goals\":[{\"key\":\"q\",\"title\":\"Q\"}]}");
		saved.put("codexbanksteward/bankV1", "{\"version\":2,\"savedAt\":5,\"items\":{\"995\":1}}");
		saved.put("codexquartermaster/kitsV1", "{\"version\":2,\"kits\":[]}");
		Shared s = read(saved, NOW);
		assertNull(s.focus);
		assertNull(s.bank);
		assertNull(s.kit);
	}

	@Test
	public void worthChangeLooksBackOneWeek()
	{
		long day = Shared.DAY_MS;
		Map<String, String> saved = new HashMap<>();
		saved.put("codexledger/ledger", "{\"total\":5000000,\"history\":["
			+ "{\"t\":" + (NOW - 30 * day) + ",\"v\":1000000},"
			+ "{\"t\":" + (NOW - 8 * day) + ",\"v\":4000000},"
			+ "{\"t\":" + (NOW - 2 * day) + ",\"v\":4800000}]}");
		Shared.Worth w = read(saved, NOW).worth;
		assertEquals(1_000_000, w.change);
		assertEquals(7, w.days);
		assertEquals("Worth 5.0M, +1.0M this week", Status.of(App.LEDGER, read(saved, NOW), NOW));

		saved.put("codexledger/ledger", "{\"total\":0,\"history\":[]}");
		assertNull("a fresh Ledger has nothing to say yet", read(saved, NOW).worth);
	}

	@Test
	public void brokenSavesNeverThrowAndReadAsNothing()
	{
		List<String> bad = Arrays.asList(null, "", "null", "[]", "{}", "{\"goals\":", "\"text\"", "42", "true",
			"{\"goals\":[null,1,\"x\",{\"title\":5}]}", "{\"total\":\"lots\",\"history\":[{\"t\":\"x\"}]}",
			"[null,{\"startedAt\":\"x\",\"skills\":[]}]", "{\"items\":[],\"savedAt\":-4}", "{\"kits\":{},\"active\":7}",
			"{\"name\":{}}", "{\"items\":{\"1\":\"x\"}}", "{\"total\":1e400}", "{\"progress\":NaN}");
		String[] keys = {"codexnavigator/goalBoard", "codexledger/ledger", "codexsessionchronicle/journal",
			"codexbanksteward/bankV1", "codexquartermaster/kitsV1", "codexgearroadmap/goal", "codexgearroadmap/bank",
			"codexguardian/oneLifeAccount", "codexnomad/holdingsV1"};
		for (String key : keys)
		{
			for (String value : bad)
			{
				Map<String, String> saved = new HashMap<>();
				saved.put(key, value);
				Shared s = read(saved, NOW);
				for (App app : App.all())
				{
					Status.of(app, s, NOW);
				}
				Advice.start(roster(App.values()), s);
			}
		}
	}

	@Test
	public void theSmallerShares()
	{
		Map<String, String> saved = new HashMap<>();
		saved.put("codexgearroadmap/goal", "{\"name\":\" Twisted bow \",\"ids\":[20997]}");
		saved.put("codexgearroadmap/bank", fixture("gear-roadmap-bank.json"));
		saved.put("codexguardian/oneLifeAccount", "true");
		saved.put("codexnomad/holdingsV1", "{\"version\":1,\"savedAt\":1,\"items\":{\"995\":5,\"385\":2},\"source\":\"nomad\"}");
		Shared s = read(saved, NOW);
		assertEquals("Saving for Twisted bow", Status.of(App.GEAR_ROADMAP, s, NOW));
		assertTrue(s.gearBank);
		assertEquals("Guarding a one-life account", Status.of(App.GUARDIAN, s, NOW));
		assertEquals("Tracking 2 kinds of item", Status.of(App.NOMAD, s, NOW));
		assertNull("Home has no line for an app that shares nothing", Status.of(App.ROUNDS, s, NOW));
	}

	@Test
	public void statusLinesReadNaturally()
	{
		Shared s = read(everything(), NOW);
		assertEquals("Worth 3.5M, +1.0M in 1 day", Status.of(App.LEDGER, s, NOW));
		assertEquals("Slayer \"Konar\" kit, 0 trips banked", Status.of(App.QUARTERMASTER, s, NOW));
		assertTrue(Status.of(App.SESSION_CHRONICLE, s, NOW).startsWith("Mining, 1 hr, "));
		assertEquals("just now", Status.ago(NOW - 5_000, NOW));
		assertEquals("4 min ago", Status.ago(NOW - 4 * 60_000, NOW));
		assertEquals("1 hr ago", Status.ago(NOW - 80 * 60_000, NOW));
		assertEquals("2 days ago", Status.ago(NOW - 51 * 3_600_000L, NOW));
		assertEquals("+1.2M", Status.signed(1_250_000));
		assertEquals("-840k", Status.signed(-840_500));
	}

	@Test
	public void numbersFollowTheSuiteRule()
	{
		assertEquals("0", Format.number(0));
		assertEquals("9,999", Format.number(9_999));
		assertEquals("10k", Format.number(10_000));
		assertEquals("999k", Format.number(999_999));
		assertEquals("1.0M", Format.number(1_000_000));
		assertEquals("1.2M", Format.number(1_299_999));
		assertEquals("999.9M", Format.number(999_999_999));
		assertEquals("2.14B", Format.number(2_147_483_647));
		assertEquals("-1.2M", Format.number(-1_200_000));
	}

	@Test
	public void firstStepsTickThemselves()
	{
		List<Advice.Step> none = Advice.start(Roster.EMPTY, Shared.NONE);
		assertEquals(5, none.size());
		assertEquals(0, Advice.done(none));
		assertEquals("Open your bank once with Bank Steward on", none.get(0).title());
		assertEquals("Add Codex Bank Steward from the Plugin Hub.", none.get(0).detail);

		Roster mine = roster(App.BANK_STEWARD, App.NAVIGATOR, App.QUARTERMASTER, App.LEDGER, App.SESSION_CHRONICLE,
			App.GEAR_ROADMAP);
		List<Advice.Step> all = Advice.start(mine, read(everything(), NOW));
		assertEquals(5, Advice.done(all));
		assertEquals("Done, shared with 3 of your apps.", all.get(0).detail);

		Map<App, Roster.State> states = new EnumMap<>(App.class);
		states.put(App.BANK_STEWARD, Roster.State.OFF);
		states.put(App.QUARTERMASTER, Roster.State.ON);
		List<Advice.Step> off = Advice.start(new Roster(states), Shared.NONE);
		assertEquals("Switch Codex Bank Steward on in RuneLite's plugin list.", off.get(0).detail);
		assertEquals("Add Codex Ledger from the Plugin Hub.", off.get(2).detail);
		assertFalse(off.get(4).done);
		assertEquals("1 of your apps will use it.", off.get(4).detail);
	}

	@Test
	public void picksSuitTheAccount()
	{
		List<Advice.Pick> fresh = Advice.picks(Roster.EMPTY, Account.MAIN);
		assertEquals(Arrays.asList(App.NAVIGATOR, App.AUTOPILOT, App.BANK_STEWARD), apps(fresh));

		List<Advice.Pick> uim = Advice.picks(Roster.EMPTY, Account.ULTIMATE);
		assertEquals(Arrays.asList(App.NOMAD, App.IRONWORKS, App.NAVIGATOR), apps(uim));

		// Guardian is not on the Hub yet, so a hardcore account is not sent looking for it.
		assertFalse(apps(Advice.picks(Roster.EMPTY, Account.HARDCORE)).contains(App.GUARDIAN));

		// Three apps that read the bank make Bank Steward the first thing to add.
		List<Advice.Pick> feeder = Advice.picks(roster(App.QUARTERMASTER, App.LEDGER, App.AUTOPILOT, App.NAVIGATOR), Account.MAIN);
		assertEquals(App.BANK_STEWARD, feeder.get(0).app);
		assertEquals("Shares your whole bank with 3 apps you already have.", feeder.get(0).reason);

		Roster everythingInstalled = roster(App.values());
		assertTrue(Advice.picks(everythingInstalled, Account.IRONMAN).isEmpty());
	}

	@Test
	public void theCatalogueIsConsistent()
	{
		Set<String> seals = new HashSet<>();
		Set<String> hub = new HashSet<>();
		for (App app : App.all())
		{
			assertTrue(app.getPluginName().startsWith("Codex "));
			assertTrue(app.getHubName().startsWith("codex-"));
			assertEquals(2, app.getSeal().length());
			assertTrue(app + " seal", seals.add(app.getSeal()));
			assertTrue(app + " hub name", hub.add(app.getHubName()));
			assertEquals(app, App.byPluginName(app.getPluginName()));
			assertTrue(app.getBlurb().endsWith("."));
		}
		assertNull(App.byPluginName("Codex Home"));
		assertEquals(19, App.all().size());
		for (Link link : Link.values())
		{
			assertFalse(link + " does not feed itself", link.getTo().contains(link.getFrom()));
		}
	}

	private static List<App> apps(List<Advice.Pick> picks)
	{
		App[] out = new App[picks.size()];
		for (int i = 0; i < out.length; i++)
		{
			out[i] = picks.get(i).app;
		}
		return Arrays.asList(out);
	}
}
