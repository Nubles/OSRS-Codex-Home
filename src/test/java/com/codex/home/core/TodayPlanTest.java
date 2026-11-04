package com.codex.home.core;

import java.util.Arrays;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.Map;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import org.junit.Test;

/** Today's plan from Codex Autopilot, and the apps added with this: Wayfinder, Money Pilot and Boss Pilot. */
public class TodayPlanTest
{
	static final long NOW = HomeCoreTest.NOW;

	static Shared read(Map<String, String> saved)
	{
		return HomeCoreTest.read(saved, NOW);
	}

	/**
	 * Pins {@code codexautopilot/planV1}. The file is Autopilot's own output: the same file sits in
	 * osrs-codex-autopilot's test resources, where a test checks Autopilot still writes exactly it.
	 */
	@Test
	public void readsAutopilotsPlanAsItWritesIt()
	{
		Map<String, String> saved = new HashMap<>();
		saved.put("codexautopilot/planV1", HomeCoreTest.fixture("autopilot-planV1.json"));
		Shared s = read(saved);
		assertEquals(4, s.plan.size());
		Shared.Step first = s.plan.get(0);
		assertEquals("chores:herb_run", first.key);
		assertEquals("WASTING", first.tier);
		assertEquals("Herb run", first.over);
		assertEquals("4 ready, 1 diseased", first.title);
		assertEquals("Herb at Catherby is diseased: cure it within 38 min or it dies", first.why);
		assertEquals(12, first.minutes);
		assertEquals("via Ardougne cloak, ~40s", first.travel);
		assertEquals("Herb run: 4 ready, 1 diseased", first.headline());
		assertEquals("Zaff's battlestaves", s.plan.get(1).title);
		assertEquals("via Varrock Teleport (from the bank), ~2 min", s.plan.get(1).travel);
		assertNull(s.plan.get(2).travel);
		assertEquals("FOCUS", s.plan.get(2).tier);
		assertEquals(1_790_099_000_000L, s.planSavedAt);
		assertEquals(Arrays.asList("Wayfinder", "Navigator", "Money Pilot", "Ledger"), s.fedBy);
		assertTrue(s.has(Shared.AUTOPILOT));
		assertEquals("Next: Herb run: 4 ready, 1 diseased", Status.of(App.AUTOPILOT, s, NOW));
	}

	@Test
	public void aPlanHomeCannotReadIsNoPlan()
	{
		for (String bad : new String[]{null, "", "{", "[]", "{\"version\":2,\"steps\":[]}", "{\"version\":1}", "{\"version\":1,\"steps\":{}}"})
		{
			Map<String, String> saved = new HashMap<>();
			saved.put("codexautopilot/planV1", bad);
			Shared s = read(saved);
			assertNull(bad, s.plan);
			assertFalse(s.has(Shared.AUTOPILOT));
			assertNull(Status.of(App.AUTOPILOT, s, NOW));
		}
		// An empty plan is a plan: Autopilot is caught up. A step without a title is left out.
		Map<String, String> saved = new HashMap<>();
		saved.put("codexautopilot/planV1", "{\"version\":1,\"savedAt\":5,\"steps\":[{\"key\":\"x\"},{\"title\":\"Do it\",\"minutes\":\"lots\"}]}");
		Shared s = read(saved);
		assertEquals(1, s.plan.size());
		assertEquals(0, s.plan.get(0).minutes);
		assertNull(s.plan.get(0).over);
		assertEquals("Do it", s.plan.get(0).headline());
		assertTrue(s.fedBy.isEmpty());
	}

