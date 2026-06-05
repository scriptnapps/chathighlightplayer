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
import net.runelite.client.ui.overlay.OverlayUtil;

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

public class ChatHighlightPlayerOverlay extends Overlay {
    private static final int DEFAULT_FADE_DURATION_MS = 400;
    private static final int MIN_FADE_DURATION_MS = 100;
    private static final int MAX_FADE_DURATION_MS = 2000;
    private static final int MIN_LINE_START_PADDING = 18;
    private static final int MAX_LINE_START_PADDING = 42;
    private static final int MIN_LINE_END_PADDING = 10;
    private static final int MAX_LINE_END_PADDING = 28;
    private static final double LINE_START_PADDING_RATIO = 0.08d;
    private static final double LINE_END_PADDING_RATIO = 0.05d;
    private Map<String, RenderedHighlight> highlightedPlayers = Collections.emptyMap();
    private final Map<String, RenderedHighlight> fadingPlayers = new LinkedHashMap<>();
    private boolean trimLines = true;
    private boolean fadeHighlights = true;
    private int fadeDurationMs = DEFAULT_FADE_DURATION_MS;

    @Inject
    private Client client;

    @Inject
    private ChatHighlightPlayerConfig config;

    @Inject
    public ChatHighlightPlayerOverlay() {
		setPosition(OverlayPosition.DYNAMIC);
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

    void setTrimLines(boolean trimLines) {
        this.trimLines = trimLines;
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
            renderHighlight(graphics, highlight.getPlayer(), highlight.getStyle(), 1.0f);
        }

        Iterator<Map.Entry<String, RenderedHighlight>> iterator = fadingPlayers.entrySet().iterator();
        while (iterator.hasNext()) {
            RenderedHighlight highlight = iterator.next().getValue();
            float fadeAlpha = highlight.getFadeAlpha(now, fadeDurationMs);
            if (fadeAlpha <= 0.0f) {
                iterator.remove();
                continue;
            }

            renderHighlight(graphics, highlight.getPlayer(), highlight.getStyle(), fadeAlpha);
        }
        return null;
    }

    private void renderHighlight(Graphics2D graphics, Player targetPlayer, HighlightStyle style, float alpha) {
        if (targetPlayer == null) {
            return;
        }

        Composite originalComposite = graphics.getComposite();
        graphics.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, alpha));

        Color color = style.getColor();
        WorldPoint targetWorldPos = targetPlayer.getWorldLocation();
        LocalPoint targetLocalPos = LocalPoint.fromWorld(client, targetWorldPos);

        if (style.isShowLine() && client.getLocalPlayer() != null) {
            WorldPoint myWorldPos = client.getLocalPlayer().getWorldLocation();
            LocalPoint myLocalPos = LocalPoint.fromWorld(client, myWorldPos);

            if (myLocalPos != null && targetLocalPos != null) {
                @SuppressWarnings("deprecation")
                int plane = client.getPlane();
                Point myScreenPos = Perspective.localToCanvas(client, myLocalPos, plane);
                Point targetScreenPos = Perspective.localToCanvas(client, targetLocalPos, plane);

                if (myScreenPos != null && targetScreenPos != null) {
                    graphics.setColor(color);
                    if (trimLines) {
                        drawTrimmedLine(graphics, myScreenPos, targetScreenPos);
                    } else {
                        graphics.drawLine(myScreenPos.getX(), myScreenPos.getY(),
                                targetScreenPos.getX(), targetScreenPos.getY());
                    }
                }
            }
        }

        if (config.showMinimapDot() && targetLocalPos != null) {
            Point minimapPoint = Perspective.localToMinimap(client, targetLocalPos);
            if (minimapPoint != null) {
                graphics.setColor(color);
                graphics.fillOval(minimapPoint.getX() - 3, minimapPoint.getY() - 3, 6, 6);
            }
        }

        OverlayUtil.renderActorOverlay(graphics, targetPlayer, style.isShowName() ? targetPlayer.getName() : "", color);
        graphics.setComposite(originalComposite);
    }

    private void drawTrimmedLine(Graphics2D graphics, Point start, Point end) {
        int dx = end.getX() - start.getX();
        int dy = end.getY() - start.getY();
        double length = Math.hypot(dx, dy);
        int startPadding = clamp((int) Math.round(length * LINE_START_PADDING_RATIO), MIN_LINE_START_PADDING, MAX_LINE_START_PADDING);
        int endPadding = clamp((int) Math.round(length * LINE_END_PADDING_RATIO), MIN_LINE_END_PADDING, MAX_LINE_END_PADDING);

        if (length <= startPadding + endPadding) {
            return;
        }

        double unitX = dx / length;
        double unitY = dy / length;

        int trimmedStartX = (int) Math.round(start.getX() + unitX * startPadding);
        int trimmedStartY = (int) Math.round(start.getY() + unitY * startPadding);
        int trimmedEndX = (int) Math.round(end.getX() - unitX * endPadding);
        int trimmedEndY = (int) Math.round(end.getY() - unitY * endPadding);

        graphics.drawLine(trimmedStartX, trimmedStartY, trimmedEndX, trimmedEndY);
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
