package com.chathighlightplayer;

import java.awt.Color;

final class HighlightStyle
{
	private final Color color;
	private final boolean showLine;
	private final boolean showName;

	HighlightStyle(Color color, boolean showLine, boolean showName)
	{
		this.color = color;
		this.showLine = showLine;
		this.showName = showName;
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
}
