package com.chathighlightplayer;

import com.google.inject.Provides;
import javax.inject.Inject;
import lombok.extern.slf4j.Slf4j;
import net.runelite.api.*;
import net.runelite.api.events.BeforeMenuRender;
import net.runelite.api.events.MenuEntryAdded;
import net.runelite.api.events.MenuOpened;
import net.runelite.api.widgets.ComponentID;
import net.runelite.api.widgets.InterfaceID;
import net.runelite.api.widgets.Widget;
import net.runelite.api.widgets.WidgetUtil;
import net.runelite.client.config.ConfigManager;
import net.runelite.client.eventbus.Subscribe;
import net.runelite.client.plugins.Plugin;
import net.runelite.client.plugins.PluginDescriptor;
import net.runelite.api.Client;
import net.runelite.api.events.GameTick;
import net.runelite.client.ui.overlay.OverlayManager;
import net.runelite.client.util.Text;

import java.awt.Color;


@Slf4j
@PluginDescriptor(
		name = "Click Chat Highlight Player",
		description = "Highlight Player when click on name in chat!"
)
public class ChatHighlightPlayerPlugin extends Plugin
{
	@Inject
	private Client client;

	@Inject
	private ChatHighlightPlayerConfig config;

	@Inject
	private OverlayManager overlayManager;

	@Inject
	private ChatHighlightPlayerOverlay overlay;

	private String targetPlayerName;
	private long durationMs = 10;
	private Color color = Color.pink;

	private long startTime = 0;
	private boolean isActive = false;
	private boolean showline = true;

	private static final String REPORT = "Report";
	private static final String TRADE = "Accept trade";

	@Subscribe
	public void onGameTick(GameTick event) {
		initiatehighlight();
	}

	private void initiatehighlight(){
		if(!isActive) {
			overlay.setTargetVisible(false,showline);
		}
		if (isActive && targetPlayerName != null && targetPlayerName.length() > 1) {
			long currentTime = System.currentTimeMillis();
			if (currentTime >= startTime + durationMs*1000) {
				isActive = false;
			}
			for (Player player : client.getPlayers()) {
				if (player.getName() != null && cleanPlayerName(player.getName()).equalsIgnoreCase(targetPlayerName)) {
					overlay.setTargetPlayer(player,color);
					overlay.setTargetVisible(true,showline);
				}
			}
        }
	}

	private void setHighlightPlayer(String playerName) {
		targetPlayerName = "";
		log.info("tagging" + playerName);
		targetPlayerName = normalizePlayerName(playerName);
		startTime = System.currentTimeMillis();
		isActive = true;
		showline = config.line();
		durationMs = config.time();
		color = config.tagColor();
		initiatehighlight();
	}

	private String colorToHex(Color color) {
		return String.format("#%02x%02x%02x", color.getRed(), color.getGreen(), color.getBlue());
	}

	private String buildHighlightedPlayerTarget(String rawTarget)
	{
		String cleanedTarget = cleanPlayerName(rawTarget);
		String levelSuffix = "";
		int levelIdx = cleanedTarget.indexOf(" (level-");
		if (levelIdx >= 0)
		{
			levelSuffix = cleanedTarget.substring(levelIdx);
			cleanedTarget = cleanedTarget.substring(0, levelIdx);
		}

		String hexColor = colorToHex(color).replace("#", "");
		if (levelSuffix.isEmpty())
		{
			return "<col=" + hexColor + ">" + cleanedTarget + "</col>";
		}

		return "<col=" + hexColor + ">" + cleanedTarget + "</col><col=ff9040>" + levelSuffix + "</col>";
	}

	private void highlightMatchingMenuEntry(MenuEntry menuEntry)
	{
		if (targetPlayerName == null || targetPlayerName.length() <= 1)
		{
			return;
		}

		String optionName = "";
		String targetName = "";
		try
		{
			optionName = normalizePlayerName(menuEntry.getOption());
			targetName = normalizePlayerName(menuEntry.getTarget());
		}
		catch (Exception ignore)
		{
		}

		Color customColor = color;
		String hexColor = colorToHex(customColor).replace("#", "");
		boolean isCondensedParent = config.highlightCondensedPlayerName()
			&& menuEntry.getSubMenu() != null
			&& menuEntry.getType() == MenuAction.RUNELITE
			&& menuEntry.getOption().isEmpty();
		if (isCondensedParent && optionName.equalsIgnoreCase(targetPlayerName) && !menuEntry.getOption().equals("Trade with"))
		{
			menuEntry.setOption("<col=" + hexColor + ">" + menuEntry.getOption() + "</col>");
		}
		if (isCondensedParent && targetName.equalsIgnoreCase(targetPlayerName))
		{
			menuEntry.setTarget(buildHighlightedPlayerTarget(menuEntry.getTarget()));
		}
		if (menuEntry.getOption().equals("Trade with") && targetName.equalsIgnoreCase(targetPlayerName))
		{
			menuEntry.setOption("<col=" + hexColor + ">" + menuEntry.getOption() + "</col>");
		}
	}

