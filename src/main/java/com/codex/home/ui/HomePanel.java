package com.codex.home.ui;

import com.codex.home.core.Advice;
import com.codex.home.core.App;
import com.codex.home.core.Format;
import com.codex.home.core.Link;
import com.codex.home.core.Roster;
import com.codex.home.core.Shared;
import com.codex.home.core.Status;
import java.awt.BasicStroke;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.GradientPaint;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.GridLayout;
import java.awt.geom.Ellipse2D;
import java.awt.geom.Path2D;
import java.awt.geom.RoundRectangle2D;
import java.util.ArrayList;
import java.util.List;
import javax.swing.JComponent;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.ScrollPaneConstants;
import javax.swing.border.EmptyBorder;
import net.runelite.client.ui.PluginPanel;

/**
 * The Home side panel: a crest with how many Codex apps are working for you, then three views.
 * Today shows your next step, the account at a glance, the first steps that get the suite sharing
 * and the apps suited to you. Suite lists every app with what it is doing now. Links shows what
 * each app shares and who uses it.
 */
public class HomePanel extends PluginPanel
{
	static final int WIDTH = 200;
	static final int TEXT = WIDTH - 30;

	private final Actions actions;
	private final Crest crest = new Crest();
	private final JPanel tabs = new JPanel(new GridLayout(1, 0, 4, 0));
	private final JScrollPane scroll = new JScrollPane();
	private View view = View.empty();
	private Tab tab = Tab.TODAY;

	public HomePanel(Actions actions)
	{
		super(false);
		this.actions = actions;
		setLayout(new BorderLayout());
		setBackground(Theme.INK);
		JPanel top = new JPanel(new BorderLayout());
		top.setOpaque(false);
		top.add(crest, BorderLayout.NORTH);
		tabs.setOpaque(false);
		tabs.setBorder(new EmptyBorder(8, 8, 0, 8));
		top.add(tabs, BorderLayout.SOUTH);
		add(top, BorderLayout.NORTH);
		scroll.setBorder(null);
		scroll.setOpaque(false);
		scroll.getViewport().setOpaque(false);
		scroll.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
		scroll.getVerticalScrollBar().setUnitIncrement(16);
		scroll.getVerticalScrollBar().setPreferredSize(new Dimension(8, 0));
		add(scroll, BorderLayout.CENTER);
		rebuild(false);
	}

	/** Called on the Swing thread whenever anything shown changes. */
	public void update(View view)
	{
		this.view = view;
		rebuild(false);
	}

	/** Called on the Swing thread to switch view. */
	public void show(Tab tab)
	{
		boolean changed = this.tab != tab;
		this.tab = tab;
		rebuild(changed);
	}

	public Tab getTab()
	{
		return tab;
	}

	private void rebuild(boolean toTop)
	{
		updateCrest();
		tabs.removeAll();
		for (Tab t : Tab.values())
		{
			tabs.add(new CodexButton(t.getTitle(), t == tab, 20, () -> actions.tab(t)));
		}
		int y = toTop ? 0 : scroll.getVerticalScrollBar().getValue();
		scroll.setViewportView(build());
		scroll.getVerticalScrollBar().setValue(y);
		revalidate();
		repaint();
	}

	private void updateCrest()
	{
		Roster r = view.roster;
		int on = r.count(Roster.State.ON);
		int total = App.all().size();
		String status;
		String shortStatus;
		Color color;
		if (r.installedCount() == 0)
		{
			status = "No Codex apps yet: see Today";
			shortStatus = "No Codex apps yet";
			color = Theme.TEXT_MUTED;
		}
		else
		{
			String who = view.loggedIn ? view.account.title() : "Logged out";
			status = who + ", " + r.installedCount() + " of " + total + " installed";
			shortStatus = r.installedCount() + " of " + total + " installed";
			color = view.loggedIn ? Theme.TEAL : Theme.TEXT_MUTED;
		}
		crest.update("YOUR CODEX", on, "app on", "apps on", status, shortStatus, color, Theme.GOLD);
	}

	private StackPanel build()
	{
		switch (tab)
		{
			case SUITE:
				return suite();
			case LINKS:
				return links();
			default:
				return today();
		}
	}

	// ----- Today -----------------------------------------------------------------------------

