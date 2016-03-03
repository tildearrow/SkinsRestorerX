package skinsrestorer.bungee.listeners;

import java.io.ByteArrayInputStream;
import java.io.DataInputStream;

import net.md_5.bungee.UserConnection;
import net.md_5.bungee.api.ProxyServer;
import net.md_5.bungee.api.chat.TextComponent;
import net.md_5.bungee.api.connection.ProxiedPlayer;
import net.md_5.bungee.api.event.PluginMessageEvent;
import net.md_5.bungee.api.plugin.Listener;
import net.md_5.bungee.event.EventHandler;
import skinsrestorer.bungee.SkinFactoryBungee;
import skinsrestorer.bungee.SkinStorage;
import skinsrestorer.bungee.SkinsRestorer;
import skinsrestorer.shared.format.SkinProfile;
import skinsrestorer.shared.storage.CooldownStorage;
import skinsrestorer.shared.storage.LocaleStorage;
import skinsrestorer.shared.utils.C;
import skinsrestorer.shared.utils.SkinFetchUtils;
import skinsrestorer.shared.utils.SkinFetchUtils.SkinFetchFailedException;

public class MessageListener implements Listener {

	@EventHandler
	public void onPluginMessage(PluginMessageEvent e) {
		if (!e.getTag().equalsIgnoreCase("BungeeCord"))
			return;

		DataInputStream dis = new DataInputStream(new ByteArrayInputStream(e.getData()));

		try {

			final String channel = dis.readUTF();

			if (!channel.equalsIgnoreCase("SkinsRestorer"))
				return;

			final String player = dis.readUTF();

			final ProxiedPlayer p = ProxyServer.getInstance().getPlayer(player);

			if (p == null || p.getServer() == null)
				return;

			final boolean removeskin = dis.readBoolean();

			if (removeskin)
				SkinFactoryBungee.getFactory().sendUpdateRequest((UserConnection) p, removeskin);

			final String skin = dis.readUTF();

			ProxyServer.getInstance().getScheduler().runAsync(SkinsRestorer.getInstance(), new Runnable() {

				@Override
				public void run() {
					try {

						SkinProfile skinprofile = SkinFetchUtils.fetchSkinProfile(skin, null);
						SkinStorage.getInstance().setSkinData(p.getName(), skinprofile);
						SkinFactoryBungee.getFactory().applySkin(p);
						TextComponent component = new TextComponent(C.c(LocaleStorage.getInstance().PLAYER_SKIN_CHANGE_SUCCESS));
						p.sendMessage(component);
					} catch (SkinFetchFailedException ex) {
						SkinProfile skinprofile = SkinStorage.getInstance().getSkinData(skin);
						if (skinprofile != null) {
							SkinStorage.getInstance().setSkinData(p.getName(), skinprofile);
							SkinFactoryBungee.getFactory().applySkin(p);
							TextComponent component = new TextComponent(C.c(LocaleStorage.getInstance().PLAYER_SKIN_CHANGE_SUCCESS_DATABASE));
							p.sendMessage(component);
						} else {
							TextComponent component = new TextComponent(C.c(LocaleStorage.getInstance().SKIN_FETCH_FAILED + ex.getMessage()));
							p.sendMessage(component);
							CooldownStorage.getInstance().resetCooldown(p.getUniqueId());
						}

					}
				}
			});

		} catch (Exception ex) {
			// These things like to throw exceptions because of possible
			// deformation
			return;
		}

	}

}