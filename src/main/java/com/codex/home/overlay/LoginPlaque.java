package com.codex.home.overlay;

import com.codex.home.core.Greeting;
import com.codex.home.ui.Sigil;
import com.codex.home.ui.Theme;
import com.codex.home.ui.Ui;
import java.awt.AlphaComposite;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.FontMetrics;
import java.awt.GradientPaint;
import java.awt.Graphics2D;
import java.awt.Shape;
import java.awt.geom.RoundRectangle2D;
import java.util.ArrayList;
import java.util.List;
import net.runelite.client.ui.overlay.Overlay;
import net.runelite.client.ui.overlay.OverlayMenuEntry;
import net.runelite.client.ui.overlay.OverlayPosition;
import net.runelite.api.MenuAction;

/**
 * The plaque Home shows over the game for a few seconds after login, in place of a chat line:
 * your next step in gilt letters, your worth and anything that needs doing before the next trip.
 * It fades away by itself, and right-click "Dismiss" takes it away at once.
 */
public class LoginPlaque extends Overlay
{
	public static final String DISMISS = "Dismiss";
	public static final String TARGET = "Codex login plaque";
	private static final int PAD = 8;
	private static final int MIN_WIDTH = 190;
	private static final int MAX_WIDTH = 260;
	private static final long FADE_MS = 800;

	private volatile Greeting shown;
	private volatile long hideAt;

	public LoginPlaque()
	{
		setPosition(OverlayPosition.TOP_LEFT);
		setPriority(PRIORITY_HIGH);
		getMenuEntries().add(new OverlayMenuEntry(MenuAction.RUNELITE_OVERLAY, DISMISS, TARGET));
	}

	/** Shows a greeting until a moment, or hides the plaque with null. */
	public void show(Greeting greeting, long until)
	{
		hideAt = until;
		shown = greeting;
	}

	public void hide()
	{
		shown = null;
	}

	/** The greeting on show, or null. */
	public Greeting getShown()
	{
		Greeting g = shown;
		return g != null && System.currentTimeMillis() < hideAt ? g : null;
	}

	@Override
	public Dimension render(Graphics2D g)
	{
		Greeting greeting = shown;
		long left = hideAt - System.currentTimeMillis();
		if (greeting == null || left <= 0)
		{
			return null;
		}
		Graphics2D g2 = (Graphics2D) g.create();
		if (left < FADE_MS)
		{
			g2.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, Math.max(0f, left / (float) FADE_MS)));
		}
		Dimension d = paint(g2, greeting);
		g2.dispose();
		return d;
	}

	/**
	 * Draws the plaque at the origin and answers its size; also used for screenshots. It draws on a
	 * copy of the graphics, so the caller's clip, font, paint and hints are left as they were.
	 */
	public static Dimension paint(Graphics2D graphics, Greeting greeting)
	{
		Graphics2D g = (Graphics2D) graphics.create();
		try
		{
			return draw(g, greeting);
		}
		finally
		{
			g.dispose();
		}
	}

	private static Dimension draw(Graphics2D g, Greeting greeting)
	{
		Theme.smooth(g);
		g.setFont(Theme.title());
		FontMetrics bold = g.getFontMetrics();
		g.setFont(Theme.small());
		FontMetrics small = g.getFontMetrics();

		int icon = 16;
		int textX = PAD + 4;
		int want = textX + icon + 6 + small.stringWidth(greeting.kicker);
		for (Greeting.Line l : greeting.lines)
		{
			want = Math.max(want, textX + (l.tone == Greeting.Tone.LEAD ? bold : small).stringWidth(l.text) + PAD);
		}
		int width = Math.max(MIN_WIDTH, Math.min(MAX_WIDTH, want + PAD));
		int room = width - textX - PAD;
		List<String[]> wrapped = new ArrayList<>();
		int height = PAD + icon + 4;
		for (Greeting.Line l : greeting.lines)
		{
			FontMetrics fm = l.tone == Greeting.Tone.LEAD ? bold : small;
			String[] w = wrap(l.text, fm, room, 2);
			wrapped.add(w);
			height += w.length * fm.getHeight();
		}
		height += PAD;

		Shape shape = new RoundRectangle2D.Float(0.5f, 0.5f, width - 1, height - 1, 10, 10);
		g.setPaint(new GradientPaint(0, 0, Theme.alpha(Theme.CARD_TOP, 238), 0, height, Theme.alpha(Theme.INK_DEEP, 238)));
		g.fill(shape);
		// Narrow the clip to the plaque and put the old one back after: never clear it.
		Shape old = g.getClip();
		g.clip(shape);
		g.setPaint(new GradientPaint(0, 0, Theme.GOLD, 0, height, Theme.alpha(Theme.GOLD, 70)));
		g.fillRect(0, 0, 3, height);
		g.setClip(old);
		g.setColor(Theme.alpha(Theme.GOLD, 150));
		g.draw(shape);
		g.setColor(Theme.alpha(Color.WHITE, 14));
		g.drawLine(6, 1, width - 6, 1);

		Sigil.paint(g, textX, PAD - 1, icon);
		g.setFont(Theme.small());
		Ui.shadowed(g, greeting.kicker, textX + icon + 6, PAD + 11, Theme.GOLD_DIM);

		int y = PAD + icon + 4;
		for (int i = 0; i < greeting.lines.size(); i++)
		{
			Greeting.Line l = greeting.lines.get(i);
			boolean lead = l.tone == Greeting.Tone.LEAD;
			FontMetrics fm = lead ? bold : small;
			g.setFont(lead ? Theme.title() : Theme.small());
			for (String part : wrapped.get(i))
			{
				y += fm.getHeight();
				Ui.shadowed(g, part, textX, y - fm.getDescent(), colour(l.tone));
			}
		}
		return new Dimension(width, height);
	}

	static Color colour(Greeting.Tone tone)
	{
		switch (tone)
		{
			case LEAD:
				return Theme.GOLD_BRIGHT;
			case ALERT:
				return Theme.EMBER;
			case NEWS:
				return Theme.TEAL;
			default:
				return Theme.TEXT;
		}
	}

	static String[] wrap(String text, FontMetrics fm, int width, int maxLines)
	{
		List<String> out = new ArrayList<>();
		StringBuilder line = new StringBuilder();
		String[] words = text.split(" ");
		for (String word : words)
		{
			String trial = line.length() == 0 ? word : line + " " + word;
			if (fm.stringWidth(trial) > width && line.length() > 0)
			{
				out.add(line.toString());
				line = new StringBuilder(word);
				if (out.size() == maxLines)
				{
					out.set(maxLines - 1, Ui.clip(out.get(maxLines - 1) + " ...", fm, width));
					return out.toArray(new String[0]);
				}
			}
			else
			{
				line = new StringBuilder(trial);
			}
		}
		if (line.length() > 0)
		{
			out.add(Ui.clip(line.toString(), fm, width));
		}
		return out.toArray(new String[0]);
	}
}