	private StackPanel today()
	{
		StackPanel list = new StackPanel(6, 8);
		Shared s = view.shared;
		if (!view.loggedIn)
		{
			list.add(note("Log in and Home reads what your Codex apps have shared about this account: your next goal, "
				+ "your worth, your last session and your bank."));
		}
		else
		{
			JComponent plan = planSection(list);
			if (plan != null)
			{
				list.add(plan);
				list.space(2);
			}
			list.add(new SectionHeader("Up next", s.focus != null && s.focus.title != null && s.focus.hoursLeft >= 0
				? "~" + hours(s.focus.hoursLeft) : null));
			list.add(focusCard());
			list.space(2);
			list.add(new SectionHeader("At a glance"));
			list.add(glance());
		}

		if (view.loggedIn && view.startHere && Advice.done(view.steps) < view.steps.size())
		{
			list.space(2);
			list.add(new SectionHeader("Start here", Advice.done(view.steps) + " of " + view.steps.size()));
			for (Advice.Step step : view.steps)
			{
				list.add(new StepRow(step, TEXT - 16));
			}
		}

		if (!view.picks.isEmpty())
		{
			list.space(2);
			list.add(new SectionHeader(view.loggedIn ? "Suited to you" : "Good first apps"));
			for (Advice.Pick pick : view.picks)
			{
				list.add(pickCard(pick));
			}
		}
		return list;
	}

	/** Steps of Autopilot's plan shown on Today. */
	static final int PLAN_STEPS = 3;
	/** A plan older than this says when it was saved: Autopilot may be off. */
	static final long PLAN_STALE_MS = 30 * 60_000L;

	/**
	 * Codex Autopilot's top steps, as it shared them: adds the header and the rows to the list, and returns
	 * a note to add after them when there is no plan to show, or null.
	 */
	private JComponent planSection(StackPanel list)
	{
		Shared s = view.shared;
		boolean installed = view.roster.installed(App.AUTOPILOT);
		if (s.plan == null)
		{
			if (!installed)
			{
				return null;
			}
			list.add(new SectionHeader("Today's plan"));
			return note("Codex Autopilot's next three steps show here once it has planned for this account.");
		}
		long age = s.planSavedAt > 0 ? view.now - s.planSavedAt : 0;
		list.add(new SectionHeader("Today's plan", age > PLAN_STALE_MS ? "saved " + Status.ago(s.planSavedAt, view.now)
			: "from Autopilot"));
		if (s.plan.isEmpty())
		{
			return note("All caught up: nothing is ready and no goal has a next step.");
		}
		for (int i = 0; i < Math.min(PLAN_STEPS, s.plan.size()); i++)
		{
			list.add(planRow(i + 1, s.plan.get(i)));
		}
		if (!s.fedBy.isEmpty())
		{
			list.add(Ui.wrapped("Fed by " + String.join(", ", s.fedBy), Theme.small(), Theme.GOLD_DIM, TEXT + 14));
		}
		return null;
	}

	static Color tier(String tier)
	{
		if (tier == null)
		{
			return Theme.GOLD_DIM;
		}
		switch (tier)
		{
			case "WASTING":
				return Theme.EMBER;
			case "READY":
				return Theme.GREEN;
			case "FOCUS":
				return Theme.TEAL;
			default:
				return Theme.GOLD_DIM;
		}
	}

	private JComponent planRow(int place, Shared.Step step)
	{
		Color tint = tier(step.tier);
		Card card = new Card(new BorderLayout(0, 3)).accent(tint).highlighted(place == 1);
		card.setBorder(new EmptyBorder(6, 10, 6, 8));
		card.add(new PlanHead(place, step, tint), BorderLayout.NORTH);
		StackPanel body = new StackPanel(2, 0);
		body.add(Ui.wrapped(step.title, place == 1 ? Theme.title() : Theme.body(), place == 1 ? Theme.GOLD_BRIGHT : Theme.TEXT, TEXT));
		if (place == 1 && step.why != null)
		{
			body.add(Ui.wrapped(step.why, Theme.small(), Theme.TEXT_MUTED, TEXT));
		}
		if (step.travel != null)
		{
			body.add(Ui.wrapped(step.travel, Theme.small(), Theme.TEAL, TEXT));
		}
		card.add(body, BorderLayout.CENTER);
		card.setToolTipText("From Codex Autopilot's plan");
		return card;
	}

	/** A plan row's head: its place in a ring, what kind of step and how long. */
	static final class PlanHead extends JComponent
	{
		private final int place;
		private final Shared.Step step;
		private final Color tint;

		PlanHead(int place, Shared.Step step, Color tint)
		{
			this.place = place;
			this.step = step;
			this.tint = tint;
			setOpaque(false);
			setPreferredSize(new Dimension(10, 18));
		}

