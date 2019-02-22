package com.acropolismc.play.sellstick;

import com.acropolismc.play.sellstick.Configs.PriceConfig;
import com.acropolismc.play.sellstick.Configs.StickConfig;

import com.palmergames.bukkit.towny.exceptions.NotRegisteredException;
import com.palmergames.bukkit.towny.object.Resident;
import com.palmergames.bukkit.towny.object.Town;
import com.palmergames.bukkit.towny.object.TownyUniverse;
import com.palmergames.bukkit.towny.object.WorldCoord;

import net.coreprotect.CoreProtectAPI;
import net.milkbowl.vault.economy.EconomyResponse;

import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.permissions.PermissionAttachmentInfo;

import java.util.ArrayList;
import java.util.List;

public class PlayerListener implements Listener {
	private SellStick plugin;

	public PlayerListener(SellStick plugin) {
		this.plugin = plugin;
	}

	public static boolean isNumeric(String str) {
		try {
			@SuppressWarnings("unused")
			double d = Double.parseDouble(str);
		} catch (NumberFormatException nfe) {
			return false;
		}
		return true;
	}

	public boolean isInfinite(List<String> lores) {
		// If the 2nd line is the same as the config, its infinite
		if (lores.get(StickConfig.instance.durabilityLine - 1).equalsIgnoreCase(StickConfig.instance.infiniteLore))
			return true;
		return false;
	}

	/**
	 * This method was created incase someone wants to put "%remaining uses out of
	 * 50% Where the last int is NOT the remaining uses.
	 * 
	 * There's probably a more efficient way to do this but I haven't gotten around
	 * to recoding it and it hasn't given an issue yet.
	 * 
	 * @param lores Takes a string list
	 * @return finds the uses in the lores and returns as int.
	 */
	public int getUsesFromLore(List<String> lores) {
		String found = "";
		// Get all the lores. lore[1] contains the uses/infinite
		// info.
		// We need to get the uses.
		// The reason for all of this is incase someone puts
		// %remaining% uses out of 50, in the config. We need to
		// be able to
		// get the first number(or the lower number in most
		// cases)
		// because an annoying person will put
		// out of 50 you have %remaining% uses left.
		// Again, the only purpose of this is to
		// Get the # of uses if theres multiple numbers
		// in the lore
		// We loop through the String(lore at index 1) and check all the indexes
		// TODO: Make this readable and more efficient.
		for (int i = 0; i < lores.get(StickConfig.instance.durabilityLine - 1).length(); i++) {
			if (Character.isDigit(lores.get(StickConfig.instance.durabilityLine - 1).charAt(i))) {
				// Increment "found" string
				// Make sure it wasnt a number from a color code

				// If it ISNT the first index
				if (i != 0) {
					// Check to see if the index before is the & sign (If its a color code)
					if (lores.get(StickConfig.instance.durabilityLine - 1).charAt(i - 1) != ChatColor.COLOR_CHAR) {
						// And if it isnt, keep track of it
						found += lores.get(StickConfig.instance.durabilityLine - 1).charAt(i);
					} else {
						// If it IS a color code, simply ignore it
						found += "-";
					}
					// But if it's index == 0
				} else {
					// There can't be a & before it, so keep track of it
					found += lores.get(StickConfig.instance.durabilityLine - 1).charAt(i);
				}
			} else {
				// Otherwise we insert a "-"
				found += "-";
			}
		}
		// We now take that found string, and split it at every "-"
		String[] split = found.split("-");

		List<Integer> hold = new ArrayList<Integer>();

		// We take the split array, and loop thru it
		for (int i = 0; i < split.length; i++) {
			// If we find a number in it,
			if (isNumeric(split[i])) {
				// We hold onto that number
				hold.add(Integer.parseInt(split[i]));
			}
		}
		// Now we just do a quick loop through the hold array and find the
		// lowest number.

		int min = -2;
		try {
			min = hold.get(0);
		} catch (Exception ex) {
			System.out.println(StickConfig.instance.durabilityLine);
			System.out.println("The problem seems to be that your sellstick useline number has changed.");
			System.out.println(ex);
		}

		for (int i = 0; i < hold.size(); i++) {
			if (hold.get(i) < min) {
				min = hold.get(i);
			}
		}
		return min;
	}

