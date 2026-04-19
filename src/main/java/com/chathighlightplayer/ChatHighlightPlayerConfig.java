package com.chathighlightplayer;

import net.runelite.client.config.Config;
import net.runelite.client.config.ConfigGroup;
import net.runelite.client.config.ConfigItem;

import java.awt.*;

@ConfigGroup("chathighlightplayer")
public interface ChatHighlightPlayerConfig extends Config
{
	@ConfigItem(
		keyName = "time",
		name = "Duration (seconds)",
		description = "Time to slow the player in seconds"
	)
	default int time()
	{
		return 10;
	}

	@ConfigItem(
			keyName = "tagColor",
			name = "Color",
			description = "If player is highlighted use this color"
	)
	default Color tagColor()
	{
		return Color.MAGENTA;
	}

	@ConfigItem(
			keyName = "showline",
			name = "Line enabled (Made by kat)",
			description = "Draw line towards player"
	)
	default boolean line()
	{
		return true;
	}

	@ConfigItem(
			keyName = "highlightCondensedPlayerName",
			name = "Highlight condensed player name",
			description = "Color the player name in condensed player option menus"
	)
	default boolean highlightCondensedPlayerName()
	{
		return true;
	}
}
