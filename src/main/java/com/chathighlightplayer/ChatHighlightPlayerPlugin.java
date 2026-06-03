package com.chathighlightplayer;

import com.google.inject.Provides;
import javax.inject.Inject;
import lombok.extern.slf4j.Slf4j;
import net.runelite.api.Client;
import net.runelite.api.MenuAction;
import net.runelite.api.MenuEntry;
import net.runelite.api.Player;
import net.runelite.api.events.BeforeMenuRender;
import net.runelite.api.events.GameTick;
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
import net.runelite.client.ui.overlay.OverlayManager;
import net.runelite.client.util.Text;

import java.awt.Color;
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
		name = "Chat Highlight Player",
	description = "Highlight players by clicking their names in chat."
)
@SuppressWarnings({"deprecation", "unused"})
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
	private long durationSeconds = 10;
	private Color color = Color.pink;

	private long startTime = 0;
	private boolean isActive = false;
	private boolean showLine = true;

	private static final String REPORT = "Report";
	private static final String TRADE = "Accept trade";
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

		overlay.setTrimLines(config.trimHighlightLines());
		overlay.setFadeHighlights(config.fadeHighlights());
		overlay.setFadeDurationMs(config.fadeDurationMs());
		initiatehighlight();
	}

	private boolean isHighlightActive()
	{
		if (!isActive)
		{
			return false;
		}

		long currentTime = System.currentTimeMillis();
		long effectiveDuration = Math.max(0, Math.min(durationSeconds, 120)); // enforce max 120s regardless of config
		if (currentTime >= startTime + effectiveDuration * 1000)
		{
			isActive = false;
			return false;
		}

		return true;
	}

	private void initiatehighlight(){
		boolean temporaryHighlightActive = isHighlightActive() && targetPlayerName != null && targetPlayerName.length() > 1;
		if (!temporaryHighlightActive)
		{
			overlay.setHighlightedPlayers(Collections.emptyMap());
			return;
		}

		Map<Player, HighlightStyle> highlightedPlayers = new LinkedHashMap<>();
		String targetNormalized = targetPlayerName.toLowerCase(Locale.ENGLISH);
		HighlightStyle tempStyle = new HighlightStyle(color, showLine, config.showTemporaryPlayerName(), config.temporaryMenuOption(), config.temporaryHighlightRegularMenuPlayerName(), config.temporaryHideOtherPlayerMenus());
		for (Player player : client.getPlayers()) {
			if (player == null || player.getName() == null) {
				continue;
			}

			if (normalizePlayerName(player.getName()).toLowerCase(Locale.ENGLISH).equals(targetNormalized)) {
				highlightedPlayers.put(player, tempStyle);
			}
		}

		overlay.setHighlightedPlayers(highlightedPlayers);
	}

	private void setHighlightPlayer(String playerName) {
		targetPlayerName = "";
		targetPlayerName = normalizePlayerName(playerName);
		startTime = System.currentTimeMillis();
		isActive = true;
		showLine = config.line();
		// Use selected preset duration (seconds)
		durationSeconds = config.duration().seconds();
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

		if (isHighlightActive() && playerName.equalsIgnoreCase(targetPlayerName))
		{
			return new HighlightStyle(color, showLine, config.showTemporaryPlayerName(), config.temporaryMenuOption(), config.temporaryHighlightRegularMenuPlayerName(), config.temporaryHideOtherPlayerMenus());
		}

		return null;
	}

	private boolean shouldSkipMenuOptionHighlight(String configuredMenuOption)
	{
		if (configuredMenuOption == null || configuredMenuOption.trim().isEmpty())
		{
			return true;
		}

		for (String rawConfiguredOption : configuredMenuOption.split(","))
		{
			String cleanConfiguredOption = Text.removeTags(rawConfiguredOption).trim();
			if (!cleanConfiguredOption.isEmpty() && !cleanConfiguredOption.equalsIgnoreCase("none"))
			{
				return false;
			}
		}

		return true;
	}

	private boolean shouldHighlightMenuOption(String menuOption, String configuredMenuOption)
	{
		if (menuOption == null || shouldSkipMenuOptionHighlight(configuredMenuOption))
		{
			return false;
		}

		String cleanMenuOption = Text.removeTags(menuOption).trim();
		for (String rawConfiguredOption : configuredMenuOption.split(","))
		{
			String cleanConfiguredOption = Text.removeTags(rawConfiguredOption).trim();
			if (cleanConfiguredOption.equals("*") || cleanMenuOption.equalsIgnoreCase(cleanConfiguredOption))
			{
				return true;
			}
		}

		return false;
	}

	private void highlightMatchingMenuEntry(MenuEntry menuEntry)
	{
		String optionName = normalizePlayerName(menuEntry.getOption());
		String targetName = normalizePlayerName(menuEntry.getTarget());

		HighlightStyle optionStyle = getMenuHighlightStyle(optionName);
		HighlightStyle targetStyle = getMenuHighlightStyle(targetName);
		boolean isCondensedParent = menuEntry.getSubMenu() != null
			&& menuEntry.getType() == MenuAction.RUNELITE
			&& menuEntry.getOption().isEmpty();
		if (isCondensedParent && optionStyle != null && !shouldSkipMenuOptionHighlight(optionStyle.getMenuOption()))
		{
			String hexColor = colorToHex(optionStyle.getColor()).replace("#", "");
			menuEntry.setOption("<col=" + hexColor + ">" + menuEntry.getOption() + "</col>");
		}
		boolean shouldHighlightPlayerName = targetStyle != null
			&& ((isCondensedParent && config.highlightCondensedPlayerName())
				|| (!isCondensedParent && targetStyle.isHighlightRegularMenuPlayerName()));
		if (shouldHighlightPlayerName)
		{
			menuEntry.setTarget(buildHighlightedPlayerTarget(menuEntry.getTarget(), targetStyle.getColor()));
		}
		if (targetStyle != null && shouldHighlightMenuOption(menuEntry.getOption(), targetStyle.getMenuOption()))
		{
			String hexColor = colorToHex(targetStyle.getColor()).replace("#", "");
			menuEntry.setOption("<col=" + hexColor + ">" + menuEntry.getOption() + "</col>");
		}
	}

	private Set<String> getKnownPlayerNames()
	{
		Set<String> playerNames = new LinkedHashSet<>();
		for (Player player : client.getPlayers())
		{
			if (player != null && player.getName() != null)
			{
				playerNames.add(normalizePlayerName(player.getName()).toLowerCase(Locale.ENGLISH));
			}
		}

		return playerNames;
	}

	private String getMenuEntryPlayerName(MenuEntry menuEntry, Set<String> knownPlayerNames)
	{
		String targetName = normalizePlayerName(menuEntry.getTarget()).toLowerCase(Locale.ENGLISH);
		if (knownPlayerNames.contains(targetName))
		{
			return targetName;
		}

		String optionName = normalizePlayerName(menuEntry.getOption()).toLowerCase(Locale.ENGLISH);
		if (knownPlayerNames.contains(optionName))
		{
			return optionName;
		}

		return null;
	}

	private MenuEntry[] filterMenuEntriesForHighlightedPlayers(MenuEntry[] menuEntries)
	{
		Set<String> knownPlayerNames = getKnownPlayerNames();
		if (knownPlayerNames.isEmpty())
		{
			return menuEntries;
		}

		Set<String> focusedPlayerNames = new LinkedHashSet<>();
		for (MenuEntry menuEntry : menuEntries)
		{
			String playerName = getMenuEntryPlayerName(menuEntry, knownPlayerNames);
			if (playerName == null)
			{
				continue;
			}

			HighlightStyle style = getMenuHighlightStyle(playerName);
			if (style != null && style.isHideOtherPlayerMenus())
			{
				focusedPlayerNames.add(playerName);
			}
		}

		if (focusedPlayerNames.isEmpty())
		{
			return menuEntries;
		}

		List<MenuEntry> filteredEntries = new ArrayList<>(menuEntries.length);
		for (MenuEntry menuEntry : menuEntries)
		{
			String playerName = getMenuEntryPlayerName(menuEntry, knownPlayerNames);
			if (playerName == null || focusedPlayerNames.contains(playerName))
			{
				filteredEntries.add(menuEntry);
			}
		}

		return filteredEntries.toArray(new MenuEntry[0]);
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

	private boolean isMenuEntryMissing(String text)
	{
		for (MenuEntry menuEntry : client.getMenu().getMenuEntries())
		{
			if (menuEntry.getOption().contains(text))
			{
				return false;
			}
		}

		return true;
	}

	private void maybeAddChatMenuEntries(String username, String target, boolean includeHighlightEntry)
	{
		if (username.trim().length() <= 1)
		{
			return;
		}

		if (includeHighlightEntry && isMenuEntryMissing("Highlight Player"))
		{
			addChatHighlightMenuEntry(username, target);
		}
	}

	private String cleanPlayerName(String name) {
		if (name == null)
		{
			return "";
		}

		return Text.removeTags(name)
				.replace('\u00A0', ' ')
				.trim();
	}

	private String normalizePlayerName(String name)
	{
		return cleanPlayerName(name)
				.replaceAll("\\s*\\(level-\\d+\\)$", "")
				.trim();
	}

	@Override
	protected void startUp()
	{
		log.info("ChatHighlightPlayerPlugin started!");
		overlay.setTrimLines(config.trimHighlightLines());
		overlay.setFadeHighlights(config.fadeHighlights());
		overlay.setFadeDurationMs(config.fadeDurationMs());
		overlayManager.add(overlay);
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
			if (isMenuEntryMissing("Highlight Player"))
			{
				Color customColor = config.tagColor();
				String hexColor = colorToHex(customColor);
				client.createMenuEntry(1)
						.setOption("<col=" + hexColor.replace("#", "") + ">" + "Highlight Player" + "</col>")
						.setTarget(entry.getTarget())
						.setType(MenuAction.WIDGET_SECOND_OPTION)
						.onClick(e -> setHighlightPlayer(username));
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
		for (MenuEntry menuEntry : client.getMenuEntries())
		{
			if (isChatboxReportMenuEntry(menuEntry))
			{
				String username = cleanPlayerName(menuEntry.getTarget());
				if (username.trim().length() > 1)
				{
					maybeAddChatMenuEntries(username, menuEntry.getTarget(), true);
					moveHighlightPlayerEntryToTop();
				}
				break;
			}
		}

		MenuEntry[] menuEntries = filterMenuEntriesForHighlightedPlayers(client.getMenuEntries());
		for (MenuEntry menuEntry : menuEntries)
		{
			highlightMatchingMenuEntry(menuEntry);
		}
		client.setMenuEntries(menuEntries);
	}

	@Subscribe
	public void onBeforeMenuRender(BeforeMenuRender event)
	{
		MenuEntry[] menuEntries = filterMenuEntriesForHighlightedPlayers(client.getMenuEntries());
		for (MenuEntry menuEntry : menuEntries)
		{
			highlightMatchingMenuEntry(menuEntry);
		}
		client.setMenuEntries(menuEntries);
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

	@Override
	protected void shutDown()
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
