package com.chathighlightplayer;

import java.awt.Color;

final class HighlightStyle
{
	private final Color color;
	private final boolean showLine;
	private final boolean showName;
	private final String menuOption;
	private final boolean highlightRegularMenuPlayerName;
	private final boolean hideOtherPlayerMenus;

	HighlightStyle(Color color, boolean showLine, boolean showName, String menuOption, boolean highlightRegularMenuPlayerName, boolean hideOtherPlayerMenus)
	{
		this.color = color;
		this.showLine = showLine;
		this.showName = showName;
		this.menuOption = menuOption;
		this.highlightRegularMenuPlayerName = highlightRegularMenuPlayerName;
		this.hideOtherPlayerMenus = hideOtherPlayerMenus;
	}

	Color getColor()
	{
		return color;
	}

	boolean isShowLine()
	{
		return showLine;
	}

	boolean isShowName()
	{
		return showName;
	}

	String getMenuOption()
	{
		return menuOption;
	}

	boolean isHighlightRegularMenuPlayerName()
	{
		return highlightRegularMenuPlayerName;
	}

	boolean isHideOtherPlayerMenus()
	{
		return hideOtherPlayerMenus;
	}
}
