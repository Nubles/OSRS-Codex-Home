package com.codex.home.ui;

/** The panel's three views. */
public enum Tab
{
	TODAY("Today"),
	SUITE("Suite"),
	LINKS("Links");

	private final String title;

	Tab(String title)
	{
		this.title = title;
	}

	public String getTitle()
	{
		return title;
	}
}
