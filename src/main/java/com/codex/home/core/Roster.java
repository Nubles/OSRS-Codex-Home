package com.codex.home.core;

import java.util.EnumMap;
import java.util.Map;

/** Which Codex apps this client has installed, and which of those are switched on. */
public final class Roster
{
	public enum State
	{
		ON,
		OFF,
		MISSING
	}

	public static final Roster EMPTY = new Roster(new EnumMap<>(App.class));

	private final Map<App, State> states;

	public Roster(Map<App, State> states)
	{
		this.states = new EnumMap<>(App.class);
		this.states.putAll(states);
	}

	public State state(App app)
	{
		return states.getOrDefault(app, State.MISSING);
	}

	public boolean installed(App app)
	{
		return state(app) != State.MISSING;
	}

	public boolean on(App app)
	{
		return state(app) == State.ON;
	}

	public int count(State state)
	{
		int n = 0;
		for (App app : App.all())
		{
			if (state(app) == state)
			{
				n++;
			}
		}
		return n;
	}

	public int installedCount()
	{
		return App.all().size() - count(State.MISSING);
	}

	@Override
	public boolean equals(Object o)
	{
		return o instanceof Roster && ((Roster) o).states.equals(states);
	}

	@Override
	public int hashCode()
	{
		return states.hashCode();
	}
}
