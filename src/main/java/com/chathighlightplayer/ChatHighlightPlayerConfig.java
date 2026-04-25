package com.chathighlightplayer;

import net.runelite.client.config.Config;
import net.runelite.client.config.ConfigGroup;
import net.runelite.client.config.ConfigItem;
import net.runelite.client.config.ConfigSection;

import java.awt.*;

@ConfigGroup("chathighlightplayer")
public interface ChatHighlightPlayerConfig extends Config
{
	String TEMPORARY_HIGHLIGHT_SECTION = "temporaryHighlight";
	String CHATBOX_SECTION = "chatboxMenu";
	String GENERAL_SECTION = "general";
	String ALWAYS_HIGHLIGHT_GROUP_ONE = "alwaysHighlightGroupOne";
	String ALWAYS_HIGHLIGHT_GROUP_TWO = "alwaysHighlightGroupTwo";
	String ALWAYS_HIGHLIGHT_GROUP_THREE = "alwaysHighlightGroupThree";

	@ConfigSection(
		name = "General",
		description = "General plugin settings",
		position = 0
	)
	String generalSection = GENERAL_SECTION;

	@ConfigSection(
		name = "Temporary Highlight",
		description = "Options for click-to-highlight behavior",
		position = 2
	)
	String temporaryHighlightSection = TEMPORARY_HIGHLIGHT_SECTION;

	@ConfigSection(
		name = "Chatbox Menu",
		description = "Options for adding Highlight Player to chatbox menus",
		position = 1
	)
	String chatboxSection = CHATBOX_SECTION;

	@ConfigSection(
		name = "Always Highlight Group 1",
		description = "Persistently highlight a list of players",
		position = 3
	)
	String alwaysHighlightGroupOneSection = ALWAYS_HIGHLIGHT_GROUP_ONE;

	@ConfigSection(
		name = "Always Highlight Group 2",
		description = "Persistently highlight a second list of players",
		position = 4
	)
	String alwaysHighlightGroupTwoSection = ALWAYS_HIGHLIGHT_GROUP_TWO;

