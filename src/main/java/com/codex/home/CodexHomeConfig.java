package com.codex.home;

import net.runelite.client.config.Config;
import net.runelite.client.config.ConfigGroup;
import net.runelite.client.config.ConfigItem;

@ConfigGroup(CodexHomeConfig.GROUP)
public interface CodexHomeConfig extends Config
{
	String GROUP = "codexhome";

	/** The tab the panel was last left on. */
	String TAB_KEY = "tab";

	@ConfigItem(
		keyName = "loginLine",
		name = "Login summary",
		description = "At login, put one line in the chat box with your next step and your worth",
		position = 0
	)
	default boolean loginLine()
	{
		return true;
	}

	@ConfigItem(
		keyName = "startHere",
		name = "Show first steps",
		description = "Show the Start here steps on the Today tab until they are all done",
		position = 1
	)
	default boolean startHere()
	{
		return true;
	}

	@ConfigItem(
		keyName = "picks",
		name = "Suggest apps",
		description = "Suggest Codex apps suited to your account on the Today tab",
		position = 2
	)
	default boolean picks()
	{
		return true;
	}
}