	@Test
	public void newAppsAreOnTheListAndNotYetOnTheHub()
	{
		for (App app : new App[]{App.WAYFINDER, App.MONEY_PILOT, App.BOSS_PILOT})
		{
			assertEquals(app + " is not on the Plugin Hub yet", App.Hub.SOON, app.getHub());
			assertFalse(app.installable());
		}
		assertEquals(App.WAYFINDER, App.byPluginName("Codex Wayfinder"));
		assertEquals(App.MONEY_PILOT, App.byPluginName("Codex Money Pilot"));
		assertEquals(App.BOSS_PILOT, App.byPluginName("Codex Boss Pilot"));
		assertEquals(App.Family.PLAN, App.WAYFINDER.getFamily());
		assertEquals(App.Family.WEALTH, App.MONEY_PILOT.getFamily());
		assertEquals(App.Family.GUIDES, App.BOSS_PILOT.getFamily());
		// Not installable, so never suggested.
		for (Advice.Pick p : Advice.picks(HomeCoreTest.roster(App.AUTOPILOT, App.NAVIGATOR), Account.MAIN))
		{
			assertFalse(p.app.getHub() == App.Hub.SOON);
		}
	}

	@Test
	public void theirLinksFlowIntoAutopilot()
	{
		Map<String, String> saved = new HashMap<>();
		// Codex Wayfinder's shared atlas, Money Pilot's clocked runs and Boss Pilot's pin, as each saves them.
		saved.put("codexwayfinder/atlasV1", "{\"version\":1,\"savedAt\":1,\"walkSecondsPerTile\":0.3,\"teleports\":[{\"name\":\"Varrock Teleport\","
			+ "\"source\":\"Varrock Teleport\",\"kind\":\"SPELL\",\"x\":3213,\"y\":3424,\"plane\":0,\"seconds\":5,\"carried\":true,\"timed\":false}],"
			+ "\"networks\":{},\"timedTrips\":0}");
		saved.put("codexmoneypilot/runs", "[{\"m\":\"vorkath\",\"t\":1,\"ms\":3600000,\"gp\":3000000},{\"m\":\"zulrah\",\"t\":2,\"ms\":3600000,\"gp\":2000000}]");
		saved.put("codexbosspilot/pinned", "kril");
		Shared s = read(saved);
		assertEquals(1, s.teleports);
		assertEquals(2, s.moneyRuns);
		assertEquals("K'ril Tsutsaroth", s.boss);
		assertEquals("1 teleport in your atlas", Status.of(App.WAYFINDER, s, NOW));
		assertEquals("2 runs clocked", Status.of(App.MONEY_PILOT, s, NOW));
		assertEquals("Pinned: K'ril Tsutsaroth", Status.of(App.BOSS_PILOT, s, NOW));

		Map<App, Roster.State> states = new EnumMap<>(App.class);
		states.put(App.WAYFINDER, Roster.State.ON);
		states.put(App.MONEY_PILOT, Roster.State.ON);
		states.put(App.BOSS_PILOT, Roster.State.OFF);
		states.put(App.AUTOPILOT, Roster.State.ON);
		Roster r = new Roster(states);
		assertEquals(Link.State.FLOWING, Link.TRAVEL.state(r, s));
		assertEquals(Link.State.FLOWING, Link.EARNINGS.state(r, s));
		// Switched off, but what it shared still reaches Autopilot.
		assertEquals(Link.State.FLOWING, Link.BOSSES.state(r, s));
		assertEquals(Arrays.asList(App.AUTOPILOT), Link.TRAVEL.users(r));
		Shared none = read(new HashMap<>());
		assertEquals(Link.State.WAITING, Link.TRAVEL.state(r, none));
		assertEquals(Link.State.MISSING, Link.TRAVEL.state(Roster.EMPTY, none));
		// Autopilot now reads Ledger's worth, Quartermaster's kits and Gear Roadmap's goal.
		assertTrue(Link.WORTH.getTo().contains(App.AUTOPILOT));
		assertTrue(Link.KITS.getTo().contains(App.AUTOPILOT));
		assertTrue(Link.GEAR.getTo().contains(App.AUTOPILOT));
		// No runs yet is nothing shared.
		saved.put("codexmoneypilot/runs", "[]");
		assertFalse(read(saved).has(Shared.MONEY_PILOT));
	}
}
