package com.chathighlightplayer;

import net.runelite.api.Client;
import net.runelite.api.Point;
import net.runelite.api.Perspective;
import net.runelite.api.Player;
import net.runelite.api.coords.LocalPoint;
import net.runelite.api.coords.WorldPoint;
import net.runelite.client.ui.overlay.Overlay;
import net.runelite.client.ui.overlay.OverlayLayer;
import net.runelite.client.ui.overlay.OverlayPosition;

import javax.inject.Inject;
import java.awt.AlphaComposite;
import java.awt.Color;
import java.awt.Composite;
import java.awt.Dimension;
import java.awt.Graphics2D;
import java.util.Collections;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;

public class ChatHighlightPlayerMinimapOverlay extends Overlay {
    private static final int DEFAULT_FADE_DURATION_MS = 400;
    private static final int MIN_FADE_DURATION_MS = 100;
    private static final int MAX_FADE_DURATION_MS = 2000;

    private Map<String, RenderedHighlight> highlightedPlayers = Collections.emptyMap();
    private final Map<String, RenderedHighlight> fadingPlayers = new LinkedHashMap<>();
    private boolean fadeHighlights = true;
    private int fadeDurationMs = DEFAULT_FADE_DURATION_MS;

    @Inject
    private Client client;

    @Inject
    private ChatHighlightPlayerConfig config;

    @Inject
    public ChatHighlightPlayerMinimapOverlay() {
        setPosition(OverlayPosition.DYNAMIC);
        // Always render above widgets so minimap dot remains visible regardless of plugin overlay settings
        setLayer(OverlayLayer.ABOVE_WIDGETS);
    }

    void setHighlightedPlayers(Map<Player, HighlightStyle> highlightedPlayers) {
        long now = System.currentTimeMillis();
        Map<String, RenderedHighlight> updatedHighlights = new LinkedHashMap<>();

        for (Map.Entry<Player, HighlightStyle> entry : highlightedPlayers.entrySet()) {
            Player player = entry.getKey();
            if (player == null || player.getName() == null) {
                continue;
            }

            String playerName = normalizePlayerName(player.getName());
            updatedHighlights.put(playerName, new RenderedHighlight(player, entry.getValue()));
            fadingPlayers.remove(playerName);
        }

        if (fadeHighlights) {
            for (Map.Entry<String, RenderedHighlight> entry : this.highlightedPlayers.entrySet()) {
                if (!updatedHighlights.containsKey(entry.getKey()) && !fadingPlayers.containsKey(entry.getKey())) {
                    RenderedHighlight fadingHighlight = entry.getValue();
                    fadingHighlight.startFade(now);
                    fadingPlayers.put(entry.getKey(), fadingHighlight);
                }
            }
        } else {
            fadingPlayers.clear();
        }

        this.highlightedPlayers = updatedHighlights;
    }

    void setFadeHighlights(boolean fadeHighlights) {
        this.fadeHighlights = fadeHighlights;
        if (!fadeHighlights) {
            fadingPlayers.clear();
        }
    }

    void setFadeDurationMs(int fadeDurationMs) {
        this.fadeDurationMs = clamp(fadeDurationMs, MIN_FADE_DURATION_MS, MAX_FADE_DURATION_MS);
    }

    @Override
    public Dimension render(Graphics2D graphics) {
        long now = System.currentTimeMillis();
        for (RenderedHighlight highlight : highlightedPlayers.values()) {
            renderDot(graphics, highlight.getPlayer(), highlight.getStyle(), 1.0f);
        }

        Iterator<Map.Entry<String, RenderedHighlight>> iterator = fadingPlayers.entrySet().iterator();
        while (iterator.hasNext()) {
            RenderedHighlight highlight = iterator.next().getValue();
            float fadeAlpha = highlight.getFadeAlpha(now, fadeDurationMs);
            if (fadeAlpha <= 0.0f) {
                iterator.remove();
                continue;
            }

            renderDot(graphics, highlight.getPlayer(), highlight.getStyle(), fadeAlpha);
        }
        return null;
    }

    private void renderDot(Graphics2D graphics, Player targetPlayer, HighlightStyle style, float alpha) {
        if (targetPlayer == null) {
            return;
        }

        Composite originalComposite = graphics.getComposite();
        graphics.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, alpha));

        Color color = style.getColor();
        WorldPoint targetWorldPos = targetPlayer.getWorldLocation();
        LocalPoint targetLocalPos = LocalPoint.fromWorld(client, targetWorldPos);

        if (config.showMinimapDot() && targetLocalPos != null) {
            Point minimapPoint = Perspective.localToMinimap(client, targetLocalPos);
            if (minimapPoint != null) {
                graphics.setColor(color);
                int size = config.minimapDotSize().size();
                int half = size / 2;
                graphics.fillOval(minimapPoint.getX() - half, minimapPoint.getY() - half, size, size);
            }
        }

        graphics.setComposite(originalComposite);
    }

    private int clamp(int value, int min, int max) {
        return Math.max(min, Math.min(max, value));
    }

    private String normalizePlayerName(String name) {
        return name.toLowerCase().trim();
    }

    private static final class RenderedHighlight {
        private final Player player;
        private final HighlightStyle style;
        private long fadeStartTime = -1L;

        private RenderedHighlight(Player player, HighlightStyle style) {
            this.player = player;
            this.style = style;
        }

        private Player getPlayer() {
            return player;
        }

        private HighlightStyle getStyle() {
            return style;
        }

        private void startFade(long now) {
            fadeStartTime = now;
        }

        private float getFadeAlpha(long now, int fadeDurationMs) {
            if (fadeStartTime < 0L) {
                return 1.0f;
            }

            long elapsed = now - fadeStartTime;
            if (elapsed >= fadeDurationMs) {
                return 0.0f;
            }

            return 1.0f - (float) elapsed / fadeDurationMs;
        }
    }
}