	private String normalizePlayerName(String name)
	{
		return cleanPlayerName(name)
			.replaceAll("\\s*\\(level-\\d+\\)$", "")
			.trim();
	}

	@Subscribe
	public void onMenuEntryAdded(MenuEntryAdded entry) {
		highlightMatchingMenuEntry(entry.getMenuEntry());

		if (entry.getType() != MenuAction.CC_OP.getId() && entry.getType() != MenuAction.CC_OP_LOW_PRIORITY.getId())
		{
			return;
		}

		final int groupId = WidgetUtil.componentToInterface(entry.getActionParam1());
		final int childId = WidgetUtil.componentToId(entry.getActionParam1());

		if (groupId != InterfaceID.CHATBOX)
		{
			return;
		}

		MenuEntry[] mm = client.getMenu().getMenuEntries();
		for(MenuEntry m:mm){
			if(m.getOption().contains("Highlight Player")) {
				return;
			}
		}

		if (entry.getOption().toLowerCase().contains(TRADE.toLowerCase()) ) {
			String username = cleanPlayerName(entry.getTarget());
			Color customColor = config.tagColor();
			String hexColor = colorToHex(customColor);
			client.createMenuEntry(1)
					.setOption("<col=" + hexColor.replace("#", "") + ">" + "Highlight Player" + "</col>")
					.setTarget(entry.getTarget())
					.setType(MenuAction.WIDGET_SECOND_OPTION)
					.onClick(e -> setHighlightPlayer(username));
			return;
		}

		final Widget widget = client.getWidget(groupId, childId);
		final Widget parent = widget.getParent();

		if (ComponentID.CHATBOX_MESSAGE_LINES != parent.getId())
		{
			return;
		}

		final int first = WidgetUtil.componentToId(ComponentID.CHATBOX_FIRST_MESSAGE);
		final int dynamicChildId = (childId - first) * 4 + 1;
		final Widget messageContents = parent.getChild(dynamicChildId);
		if (messageContents == null)
		{
			return;
		}

		if (entry.getOption().equals(REPORT) ) {
			String username = cleanPlayerName(entry.getTarget());
             if(username.trim().length() > 1) {
				 Color customColor = config.tagColor();
				 String hexColor = colorToHex(customColor);
				 client.createMenuEntry(1)
						 .setOption("<col=" + hexColor.replace("#", "") + ">" + "Highlight Player" + "</col>")
						 .setTarget(entry.getTarget())
						 .setType(MenuAction.RUNELITE_HIGH_PRIORITY)
						 .onClick(e -> setHighlightPlayer(username));
			 }
		}

	}

	@Subscribe
	public void onMenuOpened(MenuOpened event)
	{
		for (MenuEntry menuEntry : client.getMenuEntries())
		{
			highlightMatchingMenuEntry(menuEntry);
		}
	}

	@Subscribe
	public void onBeforeMenuRender(BeforeMenuRender event)
	{
		MenuEntry[] menuEntries = client.getMenuEntries();
		for (MenuEntry menuEntry : menuEntries)
		{
			highlightMatchingMenuEntry(menuEntry);
		}
		client.setMenuEntries(menuEntries);
	}

	private String cleanPlayerName(String name) {
		return Text.removeTags(name)
				.replace('\u00A0', ' ')
				.trim();
	}

	@Override
	protected void startUp() throws Exception
	{
		log.info("ChatHighlightPlayerPlugin started!");
		overlayManager.add(overlay);
	}

	@Override
	protected void shutDown() throws Exception
	{
		overlayManager.remove(overlay);
		log.info("ChatHighlightPlayerPlugin stopped!");
	}

	@Provides
	ChatHighlightPlayerConfig provideConfig(ConfigManager configManager)
	{
		return configManager.getConfig(ChatHighlightPlayerConfig.class);
	}
}