		@Override
		protected void paintComponent(Graphics graphics)
		{
			Graphics2D g = (Graphics2D) graphics.create();
			Theme.smooth(g);
			int w = getWidth();
			g.setFont(Theme.small());
			java.awt.FontMetrics fm = g.getFontMetrics();
			g.setColor(Theme.INK_DEEP);
			g.fill(new Ellipse2D.Float(0, 1, 15, 15));
			g.setColor(Theme.alpha(tint, 190));
			g.draw(new Ellipse2D.Float(0.5f, 1.5f, 14, 14));
			String n = String.valueOf(place);
			Ui.shadowed(g, n, 8 - fm.stringWidth(n) / 2, 13, Theme.GOLD_BRIGHT);
			String time = step.minutes <= 0 ? "" : step.minutes >= 60 ? "~1 hr" : "~" + step.minutes + " min";
			int tw = fm.stringWidth(time);
			Ui.shadowed(g, time, w - tw, 13, Theme.TEXT_FAINT);
			String over = step.over == null ? "" : step.over.toUpperCase();
			Ui.shadowed(g, clip(over, fm, w - 22 - tw - 6), 22, 13, Theme.GOLD_DIM);
			g.dispose();
		}

		private static String clip(String text, java.awt.FontMetrics fm, int width)
		{
			if (fm.stringWidth(text) <= width)
			{
				return text;
			}
			String t = text;
			while (t.length() > 1 && fm.stringWidth(t + "...") > width)
			{
				t = t.substring(0, t.length() - 1);
			}
			return t + "...";
		}
	}

	private JComponent focusCard()
	{
		Shared.Focus f = view.shared.focus;
		Card card = new Card(new BorderLayout(0, 5)).accent(Theme.TEAL).highlighted(f != null && f.title != null);
		StackPanel body = new StackPanel(3, 0);
		if (!view.roster.installed(App.NAVIGATOR))
		{
			body.add(Ui.wrapped("Your next step lives here", Theme.title(), Theme.GOLD_BRIGHT, TEXT));
			body.add(Ui.wrapped("Add Codex Navigator and pin a quest, diary or skill goal. Its next step shows here.",
				Theme.small(), Theme.TEXT, TEXT));
			card.add(body, BorderLayout.CENTER);
			card.onClick(() -> actions.open(App.NAVIGATOR));
			return card;
		}
		if (f == null || f.title == null)
		{
			boolean allDone = f != null && f.done > 0;
			body.add(Ui.wrapped(allDone ? "Every goal done" : "No goal pinned yet", Theme.title(), Theme.GOLD_BRIGHT, TEXT));
			body.add(Ui.wrapped(allDone ? "Pin another in Codex Navigator and its next step shows here."
				: "Pin a quest, diary or skill goal in Codex Navigator and its next step shows here.", Theme.small(), Theme.TEXT, TEXT));
			card.add(body, BorderLayout.CENTER);
			return card;
		}
		body.add(Ui.wrapped(f.next != null ? f.next : "Keep going", Theme.title(), Theme.GOLD_BRIGHT, TEXT));
		String towards = "Towards " + f.title;
		if (f.open > 1)
		{
			towards += ", and " + Format.plural(f.open - 1, "other goal", "other goals");
		}
		body.add(Ui.wrapped(towards, Theme.small(), Theme.TEXT_MUTED, TEXT));
		card.add(body, BorderLayout.CENTER);
		card.add(new Bar(f.progress), BorderLayout.SOUTH);
		return card;
	}

