package com.codex.home.core;

/** The kind of account, from the game's ironman varbit, which decides what Home suggests. */
public enum Account
{
	UNKNOWN,
	MAIN,
	IRONMAN,
	ULTIMATE,
	HARDCORE,
	GROUP;

	/** Reads the value of the game's ironman varbit (0 main, 1 iron, 2 ultimate, 3 hardcore, 4 to 6 group). */
	public static Account of(int varbit)
	{
		switch (varbit)
		{
			case 0:
				return MAIN;
			case 1:
				return IRONMAN;
			case 2:
				return ULTIMATE;
			case 3:
				return HARDCORE;
			case 4:
			case 5:
			case 6:
				return GROUP;
			default:
				return UNKNOWN;
		}
	}

	public boolean iron()
	{
		return this == IRONMAN || this == ULTIMATE || this == HARDCORE || this == GROUP;
	}

	/** How the crest names it. */
	public String title()
	{
		switch (this)
		{
			case IRONMAN:
				return "Ironman";
			case ULTIMATE:
				return "Ultimate Ironman";
			case HARDCORE:
				return "Hardcore Ironman";
			case GROUP:
				return "Group Ironman";
			case MAIN:
				return "Main account";
			default:
				return "Account";
		}
	}
}
