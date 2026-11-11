package com.codex.home.core;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * What the login plaque says: your next step, how your worth moved and, when something needs doing
 * before the next trip, that too. Built from what the other Codex apps share. It is drawn over the
 * game for a few seconds after login; Home never writes in the chat box.
 */
public final class Greeting
{
	/** How a line is coloured. */
	public enum Tone
	{
		/** The headline: the next step. */
		LEAD,
		PLAIN,
		/** Do something about it before the next trip. */
		ALERT,
		/** Good news, such as a return after a long break or something new in the game. */
		NEWS
	}

	public static final class Line
	{
		public final String text;
		public final Tone tone;

		Line(String text, Tone tone)
		{
			this.text = text;
			this.tone = tone;
		}

		@Override
		public String toString()
		{
			return text;
		}
	}

	/** The small heading over the lines. */
	public final String kicker;
	public final List<Line> lines;

	private Greeting(String kicker, List<Line> lines)
	{
		this.kicker = kicker;
		this.lines = Collections.unmodifiableList(lines);
	}

	/** The plaque, or null when there is nothing worth showing. */
	public static Greeting of(Shared s, List<Watch.Item> watch)
	{
		return of(s, watch, null);
	}

	/**
	 * The plaque, or null when there is nothing worth showing.
	 *
	 * @param extra lines another part of Home adds in front, such as a welcome back; may be null
	 */
	public static Greeting of(Shared s, List<Watch.Item> watch, List<Line> extra)
	{
		List<Line> lines = new ArrayList<>();
		if (extra != null)
		{
			lines.addAll(extra);
		}
		if (s.focus != null && s.focus.title != null)
		{
			lines.add(new Line(s.focus.next != null ? s.focus.next : "Keep going: " + s.focus.title, Tone.LEAD));
			if (s.focus.next != null)
			{
				lines.add(new Line("Towards " + s.focus.title, Tone.PLAIN));
			}
		}
		if (s.worth != null)
		{
			String w = "Worth " + Format.number(s.worth.total);
			if (s.worth.days > 0 && s.worth.change != 0)
			{
				w += " (" + Status.signed(s.worth.change) + (s.worth.days >= 7 ? " this week)"
					: " in " + Format.plural(s.worth.days, "day", "days") + ")");
			}
			lines.add(new Line(w, Tone.PLAIN));
		}
		if (watch != null)
		{
			for (Watch.Item i : watch)
			{
				if (i.tone == Watch.Tone.ALERT)
				{
					lines.add(new Line(i.title, Tone.ALERT));
				}
			}
		}
		if (lines.isEmpty())
		{
			return null;
		}
		return new Greeting("CODEX HOME", lines);
	}

	/** A line for {@link #of(Shared, List, List)}'s extra lines. */
	public static Line line(String text, Tone tone)
	{
		return new Line(text, tone);
	}

	/** All the lines as one sentence each, for tests and tooltips. */
	public String text()
	{
		StringBuilder sb = new StringBuilder();
		for (Line l : lines)
		{
			sb.append(sb.length() == 0 ? "" : " | ").append(l.text);
		}
		return sb.toString();
	}

	@Override
	public String toString()
	{
		return text();
	}
}
