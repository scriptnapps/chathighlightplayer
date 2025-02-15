package com.ChatHighlightPlayer;

import net.runelite.client.config.Config;
import net.runelite.client.config.ConfigGroup;
import net.runelite.client.config.ConfigItem;

import java.awt.*;

@ConfigGroup("ChatHighlightPlayer")
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
}
