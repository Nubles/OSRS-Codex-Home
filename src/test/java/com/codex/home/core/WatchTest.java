package com.codex.home.core;

import java.util.Arrays;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import org.junit.Test;

/**
 * The On watch list: Boss Pilot's charges, Autopilot's Hunter rumour, Minigame Pilot's Tempoross
 * games and rift points, and Sailing. Every fixture was written by the writer's own code from its
 * main branch (RumourBook.toJson, ChargeBook.toJson, StormGames.encode), so a change of shape there
 * fails here.
 */
public class WatchTest
{
	static final long NOW = HomeCoreTest.NOW;

	static Map<String, String> suite()
	{
		Map<String, String> saved = new HashMap<>();
		saved.put("codexautopilot/rumourV1", HomeCoreTest.fixture("autopilot-rumourV1.json"));
		saved.put("codexbosspilot/chargesV1", HomeCoreTest.fixture("boss-pilot-chargesV1.json"));
		saved.put("codexminigamepilot/tempoross.games", HomeCoreTest.fixture("minigame-pilot-tempoross.games.txt"));
		saved.put("codexminigamepilot/rift.points", "41:34");
		return saved;
	}

	static Shared read(Map<String, String> saved)
	{
		return HomeCoreTest.read(saved, NOW);
	}

	@Test
	public void readsAutopilotsRumour()
	{
		Shared s = read(suite());
		assertEquals("Cervus", s.rumour.master);
		assertEquals("Grey chinchompa", s.rumour.creature);
		assertFalse(s.rumour.part);
		assertEquals(12, s.rumour.done);

		Map<String, String> saved = suite();
		saved.put("codexautopilot/rumourV1", HomeCoreTest.fixture("autopilot-rumourV1-part.json"));
		assertTrue(read(saved).rumour.part);

		assertEquals("Sabre-toothed kyatt", Shared.creatureName("SABRE_TOOTHED_KYATT"));
		assertEquals("Moonlight antelope", Shared.creatureName("MOONLIGHT_ANTELOPE"));
		for (String bad : new String[]{null, "", "{", "[]", "{\"version\":2,\"creature\":\"GREY_CHINCHOMPA\"}", "{\"version\":1}",
			"{\"version\":1,\"creature\":\"not a key\",\"done\":-1}"})
		{
			saved.put("codexautopilot/rumourV1", bad);
			assertNull(bad, read(saved).rumour);
		}
	}

	@Test
	public void readsBossPilotsChargesFewestKillsFirst()
	{
		List<Shared.Charge> c = read(suite()).charges;
		assertEquals(3, c.size());
		Shared.Charge swamp = c.get(0);
		assertEquals("Trident of the swamp", swamp.name);
		assertEquals(300, swamp.left);
		assertEquals(6.0, swamp.kills, 1e-9);
		assertEquals("Zulrah", swamp.boss);
		assertTrue(swamp.estimated());
		Shared.Charge pipe = c.get(1);
		assertEquals("Toxic blowpipe", pipe.name);
		// Darts run out before scales: 2,400 at 100 a kill against 1,640 at 60.
		assertEquals("darts", pipe.pool);
		assertEquals(24.0, pipe.kills, 1e-9);
		Shared.Charge helm = c.get(2);
		assertEquals("Serpentine helm", helm.name);
		assertEquals("scales", helm.pool);
		assertEquals(-1, helm.kills, 0);
		assertEquals(11000, helm.max);
		assertFalse(helm.estimated());

		assertTrue(Watch.low(swamp));
		assertFalse(Watch.low(pipe));
		// 800 of 11,000 scales is under a tenth.
		assertTrue(Watch.low(helm));
	}

