/*
 * Aoba Hacked Client
 * Copyright (C) 2019-2024 coltonk9043
 * This maded by Donalp012
 * Licensed under the GNU General Public License, Version 3 or later.
 * See <http://www.gnu.org/licenses/>.
 */

package net.aoba.gui.screens;

import org.apache.commons.lang3.math.NumberUtils;
import net.aoba.managers.ProtocolManager;
import net.aoba.gui.components.widgets.AobaButtonWidget;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

public class ProtocolScreen extends Screen {

    private EditBox versionField;
    private EditBox protocolField;
    private EditBox packVerField;
    private EditBox brandField;
    private AobaButtonWidget addButton;
    private final Screen parent;

    public ProtocolScreen(Screen parent) {
        super(Component.literal("Protocol Screen"));
        this.parent = parent;
    }

    @Override
    protected void init() {
        super.init();

        this.versionField = this.addRenderableWidget(new EditBox(this.font, this.width / 2 - 98, this.height / 2 - 60, 196, 18, Component.empty()));
        this.versionField.setValue(ProtocolManager.OVERRIDE_NAME.isEmpty() ? ProtocolManager.getVersionName() : ProtocolManager.OVERRIDE_NAME);

        this.protocolField = this.addRenderableWidget(new EditBox(this.font, this.width / 2 - 98, this.height / 2 - 35, 196, 18, Component.empty()));
        this.protocolField.setValue(Integer.toString(ProtocolManager.getProtocolVersion()));
        this.protocolField.setResponder(text -> this.updateAddButton());

        this.packVerField = this.addRenderableWidget(new EditBox(this.font, this.width / 2 - 100, this.height / 2 - 10, 196, 18, Component.empty()));
        this.packVerField.setValue(Integer.toString(ProtocolManager.getPackVersion()));
        this.packVerField.setResponder(text -> this.updateAddButton());

        this.brandField = this.addRenderableWidget(new EditBox(this.font, this.width / 2 - 98, this.height / 2 + 15, 80, 18, Component.empty()));
        this.brandField.setValue(ProtocolManager.BRAND);

        int startX = this.width / 2 - 14;

        AobaButtonWidget vButton = new AobaButtonWidget(startX, this.height / 2 + 14, 18, 20, Component.literal("V"));
        vButton.setPressAction(b -> this.brandField.setValue("vanilla"));
        this.addRenderableWidget(vButton);

        AobaButtonWidget faButton = new AobaButtonWidget(startX + 20, this.height / 2 + 14, 22, 20, Component.literal("Fa"));
        faButton.setPressAction(b -> this.brandField.setValue("fabric"));
        this.addRenderableWidget(faButton);

        AobaButtonWidget foButton = new AobaButtonWidget(startX + 44, this.height / 2 + 14, 22, 20, Component.literal("Fo"));
        foButton.setPressAction(b -> this.brandField.setValue("forge"));
        this.addRenderableWidget(foButton);

        AobaButtonWidget qButton = new AobaButtonWidget(startX + 68, this.height / 2 + 14, 18, 20, Component.literal("Q"));
        qButton.setPressAction(b -> this.brandField.setValue("quilt"));
        this.addRenderableWidget(qButton);

        AobaButtonWidget nfButton = new AobaButtonWidget(startX + 88, this.height / 2 + 14, 22, 20, Component.literal("Nf"));
        nfButton.setPressAction(b -> this.brandField.setValue("neoforge"));
        this.addRenderableWidget(nfButton);

        AobaButtonWidget lButton = new AobaButtonWidget(startX + 112, this.height / 2 + 14, 18, 20, Component.literal("L"));
        lButton.setPressAction(b -> this.brandField.setValue("liteloader"));
        this.addRenderableWidget(lButton);

        AobaButtonWidget spoofToggle = new AobaButtonWidget(this.width / 2 - 100, this.height / 2 + 40, 96, 20, Component.literal("Spoof: " + (ProtocolManager.SPOOF_ENABLED ? "ON" : "OFF")));
        spoofToggle.setPressAction(button -> {
            this.syncFieldsToManager();
            ProtocolManager.SPOOF_ENABLED = !ProtocolManager.SPOOF_ENABLED;
            ProtocolManager.save();
            this.rebuildWidgets();
        });
        this.addRenderableWidget(spoofToggle);

        AobaButtonWidget telemetryToggle = new AobaButtonWidget(this.width / 2 + 4, this.height / 2 + 40, 96, 20, Component.literal("Telemetry: " + (ProtocolManager.BLOCK_TELEMETRY ? "BLOCK" : "ALLOW")));
        telemetryToggle.setPressAction(button -> {
            this.syncFieldsToManager();
            ProtocolManager.BLOCK_TELEMETRY = !ProtocolManager.BLOCK_TELEMETRY;
            ProtocolManager.save();
            this.rebuildWidgets();
        });
        this.addRenderableWidget(telemetryToggle);

        // --- FILA DE EXPLOIT PROTECTION ---
        AobaButtonWidget stripToggle = new AobaButtonWidget(this.width / 2 - 100, this.height / 2 + 63, 63, 20, Component.literal("Strip: " + (ProtocolManager.STRIP_KNOWN_PACKS ? "ON" : "OFF")));
        stripToggle.setPressAction(button -> {
            this.syncFieldsToManager();
            ProtocolManager.STRIP_KNOWN_PACKS = !ProtocolManager.STRIP_KNOWN_PACKS;
            ProtocolManager.save();
            this.rebuildWidgets();
        });
        this.addRenderableWidget(stripToggle);

        AobaButtonWidget scanToggle = new AobaButtonWidget(this.width / 2 - 34, this.height / 2 + 63, 65, 20, Component.literal("AntiScan: " + (ProtocolManager.BLOCK_LOCAL_SCAN ? "ON" : "OFF")));
        scanToggle.setPressAction(button -> {
            this.syncFieldsToManager();
            ProtocolManager.BLOCK_LOCAL_SCAN = !ProtocolManager.BLOCK_LOCAL_SCAN;
            ProtocolManager.save();
            this.rebuildWidgets();
        });
        this.addRenderableWidget(scanToggle);

        AobaButtonWidget probeToggle = new AobaButtonWidget(this.width / 2 + 34, this.height / 2 + 63, 66, 20, Component.literal("Probes: " + (ProtocolManager.PROBES_PROTECTION ? "ON" : "OFF")));
        probeToggle.setPressAction(button -> {
            this.syncFieldsToManager();
            ProtocolManager.PROBES_PROTECTION = !ProtocolManager.PROBES_PROTECTION;
            ProtocolManager.save();
            this.rebuildWidgets();
        });
        this.addRenderableWidget(probeToggle);

        this.addButton = new AobaButtonWidget(this.width / 2 - 100, this.height / 2 + 88, 196, 20, Component.literal("Done"));
        this.addButton.setPressAction(button -> {
            this.syncFieldsToManager();
            ProtocolManager.save();
            this.onClose();
        });
        this.addRenderableWidget(this.addButton);

        AobaButtonWidget cancelButton = new AobaButtonWidget(this.width / 2 - 100, this.height / 2 + 111, 196, 20, Component.literal("Cancel"));
        cancelButton.setPressAction(button -> this.onClose());
        this.addRenderableWidget(cancelButton);

        this.updateAddButton();
    }

