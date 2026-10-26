package com.codex.home.ui;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.GradientPaint;
import java.awt.Graphics2D;
import java.awt.RadialGradientPaint;
import java.awt.geom.Ellipse2D;
import java.awt.geom.Line2D;
import java.awt.geom.Path2D;
import java.awt.image.BufferedImage;

/**
 * The Home mark: the codex itself, an open book with gilt-edged pages, and a teal stone set above
 * its spine where every app's thread meets.
 */
public final class Sigil
{
	private Sigil()
	{
	}

	public static BufferedImage icon(int size)
	{
		BufferedImage image = new BufferedImage(size, size, BufferedImage.TYPE_INT_ARGB);
		Graphics2D g = image.createGraphics();
		Theme.smooth(g);
		paint(g, 0, 0, size);
		g.dispose();
		return image;
	}

	/** Draws the mark in a size by size box. */
	public static void paint(Graphics2D g, float x, float y, float size)
	{
		float s = size / 16f;
		float cx = x + 8 * s;

		// The two pages, rising a little toward the spine like a book lying open.
		Path2D left = new Path2D.Float();
		left.moveTo(cx, y + 6.2f * s);
		left.curveTo(cx - 2.2f * s, y + 4.6f * s, cx - 4.8f * s, y + 4.6f * s, x + 1.2f * s, y + 5.6f * s);
		left.lineTo(x + 1.2f * s, y + 13.4f * s);
		left.curveTo(cx - 4.8f * s, y + 12.4f * s, cx - 2.2f * s, y + 12.6f * s, cx, y + 14.2f * s);
		left.closePath();
		Path2D right = new Path2D.Float();
		right.moveTo(cx, y + 6.2f * s);
		right.curveTo(cx + 2.2f * s, y + 4.6f * s, cx + 4.8f * s, y + 4.6f * s, x + 14.8f * s, y + 5.6f * s);
		right.lineTo(x + 14.8f * s, y + 13.4f * s);
		right.curveTo(cx + 4.8f * s, y + 12.4f * s, cx + 2.2f * s, y + 12.6f * s, cx, y + 14.2f * s);
		right.closePath();

		Color vellum = Theme.mix(Theme.TEXT, Theme.GOLD, 0.35f);
		g.setPaint(new GradientPaint(x, y + 5 * s, vellum, cx, y + 14 * s, Theme.mix(vellum, Theme.GOLD_DIM, 0.55)));
		g.fill(left);
		g.setPaint(new GradientPaint(x + 16 * s, y + 5 * s, Theme.mix(vellum, Theme.GOLD_DIM, 0.25), cx, y + 14 * s,
			Theme.mix(vellum, Theme.GOLD_DIM, 0.7)));
		g.fill(right);

		// Lines of script on each page.
		g.setStroke(new BasicStroke(Math.max(0.6f, 0.55f * s), BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
		g.setColor(Theme.alpha(Theme.INK_DEEP, 120));
		for (int i = 0; i < 3; i++)
		{
			float ly = y + (8.0f + i * 1.9f) * s;
			g.draw(new Line2D.Float(x + 3.0f * s, ly, cx - 1.6f * s, ly + 0.5f * s));
			g.draw(new Line2D.Float(cx + 1.6f * s, ly + 0.5f * s, x + 13.0f * s, ly));
		}

		// Gilt edges and the spine.
		g.setStroke(new BasicStroke(Math.max(0.9f, 0.9f * s), BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
		g.setPaint(new GradientPaint(x, y, Theme.GOLD_BRIGHT, x, y + size, Theme.GOLD_DIM));
		g.draw(left);
		g.draw(right);
		g.setColor(Theme.alpha(Theme.INK_DEEP, 200));
		g.draw(new Line2D.Float(cx, y + 6.4f * s, cx, y + 14.0f * s));

		// The stone above the spine.
		float pr = Math.max(1.0f, 1.6f * s);
		float py = y + 2.6f * s;
		g.setPaint(new RadialGradientPaint(cx - pr * 0.3f, py - pr * 0.4f, pr * 1.6f, new float[]{0, 1},
			new Color[]{Theme.mix(Theme.TEAL, Color.WHITE, 0.45), Theme.mix(Theme.TEAL, Theme.INK_DEEP, 0.35)}));
		Path2D stone = new Path2D.Float();
		stone.moveTo(cx, py - pr);
		stone.lineTo(cx + pr, py);
		stone.lineTo(cx, py + pr);
		stone.lineTo(cx - pr, py);
		stone.closePath();
		g.fill(stone);
		g.setStroke(new BasicStroke(Math.max(0.4f, 0.4f * s)));
		g.setColor(Theme.INK_DEEP);
		g.draw(stone);
		g.setColor(Theme.alpha(Theme.TEAL, 60));
		g.fill(new Ellipse2D.Float(cx - pr * 2.2f, py - pr * 2.2f, pr * 4.4f, pr * 4.4f));
	}
}
