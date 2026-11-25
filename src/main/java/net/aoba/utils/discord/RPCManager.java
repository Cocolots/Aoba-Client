/*
 * Aoba Hacked Client
 * Copyright (C) 2019-2024 coltonk9043
 *
 * Licensed under the GNU General Public License, Version 3 or later.
 * See <http://www.gnu.org/licenses/>.
 */

package net.aoba.utils.discord;

import com.mojang.logging.LogUtils;

import net.aoba.gui.screens.*;
import net.aoba.gui.screens.alts.AddAltScreen;
import net.aoba.gui.screens.alts.AltScreen;
import net.aoba.gui.screens.alts.EditAltScreen;
import net.aoba.gui.screens.proxy.AddProxyScreen;
import net.aoba.gui.screens.proxy.EditProxyScreen;
import net.aoba.gui.screens.proxy.ProxyScreen;
import net.minecraft.client.gui.screens.ManageServerScreen;
import net.minecraft.client.gui.screens.TitleScreen;
import net.minecraft.client.gui.screens.WinScreen;
import net.minecraft.client.gui.screens.multiplayer.JoinMultiplayerScreen;
import net.minecraft.client.gui.screens.options.OptionsScreen;
import net.minecraft.client.gui.screens.worldselection.CreateWorldScreen;
import net.minecraft.client.gui.screens.worldselection.EditWorldScreen;
import net.minecraft.client.gui.screens.worldselection.SelectWorldScreen;

import static net.aoba.AobaClient.MC;

public class RPCManager {
    public static volatile boolean started;
    public static DiscordRPC presence = new DiscordRPC();

    private static Discord rpc;
    private static Thread thread;

    public boolean startRpc() {
        synchronized (RPCManager.class) {
            if (started) {
                return true;
            }
            if (!isNativeLibrarySupported(System.getProperty("os.name", ""),
                    System.getProperty("os.arch", ""))) {
                LogUtils.getLogger().warn("Discord RPC is unavailable on this platform architecture");
                return false;
            }

            try {
                Discord discord = Discord.INSTANCE;
                DiscordEventHandlers handlers = new DiscordEventHandlers();
                discord.Discord_Initialize("1497026345862168646", handlers, true, "");
                presence.startTimestamp = System.currentTimeMillis() / 1000L;
                presence.largeImageText = "";
                discord.Discord_UpdatePresence(presence);

                rpc = discord;
                started = true;
                thread = new Thread(() -> runCallbacks(discord), "Aoba-Discord-RPC");
                thread.setDaemon(true);
                thread.start();
                return true;
            } catch (LinkageError | RuntimeException e) {
                started = false;
                rpc = null;
                thread = null;
                LogUtils.getLogger().error("Discord RPC native integration could not be started", e);
                return false;
            }
        }
    }

    private static void runCallbacks(Discord discord) {
        try {
            while (started && !Thread.currentThread().isInterrupted()) {
                synchronized (RPCManager.class) {
                    if (!started) {
                        break;
                    }
                    discord.Discord_RunCallbacks();
                    presence.details = getDetails();
                    presence.state = "MC 26.3";
                    presence.smallImageText = "logged as - " + MC.getUser().getName();
                    presence.smallImageKey = "https://minotar.net/helm/" + MC.getUser().getName() + "/100.png";
                    presence.button_label_1 = "Download";
                    presence.button_url_1 = "https://github.com/Cocolots/Aoba-Client";
                    presence.largeImageKey = "https://i.imgur.com/D64DjG2.png";
                    discord.Discord_UpdatePresence(presence);
                }
                Thread.sleep(2000L);
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        } catch (LinkageError | RuntimeException e) {
            LogUtils.getLogger().error("Discord RPC callback loop stopped", e);
        } finally {
            started = false;
        }
    }

    public void stopRpc() {
        synchronized (RPCManager.class) {
            started = false;
            Discord discord = rpc;
            rpc = null;

            if (thread != null) {
                thread.interrupt();
                thread = null;
            }

            if (discord != null) {
                try {
                    discord.Discord_Shutdown();
                } catch (LinkageError | RuntimeException e) {
                    LogUtils.getLogger().error("Discord RPC native integration could not be shut down", e);
                }
            }
        }
    }

    static boolean isNativeLibrarySupported(String osName, String osArch) {
        String os = osName.toLowerCase();
        String arch = osArch.toLowerCase();
        return !os.contains("mac") || (!arch.equals("aarch64") && !arch.equals("arm64") && !arch.equals("arm"));
    }

    private static String getDetails() {
        String result = "";

        if (MC.gui.screen() instanceof TitleScreen || MC.gui.screen() instanceof MainMenuScreen) {
            result = "In Main menu";
        } else if (MC.gui.screen() instanceof JoinMultiplayerScreen || MC.gui.screen() instanceof ManageServerScreen) {
            result = "Picking a server";
        } else if (MC.getCurrentServer() != null) {
            result = "Playing on a server";
        } else if (MC.isLocalServer()) {
            result = "Playing singleplayer";
        } else if (MC.gui.screen() instanceof OptionsScreen) {
            result = "Editing options";
        } else if (MC.gui.screen() instanceof SelectWorldScreen) {
            result = "Selecting a world";
        } else if (MC.gui.screen() instanceof AobaCreditsScreen) {
            result = "Watching Aoba credits";
        } else if (MC.gui.screen() instanceof WinScreen) {
            result = "Watching credits";
        } else if (MC.gui.screen() instanceof CreateWorldScreen) {
            result = "Creating a world";
        } else if (MC.gui.screen() instanceof EditWorldScreen) {
            result = "Editing a world";
        } else if (MC.gui.screen() instanceof ProxyScreen) {
            result = "Choosing a proxy";
        } else if (MC.gui.screen() instanceof AltScreen) {
            result = "Choosing an alt";
        } else if (MC.gui.screen() instanceof AddProxyScreen) {
            result = "Creating a proxy";
        } else if (MC.gui.screen() instanceof EditProxyScreen) {
            result = "Editing a proxy";
        } else if (MC.gui.screen() instanceof AddAltScreen) {
            result = "Creating an alt";
        } else if (MC.gui.screen() instanceof EditAltScreen) {
            result = "Editing an alt";
        }
        return result;
    }
}