    private void syncFieldsToManager() {
        ProtocolManager.OVERRIDE_NAME = this.versionField.getValue();
        if (NumberUtils.isDigits(this.protocolField.getValue())) {
            ProtocolManager.OVERRIDE_PROTOCOL = Integer.parseInt(this.protocolField.getValue());
        }
        if (NumberUtils.isDigits(this.packVerField.getValue())) {
            ProtocolManager.OVERRIDE_PACK_VERSION = Integer.parseInt(this.packVerField.getValue());
        }
        ProtocolManager.BRAND = this.brandField.getValue();
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor drawContext, int mouseX, int mouseY, float delta) {
        super.extractRenderState(drawContext, mouseX, mouseY, delta);

        drawContext.text(this.font, "NOTE: Protocol may bug on some Version servers", this.width / 2 - 220, 10, 0xFFFF5555);
        drawContext.text(this.font, "WARNING: May cause crashes or anti-cheat bans on strict servers!", this.width / 2 - 220, 20, 0xFFFF5555);


        drawContext.text(this.font, "Version:", this.width / 2 - 180, this.height / 2 - 55, 0xFFAAAAAA);
        drawContext.text(this.font, "Protocol:", this.width / 2 - 180, this.height / 2 - 30, 0xFFAAAAAA);
        drawContext.text(this.font, "Pack Ver:", this.width / 2 - 180, this.height / 2 - 5, 0xFFAAAAAA);
        drawContext.text(this.font, "Brand:", this.width / 2 - 180, this.height / 2 + 20, 0xFFAAAAAA);

        int infoY = this.height - 65;
        drawContext.text(this.font, "Spoof: Sends fake client version/brand to the server.", 15, infoY, 0xFFFF00FF);
        drawContext.text(this.font, "Telemetry: Blocks background data logs sent to Mojang.", 15, infoY + 10, 0xFFFF00FF);
        drawContext.text(this.font, "Strip: Removes mod signatures to appear completely Vanilla.", 15, infoY + 20, 0xFFFF00FF);
        drawContext.text(this.font, "AntiScan: Blocks resource pack exploits targeting local ports.", 15, infoY + 30, 0xFFFF00FF);
        drawContext.text(this.font, "Probes: Prevents chat exploits used to detect active hacks.", 15, infoY + 40, 0xFFFF00FF);
    }

    @Override
    public void extractBackground(final GuiGraphicsExtractor graphics, final int mouseX, final int mouseY, final float a) {
        super.extractBackground(graphics, mouseX, mouseY, a);
    }

    @Override
    public void onClose() {
        if (this.minecraft != null) {
            this.minecraft.setScreen(this.parent);
        }
    }

    private void updateAddButton() {
        if (this.addButton != null) {
            this.addButton.active = NumberUtils.isDigits(this.protocolField.getValue())
                    && NumberUtils.isDigits(this.packVerField.getValue());
        }
    }
}
