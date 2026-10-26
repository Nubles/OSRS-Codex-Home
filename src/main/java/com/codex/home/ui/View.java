package com.codex.home.ui;

import com.codex.home.core.Account;
import com.codex.home.core.Advice;
import com.codex.home.core.Roster;
import com.codex.home.core.Shared;
import java.util.Collections;
import java.util.List;

/** Everything the panel shows, built off the Swing thread and handed over whole. */
public final class View
{
	public final boolean loggedIn;
	public final Account account;
	public final Roster roster;
	public final Shared shared;
	public final List<Advice.Step> steps;
	public final List<Advice.Pick> picks;
	public final boolean startHere;
	public final long now;

	public View(boolean loggedIn, Account account, Roster roster, Shared shared, List<Advice.Step> steps,
		List<Advice.Pick> picks, boolean startHere, long now)
	{
		this.loggedIn = loggedIn;
		this.account = account == null ? Account.UNKNOWN : account;
		this.roster = roster == null ? Roster.EMPTY : roster;
		this.shared = shared == null ? Shared.NONE : shared;
		this.steps = steps == null ? Collections.emptyList() : steps;
		this.picks = picks == null ? Collections.emptyList() : picks;
		this.startHere = startHere;
		this.now = now;
	}

	public static View empty()
	{
		return new View(false, Account.UNKNOWN, Roster.EMPTY, Shared.NONE, null, null, true, System.currentTimeMillis());
	}
}
