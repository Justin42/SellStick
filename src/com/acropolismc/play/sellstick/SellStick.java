package com.acropolismc.play.sellstick;

import java.util.logging.Logger;

import net.coreprotect.CoreProtect;
import net.coreprotect.CoreProtectAPI;
import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.RegisteredServiceProvider;
import org.bukkit.plugin.java.JavaPlugin;

import com.acropolismc.play.sellstick.Configs.PriceConfig;
import com.acropolismc.play.sellstick.Configs.StickConfig;
import com.earth2me.essentials.Essentials;

import net.milkbowl.vault.economy.Economy;

// @author shmkane
public class SellStick extends JavaPlugin {

	public Essentials ess;
	public CoreProtect coreProtect;
	private static Economy econ = null;
	private static final Logger log = Logger.getLogger("Minecraft");
	Plugin towny;

	public void onEnable() {
		this.saveDefaultConfig();

		StickConfig.instance.setup(getDataFolder());
		PriceConfig.instance.setup(getDataFolder());

		if (!setupTowny()) {
			log.severe(String.format("[%s] - Disabled due to no Towny found!", getDescription().getName()));
			getServer().getPluginManager().disablePlugin(this);
			return;
		}

		if (!setupEconomy()) {
			log.severe(String.format("[%s] - Disabled due to no Vault dependency found!", getDescription().getName()));
			getServer().getPluginManager().disablePlugin(this);
			return;
		}

		setupEssentials();
		setupCoreProtect();

		this.getCommand("sellstick").setExecutor(new SellStickCommand(this));
		this.getServer().getPluginManager().registerEvents(new PlayerListener(this), this);
	}

	public void onDisable() {
		log.warning(String.format("[%s] - Attempting to disabling...", getDescription().getName()));
		try {
			ess = null;
			econ = null;
			towny = null;
		} catch (Exception ex) {
			log.severe(String.format("[%s] - Was not disabled correctly!", getDescription().getName()));
		} finally {
			log.warning(String.format("[%s] - Attempt complete!", getDescription().getName()));
		}
	}

	public void setupEssentials() {
		if (Bukkit.getPluginManager().isPluginEnabled("Essentials")) {
			log.info(String.format("[%s] Hooked into essentials!", getDescription().getName()));
			ess = (Essentials) Bukkit.getPluginManager().getPlugin("Essentials");
		}

		if (StickConfig.instance.useEssentialsWorth) {
			if (ess == null || !ess.isEnabled()) {
				log.warning(String.format("[%s] Trying to use essentials worth but essentials not found!",
						getDescription().getName()));
			} else {
				log.info(String.format("[%s] Using essentials worth!", getDescription().getName()));
			}
		}

	}

	public void setupCoreProtect() {
		if (Bukkit.getPluginManager().isPluginEnabled("CoreProtect")) {
			if(!StickConfig.instance.useCoreProtect) {
				log.info(String.format("[%s] CoreProtect plugin found, but support disabled in configuration.", getDescription().getName()));
				log.info(String.format("[%s] Consider setting 'UseCoreProtect: true' to enable CoreProtect support.", getDescription().getName()));
				log.info(String.format("[%s] Items being removed from containers will not be logged!", getDescription().getName()));
				return;
			}
			log.info(String.format("[%s] Hooked into CoreProtect!", getDescription().getName()));
			coreProtect = (CoreProtect) Bukkit.getPluginManager().getPlugin("CoreProtect");
		}
	}

	public boolean setupTowny() {
		towny = getServer().getPluginManager().getPlugin("Towny");
		if (towny != null && towny.isEnabled())
			log.info(String.format("[%s] Hooked into Towny!", getDescription().getName()));

		return towny != null && towny.isEnabled();
	}

	private boolean setupEconomy() {
		if (getServer().getPluginManager().getPlugin("Vault") == null) {
			return false;
		}
		RegisteredServiceProvider<Economy> rsp = getServer().getServicesManager().getRegistration(Economy.class);
		if (rsp == null) {
			return false;
		}
		econ = rsp.getProvider();
		return econ != null;
	}

	public Economy getEcon() {
		// Return instance of economy.
		return SellStick.econ;
	}

	/**
	 * This will send a player a message. If message is empty, it wont send
	 * anything.
	 * 
	 * @param sender The target player
	 * @param msg    the message
	 */
	public void msg(CommandSender sender, String msg) {
		if (msg.length() == 0) {
			return;
		}

		sender.sendMessage(StickConfig.instance.prefix + msg);
	}
}