	@SuppressWarnings("deprecation")
	@EventHandler(ignoreCancelled = true, priority = EventPriority.HIGH)
	public void onUse(PlayerInteractEvent e) {
		Player p = e.getPlayer();
		// Gets item from Config.
		Material sellItem = Material.getMaterial(StickConfig.instance.item.toUpperCase());
		// When they left click with that item, and that item has the same name
		// as a sellstick
		if (e.getAction() == Action.LEFT_CLICK_BLOCK) {

			// Leaving this depricated for some backwards compatibility. Anything after
			// 1.9 should have p.getInventory().getItemInMainHand()
			if (p.getItemInHand().getType() == sellItem) {
				if (p.getItemInHand().getItemMeta().getDisplayName() != null
						&& p.getItemInHand().getItemMeta().getDisplayName().startsWith(StickConfig.instance.name)) {
					if (e.getClickedBlock().getType() == Material.CHEST
							|| e.getClickedBlock().getType() == Material.TRAPPED_CHEST) {

						// Didn't have permission :(
						if (!p.hasPermission("sellstick.use")) {
							plugin.msg(p, StickConfig.instance.noPerm);
							return;
						}

						Location location = e.getClickedBlock().getLocation();
						try {
							
							if(WorldCoord.parseWorldCoord(location).getTownBlock().hasTown()) {
								Town town = WorldCoord.parseWorldCoord(location).getTownBlock().getTown();
								Resident chestUser = TownyUniverse.getDataSource().getResident(p.getName());
								
								if(!chestUser.hasTown() || !town.getResidents().contains(chestUser) || !chestUser.getTown().equals(town)) {
									plugin.msg(p, StickConfig.instance.territoryMessage.replace("%claims%", town.getName()));
									e.setCancelled(true);
									return;
								}
							}


						} catch (NotRegisteredException e1) {
							//System.out.println("No town here");;
						}

						ItemStack is = p.getItemInHand();
						ItemMeta im = is.getItemMeta();

						// So here we go
						List<String> lores = im.getLore();
						int uses = -1;
						if (!isInfinite(lores)) {
							uses = getUsesFromLore(lores);
						}
						if (uses == -2) {
							// This should honestly never happen unless someone changes sellstick lores
							plugin.msg(p, ChatColor.RED + "There was an error!");
							plugin.msg(p, ChatColor.RED
									+ "Please let an admin know to check console, or, send them these messages:");

							plugin.msg(p, ChatColor.RED
									+ "Player has a sellstick that has had its 'DurabilityLine' changed in the config");
							plugin.msg(p, ChatColor.RED
									+ "For this reason, the plugin could not find the line number on which the finite/infinite lore exists");
							plugin.msg(p, ChatColor.RED + "This can be resolved by either:");
							plugin.msg(p, ChatColor.RED + "1: Giving the player a new sellstick");
							plugin.msg(p, ChatColor.RED + "(Includes anyone on the server that has this issue)");
							plugin.msg(p, ChatColor.RED + "or");
							plugin.msg(p, ChatColor.RED
									+ "2: Changing the DurabilityLine to match the one that is on this sellstick");

							plugin.msg(p, ChatColor.RED + "For help, contact shmkane on spigot or github");

							return;
						}

						InventoryHolder c = (InventoryHolder) e.getClickedBlock().getState();
						ItemStack[] contents = (ItemStack[]) c.getInventory().getContents();
						double total = 0;
						double slotPrice = 0;
						double price = 0;
						double multiplier = Double.NEGATIVE_INFINITY;


						StringBuilder logItemsMsg = StickConfig.instance.logItems ? new StringBuilder(500) : null;

						for (int i = 0; i < c.getInventory().getSize(); i++) {
							if (contents[i] == null) continue;

							Material blockType = contents[i].getType();
							if (blockType == null || blockType == Material.AIR) {
								continue;
							}
							try {
								if (!StickConfig.instance.useEssentialsWorth || plugin.ess == null
										|| !plugin.ess.isEnabled()) {

									//plugin.msg(p, contents[i].getType().name());

									// Extremely ineffecient, unnecessary loop through map keys.
									for (String itemName : PriceConfig.instance.getConfig().getConfigurationSection("prices")
											.getKeys(false)) {

										if ((contents[i].getType().name().equalsIgnoreCase(itemName) || (isNumeric(itemName)
												&& contents[i].getType().getId() == Integer.parseInt(itemName)))
												&& contents[i].getDurability() == contents[i].getType().getMaxDurability()) {
											price = Double.parseDouble(
													PriceConfig.instance.getConfig().getString("prices." + itemName));

										}
									}
								} else {
									// Essentials Worth
									price = plugin.ess.getWorth().getPrice(plugin.ess, contents[i]).doubleValue();
								}

								int amount = (int) contents[i].getAmount();

								slotPrice = price * amount;
								if (slotPrice > 0) {
									ItemStack sell = contents[i];
									// Log as container withdraw
									if (StickConfig.instance.useCoreProtect) {
										if(plugin.coreProtect == null || !plugin.coreProtect.isEnabled()) {
											System.out.println(("SellStick failed to hook CoreProtect. There will be no CoreProtect log of items removed from chest."));
										}
										else {
											//System.out.println("SellStick logging container transaction via CoreProtect.");
											plugin.coreProtect.getAPI().logContainerTransaction(p.getName(), location);
										}
									}
									if (logItemsMsg != null) {
										logItemsMsg.append(String.format("%sx%d(+%.2f), ", sell.getType().name(), sell.getAmount(), slotPrice));
									}
									c.getInventory().remove(sell);
									e.getClickedBlock().getState().update();
								}

							} catch (NullPointerException ex) {
								ex.printStackTrace();
							}

							total += slotPrice;
							slotPrice = 0;
							price = 0;
						}

						if (total > 0) {
							if (!isInfinite(lores)) {

								lores.set(StickConfig.instance.durabilityLine - 1,
										lores.get(StickConfig.instance.durabilityLine - 1).replaceAll(uses + "",
												(uses - 1) + ""));
								im.setLore(lores);
								is.setItemMeta(im);
							}

							/*
							 * Permissions based multiplier check. If user doesn't have
							 * sellstick.multiplier.x permission Multiplier defaults to 1 as seen below.
							 */
							for (PermissionAttachmentInfo perm : p.getEffectivePermissions()) {
								if (perm.getPermission().startsWith("sellstick.multiplier")) {
									String stringPerm = perm.getPermission();
									String permSection = stringPerm.replaceAll("sellstick.multiplier.", "");
									if (Double.parseDouble(permSection) > multiplier) {
										multiplier = Double.parseDouble(permSection);
									}
								}
							}

							/*
							 * Multiplier set to Double.NEGATIVE_INFINITY by default to signal "unchanged"
							 * Problem with defaulting to 0 is total*0 = 0, Problem with defaulting to 1 is
							 * multipliers < 1.
							 */
							EconomyResponse r;

							if (multiplier == Double.NEGATIVE_INFINITY) {
								r = plugin.getEcon().depositPlayer(p, total);
							} else {
								r = plugin.getEcon().depositPlayer(p, total * multiplier);
							}

							if (r.transactionSuccess()) {
								if (StickConfig.instance.sellMessage.contains("\\n")) {
									String[] send = StickConfig.instance.sellMessage.split("\\\\n");
									for (String msg : send) {
										plugin.msg(p, msg.replace("%balance%", plugin.getEcon().format(r.balance))
												.replace("%price%", plugin.getEcon().format(r.amount)));
									}
								} else {
									plugin.msg(p,
											StickConfig.instance.sellMessage
													.replace("%balance%", plugin.getEcon().format(r.balance))
													.replace("%price%", plugin.getEcon().format(r.amount)));
								}
								// TODO This can be specified as format in config.
								if(logItemsMsg != null) {
									System.out.println(String.format("[%s] %s(%s) sold items\n	from chest at %s %.2fx, %.2fy, %.2fz:\n%s\n	for %.2f and now has %.2f",
											plugin.getDescription().getName(), p.getName(), p.getUniqueId(),
											location.getWorld().getName(), location.getX(), location.getY(), location.getZ(),
											logItemsMsg.toString(),
											r.amount, r.balance));
								}
								else {
									System.out.println("[SellStick]" + p.getName() + " sold items for " + r.amount
											+ " and now has " + r.balance);
								}
							} else {
								plugin.msg(p, String.format("An error occured: %s", r.errorMessage));
							}

							if (uses - 1 == 0) {
								p.getInventory().remove(p.getItemInHand());
								p.updateInventory();
								plugin.msg(p, StickConfig.instance.brokenStick);
							}

						} else {
							plugin.msg(p, StickConfig.instance.nothingWorth);
						}
						e.setCancelled(true);
					}
				}
			}
		}
	}
}
