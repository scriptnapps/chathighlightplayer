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

public class ChatHighlightPlayerOverlay extends Overlay {
    private Player targetPlayer;
    private boolean istagged = false;
    private Color color = Color.MAGENTA;
    private boolean showline = true;

    @Inject
    private Client client;

    @Inject
    public ChatHighlightPlayerOverlay() {
        setPosition(OverlayPosition.DYNAMIC);
		setLayer(OverlayLayer.ALWAYS_ON_TOP);
    }

    public void setTargetPlayer(Player player,Color color) {
        this.targetPlayer = player;
        this.color = color;
    }

    public void  setTargetVisible(boolean istagged,boolean line) {
        this.istagged=istagged;
        this.showline = line;
    }

    @Override
    public Dimension render(Graphics2D graphics) {
        if (istagged && targetPlayer != null) {
			
			WorldPoint targetWorldPos = targetPlayer.getWorldLocation();
			LocalPoint targetLocalPos = LocalPoint.fromWorld(client, targetWorldPos);
			
            if(showline && client.getLocalPlayer() != null) {
                // Get my player's position
                WorldPoint myWorldPos = client.getLocalPlayer().getWorldLocation();
                LocalPoint myLocalPos = LocalPoint.fromWorld(client, myWorldPos);

                // Get target player's position
                
                

                // Convert world positions to screen positions
                if (myLocalPos != null && targetLocalPos != null) {
                    Point myScreenPos = Perspective.localToCanvas(client, myLocalPos, client.getPlane());
                    Point targetScreenPos = Perspective.localToCanvas(client, targetLocalPos, client.getPlane());

                    // Ensure both are valid before drawing the line
                    if (myScreenPos != null && targetScreenPos != null) {
                        graphics.setColor(color); // Line color
                        graphics.drawLine(myScreenPos.getX(), myScreenPos.getY(),
                                targetScreenPos.getX(), targetScreenPos.getY());
                    }
                }
            }

			if (targetPlayer != null) {
				if (targetLocalPos != null) {
					Point minimapPoint = Perspective.localToMinimap(client, targetLocalPos);
					if (minimapPoint != null) {
						graphics.setColor(color); // Dot color
						graphics.fillOval(minimapPoint.getX() - 2, minimapPoint.getY() - 2, 4, 4);
					}
				}
			}

            OverlayUtil.renderActorOverlay(graphics, targetPlayer, targetPlayer.getName(), color);
        }
        return null;
    }
}