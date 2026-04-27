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
import net.runelite.client.events.ConfigChanged;
import net.runelite.client.plugins.Plugin;
import net.runelite.client.plugins.PluginDescriptor;
import net.runelite.api.Client;
import net.runelite.api.events.GameTick;
import net.runelite.client.ui.overlay.OverlayManager;
import net.runelite.client.util.Text;

import java.awt.Toolkit;
import java.awt.Color;
import java.awt.datatransfer.StringSelection;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;


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
	private Map<String, HighlightStyle> alwaysHighlightedPlayers = Collections.emptyMap();

	private static final String REPORT = "Report";
	private static final String TRADE = "Accept trade";
	private static final String COPY_USERNAME = "Copy Username";
	private static final String CONFIG_GROUP = "chathighlightplayer";

	@Subscribe
	public void onGameTick(GameTick event) {
		initiatehighlight();
	}

	@Subscribe
	public void onConfigChanged(ConfigChanged event)
	{
		if (!CONFIG_GROUP.equals(event.getGroup()))
		{
			return;
		}

		rebuildAlwaysHighlightCache();
		overlay.setTrimLines(config.trimHighlightLines());
		initiatehighlight();
	}

	private boolean isHighlightActive()
	{
		if (!isActive)
		{
			return false;
		}

		long currentTime = System.currentTimeMillis();
		if (currentTime >= startTime + durationMs * 1000)
		{
			isActive = false;
			return false;
		}

		return true;
	}

	private void initiatehighlight(){
		boolean temporaryHighlightActive = isHighlightActive() && targetPlayerName != null && targetPlayerName.length() > 1;
		if (alwaysHighlightedPlayers.isEmpty() && !temporaryHighlightActive)
		{
			overlay.setHighlightedPlayers(Collections.emptyMap());
			return;
		}

		Map<String, HighlightStyle> configuredHighlights = new LinkedHashMap<>(alwaysHighlightedPlayers);

		if (temporaryHighlightActive) {
			configuredHighlights.put(targetPlayerName.toLowerCase(Locale.ENGLISH), new HighlightStyle(color, showline, config.showTemporaryPlayerName()));
		}

		Map<Player, HighlightStyle> highlightedPlayers = new LinkedHashMap<>();
		for (Player player : client.getPlayers()) {
			if (player == null || player.getName() == null) {
				continue;
			}

			HighlightStyle style = configuredHighlights.get(normalizePlayerName(player.getName()).toLowerCase(Locale.ENGLISH));
			if (style != null) {
				highlightedPlayers.put(player, style);
			}
		}

		overlay.setHighlightedPlayers(highlightedPlayers);
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

	private String buildHighlightedPlayerTarget(String rawTarget, Color highlightColor)
	{
		String cleanedTarget = cleanPlayerName(rawTarget);
		String levelSuffix = "";
		int levelIdx = cleanedTarget.indexOf(" (level-");
		if (levelIdx >= 0)
		{
			levelSuffix = cleanedTarget.substring(levelIdx);
			cleanedTarget = cleanedTarget.substring(0, levelIdx);
		}

		String hexColor = colorToHex(highlightColor).replace("#", "");
		if (levelSuffix.isEmpty())
		{
			return "<col=" + hexColor + ">" + cleanedTarget + "</col>";
		}

		return "<col=" + hexColor + ">" + cleanedTarget + "</col><col=ff9040>" + levelSuffix + "</col>";
	}

	private HighlightStyle getMenuHighlightStyle(String playerName)
	{
		if (playerName == null || playerName.length() <= 1)
		{
			return null;
		}

		if (isHighlightActive() && targetPlayerName != null && playerName.equalsIgnoreCase(targetPlayerName))
		{
			return new HighlightStyle(color, showline, config.showTemporaryPlayerName());
		}

		return alwaysHighlightedPlayers.get(playerName.toLowerCase(Locale.ENGLISH));
	}

	private void highlightMatchingMenuEntry(MenuEntry menuEntry)
	{
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

		HighlightStyle optionStyle = getMenuHighlightStyle(optionName);
		HighlightStyle targetStyle = getMenuHighlightStyle(targetName);
		boolean isCondensedParent = config.highlightCondensedPlayerName()
			&& menuEntry.getSubMenu() != null
			&& menuEntry.getType() == MenuAction.RUNELITE
			&& menuEntry.getOption().isEmpty();
		if (isCondensedParent && optionStyle != null && !menuEntry.getOption().equals("Trade with"))
		{
			String hexColor = colorToHex(optionStyle.getColor()).replace("#", "");
			menuEntry.setOption("<col=" + hexColor + ">" + menuEntry.getOption() + "</col>");
		}
		if (isCondensedParent && targetStyle != null)
		{
			menuEntry.setTarget(buildHighlightedPlayerTarget(menuEntry.getTarget(), targetStyle.getColor()));
		}
		if (menuEntry.getOption().equals("Trade with") && targetStyle != null)
		{
			String hexColor = colorToHex(targetStyle.getColor()).replace("#", "");
			menuEntry.setOption("<col=" + hexColor + ">" + menuEntry.getOption() + "</col>");
		}
	}

	private void addChatHighlightMenuEntry(String username, String target)
	{
		Color customColor = config.tagColor();
		String hexColor = colorToHex(customColor);
		client.createMenuEntry(1)
				.setOption("<col=" + hexColor.replace("#", "") + ">" + "Highlight Player" + "</col>")
				.setTarget(target)
				.setType(MenuAction.RUNELITE_HIGH_PRIORITY)
				.onClick(e -> setHighlightPlayer(username));
	}

	private void addCopyUsernameMenuEntry(String username, String target)
	{
		client.createMenuEntry(1)
				.setOption(COPY_USERNAME)
				.setTarget(target)
				.setType(MenuAction.RUNELITE_HIGH_PRIORITY)
				.onClick(e -> Toolkit.getDefaultToolkit().getSystemClipboard().setContents(new StringSelection(username), null));
	}

	private boolean isChatboxMessageEntry(int packedWidgetId)
	{
		final int groupId = WidgetUtil.componentToInterface(packedWidgetId);
		final int childId = WidgetUtil.componentToId(packedWidgetId);
		if (groupId != InterfaceID.CHATBOX)
		{
			return false;
		}

		final Widget widget = client.getWidget(groupId, childId);
		if (widget == null)
		{
			return false;
		}

		final Widget parent = widget.getParent();
		if (parent == null || ComponentID.CHATBOX_MESSAGE_LINES != parent.getId())
		{
			return false;
		}

		final int first = WidgetUtil.componentToId(ComponentID.CHATBOX_FIRST_MESSAGE);
		final int dynamicChildId = (childId - first) * 4 + 1;
		return parent.getChild(dynamicChildId) != null;
	}

	private boolean isChatboxReportMenuEntry(MenuEntry menuEntry)
	{
		return REPORT.equals(menuEntry.getOption()) && isChatboxMessageEntry(menuEntry.getParam1());
	}

	private boolean hasMenuEntryContaining(String text)
	{
		for (MenuEntry menuEntry : client.getMenu().getMenuEntries())
		{
			if (menuEntry.getOption().contains(text))
			{
				return true;
			}
		}

		return false;
	}

	private void maybeAddChatMenuEntries(String username, String target, boolean includeHighlightEntry)
	{
		if (username.trim().length() <= 1)
		{
			return;
		}

		if (includeHighlightEntry && !hasMenuEntryContaining("Highlight Player"))
		{
			addChatHighlightMenuEntry(username, target);
		}

		if (config.copyUsernameToClipboard() && !hasMenuEntryContaining(COPY_USERNAME))
		{
			addCopyUsernameMenuEntry(username, target);
		}
	}

	private void addConfiguredHighlights(Map<String, HighlightStyle> configuredHighlights, String playerNames, Color highlightColor, boolean lineEnabled, boolean showName)
	{
		for (String playerName : parseConfiguredNames(playerNames))
		{
			configuredHighlights.putIfAbsent(playerName.toLowerCase(Locale.ENGLISH), new HighlightStyle(highlightColor, lineEnabled, showName));
		}
	}

	private Set<String> parseConfiguredNames(String playerNames)
	{
		Set<String> parsedNames = new LinkedHashSet<>();
		if (playerNames == null || playerNames.trim().isEmpty())
		{
			return parsedNames;
		}

		for (String rawName : playerNames.split(","))
		{
			String normalizedName = normalizePlayerName(rawName);
			if (normalizedName.length() > 1)
			{
				parsedNames.add(normalizedName);
			}
		}

		return parsedNames;
	}

	private void rebuildAlwaysHighlightCache()
	{
		Map<String, HighlightStyle> configuredHighlights = new LinkedHashMap<>();
		if (config.alwaysHighlightEnabledOne())
		{
			addConfiguredHighlights(configuredHighlights, config.alwaysHighlightPlayersOne(), config.alwaysHighlightColorOne(), config.alwaysHighlightLineOne(), config.alwaysHighlightShowNameOne());
		}
		if (config.alwaysHighlightEnabledTwo())
		{
			addConfiguredHighlights(configuredHighlights, config.alwaysHighlightPlayersTwo(), config.alwaysHighlightColorTwo(), config.alwaysHighlightLineTwo(), config.alwaysHighlightShowNameTwo());
		}
		if (config.alwaysHighlightEnabledThree())
		{
			addConfiguredHighlights(configuredHighlights, config.alwaysHighlightPlayersThree(), config.alwaysHighlightColorThree(), config.alwaysHighlightLineThree(), config.alwaysHighlightShowNameThree());
		}
		alwaysHighlightedPlayers = configuredHighlights;
	}

	private void moveHighlightPlayerEntryToTop()
	{
		MenuEntry[] menuEntries = client.getMenuEntries();
		List<MenuEntry> reordered = new ArrayList<>(menuEntries.length);
		MenuEntry highlightEntry = null;

		for (MenuEntry menuEntry : menuEntries)
		{
			if (menuEntry.getOption().contains("Highlight Player"))
			{
				highlightEntry = menuEntry;
				continue;
			}

			reordered.add(menuEntry);
		}

		if (highlightEntry != null)
		{
			reordered.add(highlightEntry);
			client.setMenuEntries(reordered.toArray(new MenuEntry[0]));
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

		if (!isChatboxMessageEntry(entry.getActionParam1()))
		{
			return;
		}

		if (entry.getOption().toLowerCase().contains(TRADE.toLowerCase()) ) {
			String username = cleanPlayerName(entry.getTarget());
			if (!hasMenuEntryContaining("Highlight Player"))
			{
				Color customColor = config.tagColor();
				String hexColor = colorToHex(customColor);
				client.createMenuEntry(1)
						.setOption("<col=" + hexColor.replace("#", "") + ">" + "Highlight Player" + "</col>")
						.setTarget(entry.getTarget())
						.setType(MenuAction.WIDGET_SECOND_OPTION)
						.onClick(e -> setHighlightPlayer(username));
			}
			if (config.copyUsernameToClipboard() && !hasMenuEntryContaining(COPY_USERNAME))
			{
				addCopyUsernameMenuEntry(username, entry.getTarget());
			}
			return;
		}

		if (entry.getOption().equals(REPORT) && config.showHoverHighlight()) {
			String username = cleanPlayerName(entry.getTarget());
			maybeAddChatMenuEntries(username, entry.getTarget(), true);
		}

	}

	@Subscribe
	public void onMenuOpened(MenuOpened event)
	{
		if (!config.showHoverHighlight())
		{
			for (MenuEntry menuEntry : client.getMenuEntries())
			{
				if (isChatboxReportMenuEntry(menuEntry))
				{
					String username = cleanPlayerName(menuEntry.getTarget());
					if (username.trim().length() > 1 && (!config.showHoverHighlight() || config.copyUsernameToClipboard()))
					{
						maybeAddChatMenuEntries(username, menuEntry.getTarget(), !config.showHoverHighlight());
						moveHighlightPlayerEntryToTop();
					}
					break;
				}
			}
		}

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
		rebuildAlwaysHighlightCache();
		overlay.setTrimLines(config.trimHighlightLines());
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
