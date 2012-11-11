package me.lukas.skyblockmultiplayer.listeners;

import me.lukas.skyblockmultiplayer.GameMode;
import me.lukas.skyblockmultiplayer.Permissions;
import me.lukas.skyblockmultiplayer.Settings;
import me.lukas.skyblockmultiplayer.Language;
import me.lukas.skyblockmultiplayer.PlayerInfo;
import me.lukas.skyblockmultiplayer.SkyBlockMultiplayer;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDeathEvent;

public class EntityDeath implements Listener {

	@EventHandler
	public void onEntityDeath(EntityDeathEvent event) {
		Entity ent = event.getEntity();
		if (ent.getType() != EntityType.PLAYER) {
			return;
		}

		Player player = (Player) ent;
		if (!player.getWorld().getName().equals(SkyBlockMultiplayer.getSkyBlockWorld().getName())) { // Exit, if player not in SkyBlock
			return;
		}

		PlayerInfo pi = SkyBlockMultiplayer.settings.getPlayerInfo(player.getName());
		if (pi == null) { // Check, if player is in playerlist
			return;
		}

		if (SkyBlockMultiplayer.getInstance().playerIsOnTower(player) && !pi.getIsOnIsland()) {
			pi.setOldInventory(player.getInventory().getContents());
			pi.setOldArmor(player.getInventory().getArmorContents());
			pi.setOldExp(player.getExp());
			pi.setOldLevel(player.getLevel());
			pi.setOldFood(player.getFoodLevel());
			pi.setOldHealth(player.getMaxHealth());

			event.getDrops().clear();
			event.setDroppedExp(0);

			SkyBlockMultiplayer.getInstance().savePlayerInfo(pi);
			return;
		}

		if (SkyBlockMultiplayer.settings.getGameMode() == GameMode.BUILD && SkyBlockMultiplayer.settings.getRespawnWithInventory()) {
			pi.setIslandInventory(player.getInventory().getContents());
			pi.setIslandArmor(player.getInventory().getArmorContents());
			pi.setIslandExp(player.getExp());
			pi.setIslandLevel(player.getLevel());
			pi.setIslandFood(player.getFoodLevel());
			pi.setIslandHealth(player.getMaxHealth());

			event.getDrops().clear();
			event.setDroppedExp(0);

			SkyBlockMultiplayer.getInstance().savePlayerInfo(pi);
			return;
		}

		if (SkyBlockMultiplayer.settings.getGameMode() == GameMode.BUILD) {
			return;
		}

		pi.setDead(true);
		if (!pi.getHasIsland()) {
			return;
		}

		pi.setLivesLeft(pi.getLivesLeft() - 1);
		if (pi.getIslandsLeft() != 0 || pi.getLivesLeft() != 0) {
			return;
		}

		SkyBlockMultiplayer.getInstance().savePlayerInfo(pi);

		if (Settings.numbersPlayers < 1) {
			return;
		}
		Settings.numbersPlayers--;

		for (PlayerInfo pInfo : SkyBlockMultiplayer.settings.getPlayerInfos().values()) {
			if (pInfo.getPlayer() != null) {
				if (pInfo.getPlayer().getWorld().getName().equalsIgnoreCase(SkyBlockMultiplayer.getSkyBlockWorld().getName()) || (Permissions.SKYBLOCK_MESSAGES.has(pInfo.getPlayer()))) {
					pInfo.getPlayer().sendMessage(Language.MSGS_PLAYER_DIED1.getSentence() + Settings.numbersPlayers + Language.MSGS_PLAYER_DIED2.getSentence());
				}
			}
		}

		if (Settings.numbersPlayers == 1) {
			String winner = "";
			for (PlayerInfo pinfo : SkyBlockMultiplayer.settings.getPlayerInfos().values()) {
				if (pinfo.isDead() == false) {
					winner = pinfo.getPlayer().getName();
				}
			}

			for (PlayerInfo pInfo : SkyBlockMultiplayer.settings.getPlayerInfos().values()) {
				if (pInfo.getPlayer() != null) {
					if (pInfo.getPlayer().getWorld().getName().equalsIgnoreCase(SkyBlockMultiplayer.getSkyBlockWorld().getName()) || (Permissions.SKYBLOCK_MESSAGES.has(pInfo.getPlayer()))) {
						pInfo.getPlayer().sendMessage(Language.MSGS_PLAYER_WIN_BROADCAST1.getSentence() + winner + Language.MSGS_PLAYER_WIN_BROADCAST2.getSentence());
					}
				}
			}
			return;
		}
	}
}
