package com.chathighlightplayer;

import net.runelite.client.config.Config;
import net.runelite.client.config.ConfigGroup;
import net.runelite.client.config.ConfigItem;
import net.runelite.client.config.ConfigSection;

import java.awt.Color;

@ConfigGroup("chathighlightplayer")
@SuppressWarnings("unused")
public interface ChatHighlightPlayerConfig extends Config
{
	String TEMPORARY_HIGHLIGHT_SECTION = "temporaryHighlight";
	String CHATBOX_SECTION = "chatboxMenu";
	String GENERAL_SECTION = "general";

	@ConfigSection(
		name = "General",
		description = "General plugin settings",
		position = 0
	)
	String generalSection = GENERAL_SECTION;

	@ConfigSection(
		name = "Player Highlight",
		description = "Options for player highlight behavior",
		position = 2
	)
	String temporaryHighlightSection = TEMPORARY_HIGHLIGHT_SECTION;

	@ConfigSection(
		name = "Chatbox Menu",
		description = "Options for adding Highlight Player to chatbox menus",
		position = 1
	)
	String chatboxSection = CHATBOX_SECTION;

	@ConfigItem(
		keyName = "trimHighlightLines",
		name = "Trim highlight lines",
		description = "Shorten line endpoints so they do not pass through player models",
		position = 0,
		section = GENERAL_SECTION
	)
	default boolean trimHighlightLines()
	{
		return true;
	}

	@ConfigItem(
		keyName = "fadeHighlights",
		name = "Fade highlights",
		description = "Fade highlights out when they are removed",
		position = 1,
		section = GENERAL_SECTION
	)
	default boolean fadeHighlights()
	{
		return true;
	}

	@ConfigItem(
		keyName = "fadeDurationMs",
		name = "Fade duration (ms)",
		description = "Highlight fade duration in milliseconds. Values are clamped from 100 to 2000",
		position = 2,
		section = GENERAL_SECTION
	)
	default int fadeDurationMs()
	{
		return 400;
	}

	/* Available fixed durations for temporary highlight */
	enum HighlightDuration
	{
		S5(5), S10(10), S15(15), S20(20), S30(30), S45(45), S60(60);

		private final int seconds;

		HighlightDuration(int seconds)
		{
			this.seconds = seconds;
		}

		public int seconds()
		{
			return seconds;
		}

		@Override
		public String toString()
		{
			return Integer.toString(seconds);
		}
	}

	@ConfigItem(
		keyName = "duration",
		name = "Duration (seconds)",
		description = "Duration for player highlight (select from preset values)",
		position = 0,
		section = TEMPORARY_HIGHLIGHT_SECTION
	)
	default HighlightDuration duration()
	{
		return HighlightDuration.S10;
	}

	@ConfigItem(
			keyName = "tagColor",
			name = "Color",
				description = "Color used for player highlights",
			position = 1,
			section = TEMPORARY_HIGHLIGHT_SECTION
	)
	default Color tagColor()
	{
		return Color.MAGENTA;
	}

	@ConfigItem(
			keyName = "showline",
			name = "Line enabled",
				description = "Draw a line to the highlighted player",
			position = 2,
			section = TEMPORARY_HIGHLIGHT_SECTION
	)
	default boolean line()
	{
		return true;
	}

	@ConfigItem(
			keyName = "highlightCondensedPlayerName",
			name = "Highlight condensed player name",
				description = "Color the player name in condensed player option menus when highlighted",
			position = 3,
			section = TEMPORARY_HIGHLIGHT_SECTION
	)
	default boolean highlightCondensedPlayerName()
	{
		return true;
	}

	@ConfigItem(
			keyName = "temporaryHighlightRegularMenuPlayerName",
			name = "Highlight menu player name",
				description = "Color the player name in regular right-click menus when highlighted",
			position = 4,
			section = TEMPORARY_HIGHLIGHT_SECTION
	)
	default boolean temporaryHighlightRegularMenuPlayerName()
	{
		return true;
	}

	@ConfigItem(
			keyName = "temporaryHideOtherPlayerMenus",
			name = "Only show this player's menu",
				description = "Hide menu entries for other players when the highlighted player is in the menu",
			position = 5,
			section = TEMPORARY_HIGHLIGHT_SECTION
	)
	default boolean temporaryHideOtherPlayerMenus()
	{
		return true;
	}

	@ConfigItem(
			keyName = "temporaryMenuOption",
			name = "Menu option",
				description = "Comma-separated menu options to color for highlighted players. Leave blank or use None to disable. Use * for any option",
			position = 6,
			section = TEMPORARY_HIGHLIGHT_SECTION
	)
	default String temporaryMenuOption()
	{
		return "Trade with,";
	}

	@ConfigItem(
			keyName = "showTemporaryPlayerName",
			name = "Show name above head",
				description = "Show the player name above the highlight",
			position = 7,
			section = TEMPORARY_HIGHLIGHT_SECTION
	)
	default boolean showTemporaryPlayerName()
	{
		return true;
	}

	@ConfigItem(
			keyName = "showHoverHighlight",
			name = "Show hover tooltip (chatbox)",
			description = "Show or hide the Highlight Player tooltip when hovering over the chatbox",
			position = 0,
			section = CHATBOX_SECTION
	)
	default boolean showHoverHighlight()
	{
		return true;
	}

}
