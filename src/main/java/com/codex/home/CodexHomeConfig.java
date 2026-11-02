package com.codex.home;

import net.runelite.client.config.Config;
import net.runelite.client.config.ConfigGroup;
import net.runelite.client.config.ConfigItem;
import net.runelite.client.config.ConfigSection;

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

	@ConfigSection(
		name = "Field notes",
		description = "A local record for checking the plugin's guesses",
		position = 99,
		closedByDefault = true
	)
	String FIELD_NOTES = "fieldNotesSection";

	@ConfigItem(
		keyName = "fieldNotes",
		name = "Keep field notes",
		description = "Write what Codex Home reads and decides to its folder in .runelite/plugin-data on your computer, for checking its guesses; nothing is sent anywhere",
		section = FIELD_NOTES,
		position = 0
	)
	default boolean fieldNotes()
	{
		return false;
	}
}