	@Test
	public void brokenChargesAreNoCharges()
	{
		Map<String, String> saved = suite();
		for (String bad : new String[]{null, "", "[", "{}", "{\"version\":2,\"items\":{}}", "{\"version\":1,\"items\":{\"x\":{}}}",
			"{\"version\":1,\"items\":{\"sang\":{\"checked\":[-1]}}}"})
		{
			saved.put("codexbosspilot/chargesV1", bad);
			assertTrue(bad, read(saved).charges.isEmpty());
		}
		// An item Home does not know still shows, by its key.
		saved.put("codexbosspilot/chargesV1", "{\"version\":1,\"items\":{\"blazing-pipe\":{\"checked\":[50],\"kills\":0}}}");
		Shared.Charge c = read(saved).charges.get(0);
		assertEquals("Blazing pipe", c.name);
		assertEquals(-1, c.max);
		assertFalse(Watch.low(c));
	}

	@Test
	public void readsMinigamePilotsTemporossAndRift()
	{
		Shared s = read(suite());
		assertEquals(3, s.storm.games);
		assertEquals(3, s.storm.permits);
		assertEquals(3450, s.storm.points);
		assertTrue(s.storm.won);
		// 5 permits over 3,000 ticks of 0.6 s: 10 an hour, as Minigame Pilot says it.
		assertEquals(10.0, s.storm.permitsPerHour, 1e-9);
		assertEquals(34, s.rift.searches());
		assertTrue(s.has(Shared.MINIGAME_PILOT));
		assertEquals("Tempoross: 3 permits last game", Status.of(App.MINIGAME_PILOT, s, NOW));

		Map<String, String> saved = new HashMap<>();
		for (String bad : new String[]{"", "1", "2;1,1,1,1,1", "1;x,1,1,1,1", "1;1,1,1,1,7"})
		{
			saved.put("codexminigamepilot/tempoross.games", bad);
			assertNull(bad, read(saved).storm);
		}
		for (String bad : new String[]{"", "4", "-1:3", "a:b", "1:2:3"})
		{
			saved.put("codexminigamepilot/rift.points", bad);
			assertNull(bad, read(saved).rift);
		}
		assertFalse(read(saved).has(Shared.MINIGAME_PILOT));
		saved.put("codexminigamepilot/rift.points", "7:9");
		assertEquals("Rift: 7 searches banked", Status.of(App.MINIGAME_PILOT, read(saved), NOW));
	}

	@Test
	public void theListPutsWhatNeedsDoingFirst()
	{
		Shared s = read(suite());
		List<Watch.Item> w = Watch.of(s, new Sailing(47, 80_000, 312), NOW);
		StringBuilder keys = new StringBuilder();
		for (Watch.Item i : w)
		{
			keys.append(i.key).append(' ');
		}
		// Two low items (the blowpipe is fine, so it is not listed), then the rift's searches, then the rest.
		assertEquals("charges:swamp charges:serp rift rumour tempoross sailing ", keys.toString());
		assertEquals("Trident of the swamp: ~6 kills left", w.get(0).title);
		assertEquals("~300 charges, at your Zulrah pace. Recharge before your next trip", w.get(0).detail);
		assertEquals(Watch.Tone.ALERT, w.get(0).tone);
		assertEquals("Serpentine helm: 800 scales", w.get(1).title);
		assertEquals("34 searches banked", w.get(2).title);
		assertEquals("41 elemental, 34 catalytic points; catalytic is 7 behind", w.get(2).detail);
		assertEquals("Hunt a grey chinchompa", w.get(3).title);
		assertEquals("For Cervus; 12 rumours done", w.get(3).detail);
		assertEquals("Last game: 3 permits", w.get(4).title);
		assertTrue(w.get(4).detail, w.get(4).detail.startsWith("3,450 points, 2 hr ago; 10.0 permits an hour over 3 games"));
		assertEquals("Trident of the swamp: ~6 kills left. Serpentine helm: 800 scales", Watch.reminder(w));
		assertEquals("Trident of the swamp running low", Status.of(App.BOSS_PILOT, s, NOW));
	}