	private JComponent glance()
	{
		Shared s = view.shared;
		Roster r = view.roster;
		List<StatTile> tiles = new ArrayList<>();
		if (r.installed(App.LEDGER) || s.worth != null)
		{
			tiles.add(new StatTile("Worth", s.worth == null ? "Not yet" : Format.number(s.worth.total), s.worth != null));
			String week = s.worth == null || s.worth.days == 0 ? "Not yet" : Status.signed(s.worth.change);
			String label = s.worth == null || s.worth.days >= 7 || s.worth.days == 0 ? "This week" : "Last " + Format.plural(s.worth.days, "day", "days");
			tiles.add(new StatTile(label, week, s.worth != null && s.worth.days > 0));
		}
		if (r.installed(App.SESSION_CHRONICLE) || s.session != null)
		{
			tiles.add(new StatTile("Last session", s.session == null ? "Not yet" : Format.shortDuration(s.session.playedMs / 1000)
				+ (s.session.gp == null || s.session.gp == 0 ? "" : ", " + Status.signed(s.session.gp)), s.session != null));
		}
		if (r.installed(App.BANK_STEWARD) || s.bank != null)
		{
			tiles.add(new StatTile("Bank read", s.bank == null ? "Open it once" : Status.ago(s.bank.savedAt, view.now), s.bank != null));
		}
		if (r.installed(App.QUARTERMASTER) || s.kit != null)
		{
			String kit = s.kit == null || !s.kit.chosen() ? "None chosen" : s.kit.trips >= 0 ? s.kit.name + ", " + s.kit.trips : s.kit.name;
			tiles.add(new StatTile(s.kit != null && s.kit.chosen() && s.kit.trips >= 0 ? "Kit, trips" : "Kit", kit, s.kit != null && s.kit.chosen()));
		}
		if (s.gearGoal != null)
		{
			tiles.add(new StatTile("Saving for", s.gearGoal, true));
		}
		if (tiles.isEmpty())
		{
			return note("Add Codex Ledger, Session Chronicle or Bank Steward and your worth, sessions and bank show here.");
		}
		JPanel grid = new JPanel(new GridLayout(0, 2, 6, 6));
		grid.setOpaque(false);
		for (StatTile t : tiles)
		{
			grid.add(t);
		}
		if (tiles.size() % 2 == 1)
		{
			JPanel blank = new JPanel();
			blank.setOpaque(false);
			grid.add(blank);
		}
		return grid;
	}

	private JComponent pickCard(Advice.Pick pick)
	{
		Card card = new Card(new BorderLayout(8, 0)).accent(Seal.colour(pick.app.getFamily()));
		JPanel left = new JPanel(new BorderLayout());
		left.setOpaque(false);
		left.add(new Seal(pick.app, true, 24), BorderLayout.NORTH);
		card.add(left, BorderLayout.WEST);
		StackPanel body = new StackPanel(2, 0);
		body.add(Ui.wrapped(pick.app.shortName(), Theme.title(), Theme.GOLD_BRIGHT, TEXT - 32));
		body.add(Ui.wrapped(pick.reason, Theme.small(), Theme.TEXT, TEXT - 32));
		body.add(Ui.wrapped("Search the Plugin Hub for " + pick.app.getPluginName(), Theme.small(), Theme.GOLD_DIM, TEXT - 32));
		card.add(body, BorderLayout.CENTER);
		card.onClick(() -> actions.open(pick.app));
		card.setToolTipText("Open " + pick.app.getPluginName() + " on the Plugin Hub website");
		return card;
	}

	// ----- Suite -----------------------------------------------------------------------------

	private StackPanel suite()
	{
		StackPanel list = new StackPanel(6, 8);
		Roster r = view.roster;
		list.add(note("Every Codex app. Lit seals are installed. Click one to open its page on the Plugin Hub; "
			+ "add it there from RuneLite's Plugin Hub panel."));
		for (App.Family family : App.Family.values())
		{
			List<App> apps = new ArrayList<>();
			int have = 0;
			for (App app : App.all())
			{
				if (app.getFamily() == family)
				{
					apps.add(app);
					have += r.installed(app) ? 1 : 0;
				}
			}
			list.space(2);
			list.add(new SectionHeader(family.getTitle(), have + " of " + apps.size()));
			for (App app : apps)
			{
				list.add(appRow(app));
			}
		}
		return list;
	}

	private JComponent appRow(App app)
	{
		Roster.State state = view.roster.state(app);
		Card card = new Card(new BorderLayout(8, 0));
		card.setBorder(new EmptyBorder(6, 8, 6, 8));
		JPanel left = new JPanel(new BorderLayout());
		left.setOpaque(false);
		left.add(new Seal(app, state != Roster.State.MISSING, 22), BorderLayout.NORTH);
		card.add(left, BorderLayout.WEST);

		StackPanel body = new StackPanel(1, 0);
		JPanel head = new JPanel(new BorderLayout(4, 0));
		head.setOpaque(false);
		head.add(Ui.label(app.shortName(), Theme.body(), state == Roster.State.MISSING ? Theme.TEXT_MUTED : Theme.GOLD_BRIGHT),
			BorderLayout.CENTER);
		head.add(new Chip(chip(app, state), chipColour(app, state)), BorderLayout.EAST);
		body.add(head);

		String live = view.loggedIn && state != Roster.State.MISSING ? Status.of(app, view.shared, view.now) : null;
		String line = live != null ? live : app.getBlurb();
		body.add(Ui.wrapped(line, Theme.small(), live != null ? Theme.TEXT : Theme.TEXT_FAINT, TEXT - 30));
		card.add(body, BorderLayout.CENTER);
		card.onClick(() -> actions.open(app));
		card.setToolTipText(app.getBlurb());
		return card;
	}

