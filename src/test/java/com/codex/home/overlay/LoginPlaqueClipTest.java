package com.codex.home.overlay;

import com.codex.home.core.Greeting;
import com.codex.home.core.Shared;
import java.awt.AlphaComposite;
import java.awt.Composite;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.geom.AffineTransform;
import java.awt.image.BufferedImage;
import java.util.Collections;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import org.junit.Test;

/**
 * RuneLite draws every overlay with one Graphics2D and its own clip. A plaque that clears the clip
 * (setClip(null)) or leaves its font, transform or composite changed breaks the overlays drawn after
 * it, so the login plaque must hand the graphics back exactly as it got them.
 */
public class LoginPlaqueClipTest
{
	static
	{
		System.setProperty("java.awt.headless", "true");
	}

	@Test
	public void renderLeavesTheClipAndStateAsTheyWere()
	{
		BufferedImage img = new BufferedImage(400, 200, BufferedImage.TYPE_INT_ARGB);
		Graphics2D g = img.createGraphics();
		Rectangle clip = new Rectangle(7, 9, 300, 120);
		g.setClip(clip);
		Font font = new Font(Font.DIALOG, Font.PLAIN, 11);
		g.setFont(font);
		AffineTransform transform = new AffineTransform(g.getTransform());
		Composite composite = g.getComposite();

		LoginPlaque plaque = new LoginPlaque();
		Greeting greeting = Greeting.of(Shared.NONE, null,
			Collections.singletonList(Greeting.line("Start Shield of Arrav towards Dragon Slayer II", Greeting.Tone.LEAD)));
		// Fading: the last moments draw with a composite of their own.
		for (long until : new long[]{System.currentTimeMillis() + 10_000, System.currentTimeMillis() + 300})
		{
			plaque.show(greeting, until);
			Dimension d = plaque.render(g);
			assertNotNull(d);
			assertEquals(clip, g.getClip().getBounds());
			assertEquals(font, g.getFont());
			assertEquals(transform, g.getTransform());
			assertEquals(composite, g.getComposite());
		}
		assertEquals(AlphaComposite.SrcOver, g.getComposite());
		g.dispose();
	}

	@Test
	public void paintForScreenshotsAlsoLeavesTheClip()
	{
		BufferedImage img = new BufferedImage(400, 200, BufferedImage.TYPE_INT_ARGB);
		Graphics2D g = img.createGraphics();
		Rectangle clip = new Rectangle(0, 0, 50, 50);
		g.setClip(clip);
		LoginPlaque.paint(g, Greeting.of(Shared.NONE, null,
			Collections.singletonList(Greeting.line("Worth 48.2M", Greeting.Tone.PLAIN))));
		assertEquals(clip, g.getClip().getBounds());
		g.dispose();
	}
}