	@Test
	public void aCalmBookShowsItsLowestItem()
	{
		Map<String, String> saved = new HashMap<>();
		saved.put("codexbosspilot/chargesV1", "{\"version\":1,\"items\":{\"shadow\":{\"checked\":[15000],\"at\":5,\"kills\":0,\"used\":[0.0]},"
			+ "\"bowfa\":{\"checked\":[9000],\"at\":5,\"kills\":0,\"used\":[0.0]}}}");
		List<Watch.Item> w = Watch.of(read(saved), null, NOW);
		assertEquals(1, w.size());
		assertEquals("Bow of faerdhinen: 9,000 charges", w.get(0).title);
		assertEquals(Watch.Tone.STEADY, w.get(0).tone);
		assertEquals(0.45, w.get(0).progress, 1e-9);
		assertNull(Watch.reminder(w));
	}

	@Test
	public void aRumourPartToHandInIsAnAlert()
	{
		Map<String, String> saved = new HashMap<>();
		saved.put("codexautopilot/rumourV1", HomeCoreTest.fixture("autopilot-rumourV1-part.json"));
		List<Watch.Item> w = Watch.of(read(saved), null, NOW);
		assertEquals("Hand in the rare part", w.get(0).title);
		assertEquals(Watch.Tone.ALERT, w.get(0).tone);
		assertEquals("Hand in the rare part", Watch.reminder(w));
		saved.put("codexautopilot/rumourV1", "{\"version\":1,\"part\":false,\"done\":40}");
		w = Watch.of(read(saved), null, NOW);
		assertEquals("No rumour held", w.get(0).title);
		assertEquals("Ask a guild hunter for your next; 40 rumours done", w.get(0).detail);
	}

	@Test
	public void sailingProgressAndPortTaskSlots()
	{
		// Levels from RuneLite's own experience table.
		Sailing s = new Sailing(47, net.runelite.api.Experience.getXpForLevel(47), 312);
		assertEquals(0, s.progress(), 1e-9);
		assertEquals(net.runelite.api.Experience.getXpForLevel(48) - net.runelite.api.Experience.getXpForLevel(47), s.toNext());
		assertEquals(3, s.slots());
		assertEquals(56, s.nextSlotLevel());
		// OSRS Wiki, Port tasks: one slot at 1, then 7, 28, 56 and 84.
		assertEquals(Arrays.asList(1, 1, 2, 2, 3, 3, 4, 4, 5, 5), Arrays.asList(Sailing.slots(1), Sailing.slots(6), Sailing.slots(7),
			Sailing.slots(27), Sailing.slots(28), Sailing.slots(55), Sailing.slots(56), Sailing.slots(83), Sailing.slots(84), Sailing.slots(99)));
		Sailing top = new Sailing(99, 13_034_431, 0);
		assertEquals(1, top.progress(), 0);
		assertEquals(-1, top.nextSlotLevel());
		assertEquals(-1, top.portTasks);
		Watch.Item item = Watch.of(Shared.NONE, s, NOW).get(0);
		assertTrue(item.title, item.title.startsWith("Level 47, "));
		assertEquals("312 port tasks done; you can hold 3 at once, 4 from level 56", item.detail);
		assertNull(item.from);
		assertTrue(Watch.of(Shared.NONE, null, NOW).isEmpty());
	}

	@Test
	public void wayfinderFeedsTheAppsThatDrawItsRoutes()
	{
		// Nomad, Ironworks and Provisioner read codexwayfinder/atlasV1 (their core/Travel.java) to draw Wayfinder's routes.
		assertEquals(Arrays.asList(App.AUTOPILOT, App.NOMAD, App.IRONWORKS, App.PROVISIONER), Link.TRAVEL.getTo());
		Map<App, Roster.State> states = new EnumMap<>(App.class);
		states.put(App.WAYFINDER, Roster.State.ON);
		states.put(App.IRONWORKS, Roster.State.ON);
		states.put(App.PROVISIONER, Roster.State.OFF);
		assertEquals(Arrays.asList(App.IRONWORKS, App.PROVISIONER), Link.TRAVEL.users(new Roster(states)));
	}
}
