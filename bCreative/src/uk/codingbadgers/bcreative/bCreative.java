/**
 * bCreative 1.2-SNAPSHOT
 * Copyright (C) 2013  CodingBadgers <plugins@mcbadgercraft.com>
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package uk.codingbadgers.bcreative;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.logging.Level;

import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.Material;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Item;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerDropItemEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerPickupItemEvent;
import org.bukkit.event.player.PlayerTeleportEvent;

import uk.codingbadgers.bFundamentals.bFundamentals;
import uk.codingbadgers.bFundamentals.module.Module;
import uk.codingbadgers.bFundamentals.player.FundamentalPlayer;

public class bCreative extends Module implements Listener {

	/**
	 * The list of worlds that bCreative is active in
	 */
	private List<String> activeWorlds = null;
	
	/**
	 * The list of black listed interact materials
	 */
	private List<Material> interactBlacklist = null;
	
	/**
	 * A hashmap of the last item a player tried to pickup
	 */
	private HashMap<Player, Item> playersLastPickupItem = null;

	/* 
	 * Called when the module is enabled
	 */
	@Override
	@SuppressWarnings("deprecation")
	public void onEnable() {
		register(this);
		
		FileConfiguration config = getConfig();
		config.addDefault("worlds", new ArrayList<String>());
		config.addDefault("interact-blacklist", Arrays.asList("ENDER_CHEST", "CHEST"));
		config.options().copyDefaults(true);
		saveConfig();
		
		activeWorlds = new ArrayList<String>();
		List<String> configWorlds = config.getStringList("worlds");
		for (String worldName : configWorlds)
			activeWorlds.add(worldName.toLowerCase());
		
		log(Level.INFO, "bCreative is active in the following worlds:");
		for (String worldName : activeWorlds)
			log(Level.INFO, " - " + worldName);
		
		interactBlacklist = new ArrayList<Material>();
		for (String materialId : config.getStringList("interact-blacklist")) {
			try {
				int id = Integer.parseInt(materialId);
				Material interactMaterial = Material.getMaterial(id);
				if (interactMaterial != null) {
					interactBlacklist.add(interactMaterial);
				}
			} catch (NumberFormatException ex) {
				Material interactMaterial = Material.getMaterial(materialId);
				if (interactMaterial == null) {
					interactMaterial = Material.matchMaterial(materialId);
				}
				if (interactMaterial != null) {
					interactBlacklist.add(interactMaterial);
				}
			}
		}
		log(Level.INFO, "bCreative will block interaction with the following materials:");
		for (Material material : interactBlacklist)
			log(Level.INFO, " - " + material.name());
		
		playersLastPickupItem = new HashMap<Player, Item>();
	}

	/* 
	 * Called when the module is disabled
	 */
	@Override
	public void onDisable() {
		activeWorlds.clear();
		interactBlacklist.clear();
		playersLastPickupItem.clear();
	}
	
	/* 
	 * Called when a player interacts with something
	 */
	@EventHandler
	public void onPlayerInteract(PlayerInteractEvent event) {
		
		final Player player = event.getPlayer();
		
		// If the player has the interact permission, let them do what they want
		if (hasPermission(player, "bcreative.player.interact")) {
			return;
		}
		
		// We only care about creative players
		if (player.getGameMode() != GameMode.CREATIVE) {
			return;
		}

		// We only care about right click events
		if (event.getAction() != Action.RIGHT_CLICK_BLOCK) {
			return;
		}
		
		// Is the player in a world where bCreative is active?
		if (!activeWorlds.contains(player.getWorld().getName().toLowerCase())) { 
			return;
		}
		
		// Is the interacted material in our blacklist?
		final Material material = event.getClickedBlock().getType();
		if (!interactBlacklist.contains(material)) {
			return;
		}
		
		// Cancel the event as the material is on the blacklist
		event.setCancelled(true);
		sendMessage(getName(), player, "You cannot open '" + material.name() + "' whilst in creative mode.");
	}

	/* 
	 * Called when a player drops an item
	 */
	@EventHandler
	public void onPlayerItemDrop(PlayerDropItemEvent event) {
		
		final Player player = event.getPlayer();
		
		// If the player has this permission let them drop items
		if (hasPermission(player, "bcreative.player.item.drop")) {
			return;
		}
		
		// We only care about creative mode
		if (player.getGameMode() != GameMode.CREATIVE) {
			return;
		}
		
		// See if they are in a world where bCreative is active
		if (!activeWorlds.contains(player.getWorld().getName().toLowerCase())) { 
			return;
		}
		
		// Cancel the drop event
		event.setCancelled(true);
		sendMessage(getName(), player, "You cannot drop items whilst in creative mode.");
	}
	
	/* 
	 * Called when a player picks up an item
	 */
	@EventHandler
	public void onPlayerItemPickup(PlayerPickupItemEvent event) {
		
		final Player player = event.getPlayer();
		
		// If the player has this permission, let them pick things up
		if (hasPermission(player, "bcreative.player.item.pickup")) {
			return;
		}
		
		// We only care about creative mode
		if (player.getGameMode() != GameMode.CREATIVE) {
			return;
		}
		
		// Is bCreative active in this world?
		if (!activeWorlds.contains(player.getWorld().getName().toLowerCase())) { 
			return;
		}
		
		// Cancel the pickup event
		event.setCancelled(true);
		
		// Get the item and the last item the player tried to pickup
		final Item item = event.getItem();
		final Item lastKnown = playersLastPickupItem.get(player);
				
		// If the player is trying to pickup a new item tell them they can't
		if (item != lastKnown) {
			
			// If we have a last known, remove it
			if (lastKnown != null) {
				playersLastPickupItem.remove(player);
			}
			
			// Update the last known item for this player
			playersLastPickupItem.put(player, item);
			
			sendMessage(getName(), player, "You cannot pickup items whilst in creative mode.");
		}
	}

	/* 
	 * Called when a player teleports
	 */
	@EventHandler
	public void onPlayerTeleport(PlayerTeleportEvent event) {
		
		final Player player = event.getPlayer();
		
		// If the player has this permission let them teleport how they want
		if (hasPermission(player, "bcreative.player.teleport")) {
			return;
		}
		
		// If they arn't changing world, let them teleport as normal
		if (event.getTo().getWorld() == event.getFrom().getWorld()) {
			return;
		}
		
		FundamentalPlayer fundamentalPlayer = bFundamentals.Players.getPlayer(player);

		// Teleporting to creative world
		if (activeWorlds.contains(event.getTo().getWorld().getName())) {
			// Order is everything
			fundamentalPlayer.backupInventory(activeWorlds.contains(event.getFrom().getWorld()) ? event.getFrom().getWorld() : Bukkit.getWorlds().get(0), true);
			fundamentalPlayer.restoreInventory(event.getTo().getWorld());
			sendMessage(getName(), event.getPlayer(), "You're inventory has been backed up whilst you are in the creative world");
		}

		// Teleporting from creative world
		if (activeWorlds.contains(event.getFrom().getWorld().getName())) {	
			// Order is everything		
			fundamentalPlayer.backupInventory(event.getFrom().getWorld(), true);			
			fundamentalPlayer.restoreInventory(activeWorlds.contains(event.getTo().getWorld()) ? event.getTo().getWorld() : Bukkit.getWorlds().get(0));
			sendMessage(getName(), event.getPlayer(), "You're inventory has been restored now you have left the creative world");
		}
	}
}
