package com.chathighlightplayer;

import net.runelite.client.RuneLite;
import net.runelite.client.externalplugins.ExternalPluginManager;

public class ChatHighlightPlayerTest
{
	public static void main(String[] args) throws Exception
	{
		ExternalPluginManager.loadBuiltin(ChatHighlightPlayerPlugin.class);
		RuneLite.main(args);
	}
}