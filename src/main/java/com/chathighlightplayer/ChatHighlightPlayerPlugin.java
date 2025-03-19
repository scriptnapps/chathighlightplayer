package com.chathighlightplayer;

import com.google.inject.Provides;
import javax.inject.Inject;
import lombok.extern.slf4j.Slf4j;
import net.runelite.api.*;
import net.runelite.api.events.MenuEntryAdded;
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
		targetPlayerName = cleanPlayerName(playerName);
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

	@Subscribe
	public void onMenuEntryAdded(MenuEntryAdded entry) {

		if (entry.getMenuEntry().getOption().equals("Trade with")) {
			String uman = "";
			try {
				uman = cleanPlayerName(entry.getMenuEntry().getTarget().replaceAll(" \\(level-\\d+\\)", ""));
			}catch(Exception ignore){}
			if(uman != null && uman.equalsIgnoreCase(targetPlayerName)){
				Color customColor = color;
				String hexColor = colorToHex(customColor);
				entry.getMenuEntry().setOption("<col=" + hexColor.replace("#", "") + ">" + entry.getMenuEntry().getOption() + "</col>");
			}
		}

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
