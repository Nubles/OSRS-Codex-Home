package com.codex.home.ui;

import com.codex.home.core.App;
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.FontMetrics;
import java.awt.GradientPaint;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RadialGradientPaint;
import java.awt.geom.Ellipse2D;
import javax.swing.JComponent;

/**
 * An app's wax seal: a small roundel in its family's colour carrying the app's two letters.
 * A seal for an app you do not have is drawn unlit, as an empty ring.
 */
public class Seal extends JComponent
{
	private final App app;
	private final boolean lit;

	public Seal(App app, boolean lit, int size)
	{
		this.app = app;
		this.lit = lit;
		setOpaque(false);
		Dimension d = new Dimension(size, size);
		setPreferredSize(d);
		setMinimumSize(d);
		setMaximumSize(d);
	}

	public static Color colour(App.Family family)
	{
		switch (family)
		{
			case PLAN:
				return Theme.TEAL;
			case WEALTH:
				return Theme.GOLD;
			case STORY:
				return Theme.VIOLET;
			case GUIDES:
				return Theme.GREEN;
			default:
				return Theme.EMBER;
		}
	}

	@Override
	protected void paintComponent(Graphics graphics)
	{
		Graphics2D g = (Graphics2D) graphics.create();
		Theme.smooth(g);
		paint(g, app, lit, 0, 0, Math.min(getWidth(), getHeight()));
		g.dispose();
	}

	static void paint(Graphics2D g, App app, boolean lit, int x, int y, int d)
	{
		Color c = colour(app.getFamily());
		Ellipse2D disc = new Ellipse2D.Float(x + 1, y + 1, d - 2, d - 2);
		if (lit)
		{
			g.setPaint(new RadialGradientPaint(x + d * 0.4f, y + d * 0.32f, d * 0.75f, new float[]{0, 1},
				new Color[]{Theme.mix(c, Color.WHITE, 0.12), Theme.mix(c, Theme.INK_DEEP, 0.62)}));
			g.fill(disc);
			g.setStroke(new BasicStroke(1.2f));
			g.setPaint(new GradientPaint(x, y, Theme.mix(c, Color.WHITE, 0.35), x, y + d, Theme.mix(c, Theme.INK_DEEP, 0.4)));
			g.draw(disc);
		}
		else
		{
			g.setColor(Theme.INK_DEEP);
			g.fill(disc);
			g.setStroke(new BasicStroke(1f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND, 1f, new float[]{2f, 2f}, 0));
			g.setColor(Theme.alpha(c, 120));
			g.draw(disc);
		}
		g.setFont(Theme.small());
		FontMetrics fm = g.getFontMetrics();
		String s = app.getSeal();
		int tx = x + (d - fm.stringWidth(s)) / 2 + 1;
		int ty = y + (d + fm.getAscent()) / 2 - 2;
		if (lit)
		{
			Ui.shadowed(g, s, tx, ty, Theme.mix(Theme.TEXT, Color.WHITE, 0.3));
		}
		else
		{
			g.setColor(Theme.alpha(c, 150));
			g.drawString(s, tx, ty);
		}
	}
}
