package uk.codingbadgers.bFundamentals.module;

import java.io.BufferedReader;
import java.io.DataInputStream;
import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.io.FileInputStream;
import java.io.InputStreamReader;
import java.util.HashMap;
import java.util.logging.Level;

import n3wton.me.BukkitDatabaseManager.Database.BukkitDatabase;
import net.milkbowl.vault.permission.Permission;

import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.Listener;

import com.nodinchan.ncbukkit.loader.Loadable;

import uk.codingbadgers.bFundamentals.bFundamentals;
import uk.codingbadgers.bFundamentals.commands.ModuleCommand;

/**
 * The base Module class.
 */
public abstract class Module extends Loadable implements Listener {

	/** The base plugin. */
	protected final bFundamentals m_plugin;
	
	/** The config. */
	protected FileConfiguration m_config;
	
	/** The config file. */
	protected File m_configFile = null;
	
	/** The version of the module. */
	private String m_version = null;
	
	/** The name of the module. */
	private String m_name = null;
	
	/** The language map. */
	private HashMap<String, String> m_languageMap = new HashMap<String, String>();
	
	/** The commands registered to this module. */
	protected List<ModuleCommand> m_commands = new ArrayList<ModuleCommand>();

	/** All the listeners registered to this module */
	private List<Listener> m_listeners = new ArrayList<Listener>();
	
	/** The database registered to the modules. */
	protected static BukkitDatabase m_database = null;
	
	/** The Permissions instance. */
	private static Permission m_permissions = null;
	
	/**
	 * Instantiates a new module.
	 *
	 * @param name the name of the module
	 * @param version the version of the module
	 */
	public Module(String name, String version) {
		super(name);
		m_version = version;
		m_plugin = bFundamentals.getInstance();
		m_database = bFundamentals.getBukkitDatabase();
		m_permissions = bFundamentals.getPermissions();
		m_name = name;
	}
	
	/**
	 * Load language file.
	 */
	protected void loadLanguageFile() {
		File languageFile = new File(getDataFolder() + File.separator + m_name + "_" + m_plugin.getConfigurationManager().getLanguage() + ".lang");
		
		if (!languageFile.exists()) {
			log(Level.SEVERE, "Missing language file '" + languageFile.getAbsolutePath() + "'!");
			return;
		}
		
		log(Level.INFO, "Loading Language File: " + languageFile.getName());
		
		try {
			FileInputStream fstream = new FileInputStream(languageFile);
			DataInputStream in = new DataInputStream(fstream);
			BufferedReader br = new BufferedReader(new InputStreamReader(in));
			
			String line = null;
			String key = null;
			while ((line = br.readLine()) != null)   {
				
				if (line.isEmpty())
					continue;
				
				if (line.startsWith("#")) {
					key = line.substring(1);
					continue;
				}
				
				if (key == null) {
					log(Level.WARNING, "Trying to parse a language value, with no key set!");
					continue;
				}

				m_languageMap.put(key, line);				
			}
			
			br.close();
			
		} catch (Exception e) {
			e.printStackTrace();
		}
	
	}
	
	/**
	 * Log to console.
	 *
	 * @param level the Log level
	 * @param string the message
	 */
	public void log(Level level, String string) {
		bFundamentals.log(level, "[" + super.getName() + "] " + string);
	}
	
	/**
	 * Register a event listener.
	 *
	 * @param listener the bukkit event listener
	 */
	public final void register(Listener listener) {
		m_plugin.getServer().getPluginManager().registerEvents(listener, m_plugin);
		m_listeners.add(listener);
	}
	
	/**
	 * Gets the permissions.
	 *
	 * @return the permissions
	 */
	public Permission getPermissions() {
		return m_permissions;
	}
	
	/**
	 * On enable.
	 */
	public abstract void onEnable();
	
	/**
	 * On disable.
	 */
	public abstract void onDisable();
	
	/**
	 * On command (Bukkit command system).
	 *
	 * @param sender the sender
	 * @param cmd the cmd
	 * @param label the label
	 * @param args the args
	 * @return true, if successful
	 */
	@Deprecated
	public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args){
		return false;
	}
	
	/**
	 * On command (Our command system).
	 *
	 * @param sender the sender
	 * @param label the label
	 * @param args the args
	 * @return true, if successful
	 */
	public boolean onCommand(Player sender, String label, String[] args){
		return false;
	}
	
	/**
	 * Gets the version.
	 *
	 * @return the version
	 */
	public String getVersion() {
		return m_version;
	}
	
	/**
	 * Checks for permission.
	 *
	 * @param player the player
	 * @param node the node
	 * @return true, if successful
	 */
	public static boolean hasPermission(final Player player, final String node) {
		if (m_permissions.has(player, node)) {
			return true;
		}
		return false;
	}
	
	/**
	 * Send message to a player.
	 *
	 * @param name the name of the module
	 * @param player the player to send to
	 * @param message the message
	 */
	public static void sendMessage(String name, Player player, String message) {
		player.sendMessage(ChatColor.DARK_PURPLE + "[" + name + "] " + ChatColor.RESET + message);
	}
	
	/**
	 * Checks if is command registered.
	 *
	 * @param command the command
	 * @return true, if is command registered
	 */
	public boolean isCommandRegistered(String command) {
		for (ModuleCommand cmd : m_commands) {
			if (cmd.equals(command))
				return true;
		}
		return false;
	}

	/**
	 * Gets the language value.
	 *
	 * @param key the key
	 * @return the language value
	 */
	public String getLanguageValue(String key) {
		return m_languageMap.get(key);
	}
	
	/**
	 * Register command.
	 *
	 * @param command the command
	 */
	protected void registerCommand(ModuleCommand command) {
		m_commands.add(command);
	}

	/**
	 * Get all the listeners registered to this module, for cleaning up on disable
	 * 
	 * @return a list of all listeners
	 */
	public List<Listener> getListeners() {
		return m_listeners;
	}

}