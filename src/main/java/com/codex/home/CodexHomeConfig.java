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
		name = "Login plaque",
		description = "At login, show a small plaque over the game for a few seconds with your next step and your worth."
			+ " Home never writes in the chat box",
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

	@ConfigItem(
		keyName = "watch",
		name = "On watch",
		description = "Show the On watch list on the Today tab: charged items running low (Boss Pilot), your Hunter rumour (Autopilot),"
			+ " Tempoross and Guardians of the Rift (Minigame Pilot) and your Sailing",
		position = 3
	)
	default boolean watch()
	{
		return true;
	}

	@ConfigItem(
		keyName = "sailing",
		name = "Sailing progress",
		description = "Show your Sailing level, the experience to the next and your port tasks on the On watch list",
		position = 4
	)
	default boolean sailing()
	{
		return true;
	}

	@ConfigItem(
		keyName = "watchLogin",
		name = "Warnings at login",
		description = "Add what needs doing (a charged item running low, a rumour part to hand in) to the login plaque",
		position = 5
	)
	default boolean watchLogin()
	{
		return true;
	}
}