	static String chip(App app, Roster.State state)
	{
		switch (state)
		{
			case ON:
				return "On";
			case OFF:
				return "Off";
			default:
				switch (app.getHub())
				{
					case REVIEW:
						return "In review";
					case SOON:
						return "Soon";
					case PAUSED:
						return "Paused";
					default:
						return "Get";
				}
		}
	}

	private static Color chipColour(App app, Roster.State state)
	{
		switch (state)
		{
			case ON:
				return Theme.GREEN;
			case OFF:
				return Theme.TEXT_MUTED;
			default:
				return app.installable() ? Theme.GOLD : Theme.TEXT_FAINT;
		}
	}

	// ----- Links -----------------------------------------------------------------------------

	private StackPanel links()
	{
		StackPanel list = new StackPanel(6, 8);
		list.add(note("Codex apps share what they learn with each other, so each one knows more than it could alone. "
			+ "Here is what flows on " + (view.loggedIn ? "this account." : "your account once you log in.")));
		int flowing = 0;
		for (Link link : Link.values())
		{
			flowing += link.state(view.roster, view.shared) == Link.State.FLOWING ? 1 : 0;
		}
		list.space(2);
		list.add(new SectionHeader("Threads", flowing + " of " + Link.values().length + " flowing"));
		for (Link link : Link.values())
		{
			list.add(linkCard(link));
		}
		return list;
	}

	private JComponent linkCard(Link link)
	{
		Link.State state = link.state(view.roster, view.shared);
		Color colour;
		String line;
		switch (state)
		{
			case FLOWING:
				colour = Theme.GREEN;
				line = "Flowing";
				break;
			case WAITING:
				colour = Theme.GOLD;
				line = view.loggedIn ? "Waiting: " + link.getStart().toLowerCase() : "Log in to read it";
				break;
			case OFF:
				colour = Theme.TEXT_MUTED;
				line = link.getFrom().shortName() + " is switched off";
				break;
			default:
				colour = Theme.EMBER;
				line = "Add " + link.getFrom().getPluginName() + " to start it";
				break;
		}
		Card card = new Card(new BorderLayout(0, 4)).accent(colour);
		JPanel head = new JPanel(new BorderLayout(6, 0));
		head.setOpaque(false);
		head.add(new Seal(link.getFrom(), state != Link.State.MISSING, 20), BorderLayout.WEST);
		StackPanel title = new StackPanel(0, 0);
		title.add(Ui.label(link.getWhat(), Theme.body(), Theme.GOLD_BRIGHT));
		title.add(Ui.label("from " + link.getFrom().shortName(), Theme.small(), Theme.TEXT_MUTED));
		head.add(title, BorderLayout.CENTER);
		card.add(head, BorderLayout.NORTH);

		StackPanel body = new StackPanel(2, 0);
		body.add(Ui.wrapped(line, Theme.small(), colour, TEXT));
		List<App> users = link.users(view.roster);
		int others = link.getTo().size() - users.size();
		StringBuilder sb = new StringBuilder();
		if (users.isEmpty())
		{
			sb.append("Used by ").append(Format.plural(link.getTo().size(), "app", "apps")).append(", none installed yet.");
		}
		else
		{
			sb.append("Used by your ");
			for (int i = 0; i < users.size(); i++)
			{
				if (i > 0)
				{
					sb.append(i == users.size() - 1 ? " and " : ", ");
				}
				sb.append(users.get(i).shortName());
			}
			sb.append(others > 0 ? ", and " + others + " more you could add." : ".");
		}
		body.add(Ui.wrapped(sb.toString(), Theme.small(), Theme.TEXT, TEXT));
		card.add(body, BorderLayout.CENTER);
		if (state == Link.State.MISSING)
		{
			card.onClick(() -> actions.open(link.getFrom()));
		}
		return card;
	}

	// ----- Pieces ----------------------------------------------------------------------------

	private static String hours(double h)
	{
		if (h < 1)
		{
			return "under 1 hr";
		}
		return Format.plural(Math.round(h), "hr", "hr") + " left";
	}

	private static JComponent note(String text)
	{
		Card card = new Card(new BorderLayout());
		card.add(Ui.wrapped(text, Theme.small(), Theme.TEXT_MUTED, TEXT), BorderLayout.CENTER);
		return card;
	}

