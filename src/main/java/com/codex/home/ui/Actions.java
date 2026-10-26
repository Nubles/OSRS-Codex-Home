package com.codex.home.ui;

import com.codex.home.core.App;

/** What the panel's buttons ask the plugin to do. */
public interface Actions
{
	/** Show one of the panel's tabs. */
	void tab(Tab tab);

	/** Open an app's page: its Plugin Hub page, or where it is built until it reaches the Hub. */
	void open(App app);
}
