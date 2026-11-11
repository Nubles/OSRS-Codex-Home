package com.codex.home.core;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import org.junit.Test;

/** The login plaque that took the place of the old chat line. */
public class GreetingTest
{
	static final long NOW = HomeCoreTest.NOW;

	@Test
	public void saysTheNextStepWorthAndWhatNeedsDoing()
	{
		Map<String, String> saved = HomeCoreTest.everything();
		saved.put("codexautopilot/rumourV1", HomeCoreTest.fixture("autopilot-rumourV1-part.json"));
		Shared s = HomeCoreTest.read(saved, NOW);
		Greeting g = Greeting.of(s, Watch.of(s, null, NOW));
		assertEquals("CODEX HOME", g.kicker);
		assertEquals(Greeting.Tone.LEAD, g.lines.get(0).tone);
		assertEquals("Start Shield of Arrav", g.lines.get(0).text);
		assertEquals("Towards Dragon Slayer II", g.lines.get(1).text);
		assertEquals(Greeting.Tone.ALERT, g.lines.get(g.lines.size() - 1).tone);
		assertEquals("Hand in the rare part", g.lines.get(g.lines.size() - 1).text);
	}

	@Test
	public void worthAloneStillGreets()
	{
		Map<String, String> saved = new HashMap<>();
		saved.put("codexledger/ledger", "{\"total\":48250000,\"history\":[{\"t\":" + (NOW - 9 * Shared.DAY_MS)
			+ ",\"v\":44100000}]}");
		Greeting g = Greeting.of(HomeCoreTest.read(saved, NOW), null);
		assertEquals("Worth 48.2M (+4.1M this week)", g.text());
	}

	@Test
	public void nothingSharedMeansNoPlaque()
	{
		assertNull(Greeting.of(Shared.NONE, null));
	}

	@Test
	public void extraLinesComeFirst()
	{
		List<Greeting.Line> extra = Arrays.asList(Greeting.line("Welcome back: away 47 days", Greeting.Tone.NEWS));
		Greeting g = Greeting.of(Shared.NONE, null, extra);
		assertEquals("Welcome back: away 47 days", g.text());
	}
}