	@ConfigSection(
		name = "Always Highlight Group 3",
		description = "Persistently highlight a third list of players",
		position = 5
	)
	String alwaysHighlightGroupThreeSection = ALWAYS_HIGHLIGHT_GROUP_THREE;

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
		keyName = "time",
		name = "Duration (seconds)",
		description = "Time to show the temporary player highlight in seconds",
		position = 0,
		section = TEMPORARY_HIGHLIGHT_SECTION
	)
	default int time()
	{
		return 10;
	}

	@ConfigItem(
			keyName = "tagColor",
			name = "Color",
			description = "Color used for temporary player highlights",
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
			description = "Draw a line to the temporarily highlighted player",
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
			description = "Color the player name in condensed player option menus",
			position = 3,
			section = TEMPORARY_HIGHLIGHT_SECTION
	)
	default boolean highlightCondensedPlayerName()
	{
		return true;
	}

	@ConfigItem(
			keyName = "showTemporaryPlayerName",
			name = "Show name above head",
			description = "Show the player name above the temporary highlight",
			position = 4,
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

	@ConfigItem(
			keyName = "copyUsernameToClipboard",
			name = "Copy username to clipboard",
			description = "Show a Copy Username option when right-clicking usernames in chat",
			position = 1,
			section = CHATBOX_SECTION
	)
	default boolean copyUsernameToClipboard()
	{
		return false;
	}

	@ConfigItem(
		keyName = "alwaysHighlightEnabledOne",
		name = "Enabled",
		description = "Enable always-highlight group 1",
		position = 0,
		section = ALWAYS_HIGHLIGHT_GROUP_ONE
	)
	default boolean alwaysHighlightEnabledOne()
	{
		return false;
	}

	@ConfigItem(
		keyName = "alwaysHighlightPlayersOne",
		name = "Player names",
		description = "Comma-separated list of player names to always highlight",
		position = 1,
		section = ALWAYS_HIGHLIGHT_GROUP_ONE
	)
	default String alwaysHighlightPlayersOne()
	{
		return "";
	}

	@ConfigItem(
		keyName = "alwaysHighlightColorOne",
		name = "Color",
		description = "Color used for always-highlight group 1",
		position = 2,
		section = ALWAYS_HIGHLIGHT_GROUP_ONE
	)
	default Color alwaysHighlightColorOne()
	{
		return Color.GREEN;
	}

	@ConfigItem(
		keyName = "alwaysHighlightLineOne",
		name = "Line enabled",
		description = "Draw a line to players in always-highlight group 1",
		position = 3,
		section = ALWAYS_HIGHLIGHT_GROUP_ONE
	)
	default boolean alwaysHighlightLineOne()
	{
		return false;
	}

	@ConfigItem(
		keyName = "alwaysHighlightShowNameOne",
		name = "Show name above head",
		description = "Show player names above players in always-highlight group 1",
		position = 4,
		section = ALWAYS_HIGHLIGHT_GROUP_ONE
	)
	default boolean alwaysHighlightShowNameOne()
	{
		return true;
	}

	@ConfigItem(
		keyName = "alwaysHighlightEnabledTwo",
		name = "Enabled",
		description = "Enable always-highlight group 2",
		position = 0,
		section = ALWAYS_HIGHLIGHT_GROUP_TWO
	)
	default boolean alwaysHighlightEnabledTwo()
	{
		return false;
	}

	@ConfigItem(
		keyName = "alwaysHighlightPlayersTwo",
		name = "Player names",
		description = "Comma-separated list of player names to always highlight",
		position = 1,
		section = ALWAYS_HIGHLIGHT_GROUP_TWO
	)
	default String alwaysHighlightPlayersTwo()
	{
		return "";
	}

	@ConfigItem(
		keyName = "alwaysHighlightColorTwo",
		name = "Color",
		description = "Color used for always-highlight group 2",
		position = 2,
		section = ALWAYS_HIGHLIGHT_GROUP_TWO
	)
	default Color alwaysHighlightColorTwo()
	{
		return Color.RED;
	}

	@ConfigItem(
		keyName = "alwaysHighlightLineTwo",
		name = "Line enabled",
		description = "Draw a line to players in always-highlight group 2",
		position = 3,
		section = ALWAYS_HIGHLIGHT_GROUP_TWO
	)
	default boolean alwaysHighlightLineTwo()
	{
		return false;
	}

	@ConfigItem(
		keyName = "alwaysHighlightShowNameTwo",
		name = "Show name above head",
		description = "Show player names above players in always-highlight group 2",
		position = 4,
		section = ALWAYS_HIGHLIGHT_GROUP_TWO
	)
	default boolean alwaysHighlightShowNameTwo()
	{
		return true;
	}

	@ConfigItem(
		keyName = "alwaysHighlightEnabledThree",
		name = "Enabled",
		description = "Enable always-highlight group 3",
		position = 0,
		section = ALWAYS_HIGHLIGHT_GROUP_THREE
	)
	default boolean alwaysHighlightEnabledThree()
	{
		return false;
	}

	@ConfigItem(
		keyName = "alwaysHighlightPlayersThree",
		name = "Player names",
		description = "Comma-separated list of player names to always highlight",
		position = 1,
		section = ALWAYS_HIGHLIGHT_GROUP_THREE
	)
	default String alwaysHighlightPlayersThree()
	{
		return "";
	}

	@ConfigItem(
		keyName = "alwaysHighlightColorThree",
		name = "Color",
		description = "Color used for always-highlight group 3",
		position = 2,
		section = ALWAYS_HIGHLIGHT_GROUP_THREE
	)
	default Color alwaysHighlightColorThree()
	{
		return Color.CYAN;
	}

	@ConfigItem(
		keyName = "alwaysHighlightLineThree",
		name = "Line enabled",
		description = "Draw a line to players in always-highlight group 3",
		position = 3,
		section = ALWAYS_HIGHLIGHT_GROUP_THREE
	)
	default boolean alwaysHighlightLineThree()
	{
		return false;
	}

	@ConfigItem(
		keyName = "alwaysHighlightShowNameThree",
		name = "Show name above head",
		description = "Show player names above players in always-highlight group 3",
		position = 4,
		section = ALWAYS_HIGHLIGHT_GROUP_THREE
	)
	default boolean alwaysHighlightShowNameThree()
	{
		return true;
	}
}
