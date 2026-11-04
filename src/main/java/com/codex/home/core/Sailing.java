package com.codex.home.core;

import net.runelite.api.Experience;

/**
 * The account's Sailing as the game says it now: level and experience ({@code Skill.SAILING}) and
 * the game's own count of port tasks completed ({@code VarPlayerID.PORT_TASKS_COMPLETED}, the
 * count Codex Session Chronicle counts port tasks from). Read by Home itself, not shared by an app.
 */
public final class Sailing
{
	/**
	 * Port tasks held at once from each Sailing level (OSRS Wiki, Port tasks: "Players may
	 * initially only have one active task, but this number increases at higher levels": 1 at
	 * level 1, 2 at 7, 3 at 28, 4 at 56, 5 at 84).
	 */
	static final int[] SLOT_LEVELS = {1, 7, 28, 56, 84};

	public final int level;
	public final int xp;
	/** Port tasks completed, or -1 before the game has sent the count. */
	public final int portTasks;

	public Sailing(int level, int xp, int portTasks)
	{
		this.level = Math.max(1, Math.min(Experience.MAX_REAL_LEVEL, level));
		this.xp = Math.max(0, xp);
		// The count reads 0 until the game sends it: a real 0 shows the same as not known.
		this.portTasks = portTasks > 0 ? portTasks : -1;
	}

	/** How far through this level, 0 to 1; 1 at level 99. */
	public double progress()
	{
		if (level >= Experience.MAX_REAL_LEVEL)
		{
			return 1;
		}
		int from = Experience.getXpForLevel(level);
		int to = Experience.getXpForLevel(level + 1);
		return Math.max(0, Math.min(1, (xp - from) / (double) (to - from)));
	}

	/** Experience still to the next level, or 0 at 99. */
	public int toNext()
	{
		return level >= Experience.MAX_REAL_LEVEL ? 0 : Math.max(0, Experience.getXpForLevel(level + 1) - xp);
	}

	/** Port tasks this level lets the player hold at once. */
	public int slots()
	{
		return slots(level);
	}

	static int slots(int level)
	{
		int n = 0;
		for (int l : SLOT_LEVELS)
		{
			n += level >= l ? 1 : 0;
		}
		return Math.max(1, n);
	}

	/** The next level that adds a port task slot, or -1 when every slot is open. */
	public int nextSlotLevel()
	{
		for (int l : SLOT_LEVELS)
		{
			if (level < l)
			{
				return l;
			}
		}
		return -1;
	}

	@Override
	public boolean equals(Object o)
	{
		if (!(o instanceof Sailing))
		{
			return false;
		}
		Sailing s = (Sailing) o;
		return s.level == level && s.xp == xp && s.portTasks == portTasks;
	}

	@Override
	public int hashCode()
	{
		return (level * 31 + xp) * 31 + portTasks;
	}
}
