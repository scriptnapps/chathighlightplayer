package com.chathighlightplayer;

import net.runelite.api.Client;
import net.runelite.api.Perspective;
import net.runelite.api.Player;
import net.runelite.client.ui.overlay.Overlay;
import net.runelite.client.ui.overlay.OverlayLayer;
import net.runelite.client.ui.overlay.OverlayPosition;
import net.runelite.client.ui.overlay.OverlayUtil;
import net.runelite.api.coords.WorldPoint;
import net.runelite.api.coords.LocalPoint;
import net.runelite.api.Point;

import javax.inject.Inject;
import java.awt.*;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

public class ChatHighlightPlayerOverlay extends Overlay {
    private static final int MIN_LINE_START_PADDING = 18;
    private static final int MAX_LINE_START_PADDING = 42;
    private static final int MIN_LINE_END_PADDING = 10;
    private static final int MAX_LINE_END_PADDING = 28;
    private static final double LINE_START_PADDING_RATIO = 0.08d;
    private static final double LINE_END_PADDING_RATIO = 0.05d;
    private Map<Player, HighlightStyle> highlightedPlayers = Collections.emptyMap();
    private boolean trimLines = true;

    @Inject
    private Client client;

    @Inject
    public ChatHighlightPlayerOverlay() {
		setPosition(OverlayPosition.DYNAMIC);
		setLayer(OverlayLayer.UNDER_WIDGETS);
    }

    public void setHighlightedPlayers(Map<Player, HighlightStyle> highlightedPlayers) {
        this.highlightedPlayers = new LinkedHashMap<>(highlightedPlayers);
    }

    public void setTrimLines(boolean trimLines) {
        this.trimLines = trimLines;
    }

    @Override
    public Dimension render(Graphics2D graphics) {
        for (Map.Entry<Player, HighlightStyle> entry : highlightedPlayers.entrySet()) {
            renderHighlight(graphics, entry.getKey(), entry.getValue());
        }
        return null;
    }

    private void renderHighlight(Graphics2D graphics, Player targetPlayer, HighlightStyle style) {
        if (targetPlayer == null) {
            return;
        }

        Color color = style.getColor();
        WorldPoint targetWorldPos = targetPlayer.getWorldLocation();
        LocalPoint targetLocalPos = LocalPoint.fromWorld(client, targetWorldPos);

        if (style.isShowLine() && client.getLocalPlayer() != null) {
            WorldPoint myWorldPos = client.getLocalPlayer().getWorldLocation();
            LocalPoint myLocalPos = LocalPoint.fromWorld(client, myWorldPos);

            if (myLocalPos != null && targetLocalPos != null) {
                Point myScreenPos = Perspective.localToCanvas(client, myLocalPos, client.getPlane());
                Point targetScreenPos = Perspective.localToCanvas(client, targetLocalPos, client.getPlane());

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

        if (targetLocalPos != null) {
            Point minimapPoint = Perspective.localToMinimap(client, targetLocalPos);
            if (minimapPoint != null) {
                graphics.setColor(color);
                graphics.fillOval(minimapPoint.getX() - 2, minimapPoint.getY() - 2, 4, 4);
            }
        }

        OverlayUtil.renderActorOverlay(graphics, targetPlayer, style.isShowName() ? targetPlayer.getName() : "", color);
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
}