	/** A thin gilt progress bar. */
	static final class Bar extends JComponent
	{
		private final double progress;

		Bar(double progress)
		{
			this.progress = progress;
			setOpaque(false);
			setPreferredSize(new Dimension(10, 14));
		}

		@Override
		protected void paintComponent(Graphics graphics)
		{
			Graphics2D g = (Graphics2D) graphics.create();
			Theme.smooth(g);
			int w = getWidth();
			String pct = Math.round(Math.floor(progress * 100)) + "%";
			g.setFont(Theme.small());
			int tw = g.getFontMetrics().stringWidth(pct);
			int bw = w - tw - 6;
			RoundRectangle2D track = new RoundRectangle2D.Float(0, 4, bw, 6, 6, 6);
			g.setColor(Theme.INK_DEEP);
			g.fill(track);
			int fill = (int) Math.round(bw * progress);
			if (fill > 0)
			{
				Color c = Theme.progressColor(progress);
				g.setPaint(new GradientPaint(0, 4, Theme.mix(c, Color.WHITE, 0.25), 0, 10, c));
				g.fill(new RoundRectangle2D.Float(0, 4, Math.max(6, fill), 6, 6, 6));
			}
			g.setColor(Theme.STROKE_SOFT);
			g.draw(track);
			Ui.shadowed(g, pct, w - tw, 11, Theme.TEXT_MUTED);
			g.dispose();
		}
	}

	/** A small rounded label, like "On" or "Get". */
	static final class Chip extends JComponent
	{
		private final String text;
		private final Color colour;

		Chip(String text, Color colour)
		{
			this.text = text;
			this.colour = colour;
			setOpaque(false);
			setFont(Theme.small());
			setPreferredSize(new Dimension(getFontMetrics(getFont()).stringWidth(text) + 12, 14));
		}

		@Override
		protected void paintComponent(Graphics graphics)
		{
			Graphics2D g = (Graphics2D) graphics.create();
			Theme.smooth(g);
			int w = getWidth();
			RoundRectangle2D shape = new RoundRectangle2D.Float(0.5f, 0.5f, w - 1, 13, 12, 12);
			g.setColor(Theme.alpha(colour, 36));
			g.fill(shape);
			g.setColor(Theme.alpha(colour, 150));
			g.draw(shape);
			g.setFont(getFont());
			g.setColor(colour);
			g.drawString(text, 6, 11);
			g.dispose();
		}
	}

	/** One first step: a tick or an empty ring, what to do and what it gives. */
	static final class StepRow extends JPanel
	{
		StepRow(Advice.Step step, int width)
		{
			super(new BorderLayout(8, 0));
			setOpaque(false);
			setBorder(new EmptyBorder(2, 2, 2, 2));
			JPanel mark = new JPanel(new BorderLayout());
			mark.setOpaque(false);
			mark.add(new Tick(step.done), BorderLayout.NORTH);
			add(mark, BorderLayout.WEST);
			StackPanel body = new StackPanel(1, 0);
			body.add(Ui.wrapped(step.title(), Theme.body(), step.done ? Theme.TEXT_MUTED : Theme.TEXT, width));
			body.add(Ui.wrapped(step.detail, Theme.small(), step.done ? Theme.TEXT_FAINT : Theme.GOLD_DIM, width));
			add(body, BorderLayout.CENTER);
		}
	}

	static final class Tick extends JComponent
	{
		private final boolean done;

		Tick(boolean done)
		{
			this.done = done;
			setOpaque(false);
			setPreferredSize(new Dimension(14, 16));
		}

		@Override
		protected void paintComponent(Graphics graphics)
		{
			Graphics2D g = (Graphics2D) graphics.create();
			Theme.smooth(g);
			Ellipse2D ring = new Ellipse2D.Float(1, 2, 12, 12);
			if (done)
			{
				g.setColor(Theme.alpha(Theme.GREEN, 60));
				g.fill(ring);
				g.setColor(Theme.GREEN);
				g.draw(ring);
				Path2D tick = new Path2D.Float();
				tick.moveTo(4, 8);
				tick.lineTo(6.2, 10.4);
				tick.lineTo(10.2, 5.4);
				g.setStroke(new BasicStroke(1.6f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
				g.draw(tick);
			}
			else
			{
				g.setColor(Theme.alpha(Theme.GOLD, 140));
				g.draw(ring);
			}
			g.dispose();
		}
	}
}
