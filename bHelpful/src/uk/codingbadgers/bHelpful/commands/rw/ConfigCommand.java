package uk.codingbadgers.bHelpful.commands.rw;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.logging.Level;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.command.CommandException;
import org.bukkit.command.CommandSender;

import uk.codingbadgers.bFundamentals.commands.ModuleCommand;
import uk.codingbadgers.bHelpful.bHelpful;

/**
 * The Class ConfigCommand, represents a command which holds its output in a
 * config.
 * 
 * @see ModuleCommand
 */
public abstract class ConfigCommand extends ModuleCommand {

	/** If the command uses config file. */
	protected final boolean m_config;

	/** If the command is loaded. */
	private boolean m_loaded;

	/** The config file for the command. */
	protected File m_file;

	/**
	 * Instantiates a new config command.
	 * 
	 * @param label
	 *            the command label
	 * @param usage
	 *            the command usage
	 * @param config
	 *            if the command uses a config file on disk
	 */
	public ConfigCommand(String label, String usage, boolean config) {
		super(label, usage);
		this.m_config = config;
		this.m_loaded = false;
		if (config) {
			this.m_file = new File(bHelpful.MODULE.getDataFolder(), label + ".cfg");
		}
	}

	/**
	 * Instantiates a new config command, which defaults to having a config.
	 * 
	 * @param label
	 *            the command label
	 * @param usage
	 *            the command usage
	 * @see ConfigCommand#ConfigCommand(String, String, boolean)
	 */
	public ConfigCommand(String label, String usage) {
		this(label, usage, true);
	}

	/**
	 * Instantiates a new config command, which defaults to having a config and
	 * the usage being the same as the label.
	 * 
	 * @param label
	 *            the label
	 * @see ConfigCommand#ConfigCommand(String, String, boolean)
	 */
	public ConfigCommand(String label) {
		this(label, "/" + label, true);
	}

	/**
	 * Handle the command.
	 * 
	 * @param sender
	 *            the sender
	 * @param label
	 *            the label
	 * @param args
	 *            the args
	 */
	protected abstract void handleCommand(CommandSender sender, String label, String[] args);

	/**
	 * Load the config.
	 * 
	 * @throws IOException
	 *             Signals that an I/O exception has occurred.
	 */
	protected abstract void loadConfig() throws IOException;

	/**
	 * Load the command, creates the default config if the file doesn't exist
	 * and loads the config.
	 */
	public void loadCommand() {
		if (m_loaded) {
			return;
		}

		try {
			if (m_config && !m_file.exists()) {
				if (!m_file.createNewFile()) {
					// should never be called
					throw new IOException("Unkown error in creating default config file for " + m_label);
				}

				InputStream ins = getClass().getResourceAsStream("config" + File.separator + m_file.getName());

				if (ins != null) {
					BufferedReader reader = new BufferedReader(new InputStreamReader(ins));
					BufferedWriter writer = new BufferedWriter(new FileWriter(m_file));

					try {
						SimpleDateFormat format = new SimpleDateFormat("d/M/y h:m:s");
						String line = '#' + m_file.getName() + " generated on " + format.format(new Date());
						writer.write(line);
						writer.write('#');
						while ((line = reader.readLine()) != null) {
							writer.write(replaceFileMacros(line));
						}
					} finally {
						writer.close();
						reader.close();
					}
				}
			}

			loadConfig();
		} catch (IOException ex) {
			bHelpful.MODULE.getLogger().log(Level.WARNING, "Exception loading " + m_label + "'s config file (" + m_file.getName() + ")", ex);
			m_loaded = false;
			return;
		}

		m_loaded = true;
	}
	
	private String replaceFileMacros(String line) {
		line = line.replaceAll("%server%", Bukkit.getServerName());
		line = line.replaceAll("%d", String.valueOf(Calendar.getInstance().get(Calendar.DATE)));
		line = line.replaceAll("%m", String.valueOf(Calendar.getInstance().get(Calendar.MONTH + 1)));
		line = line.replaceAll("%y", String.valueOf(Calendar.getInstance().get(Calendar.YEAR)));
		line = line.replaceAll("%h", String.valueOf(Calendar.getInstance().get(Calendar.HOUR_OF_DAY)));
		line = line.replaceAll("%m", String.valueOf(Calendar.getInstance().get(Calendar.MINUTE)));
		line = line.replaceAll("%s", String.valueOf(Calendar.getInstance().get(Calendar.SECOND)));
		return line;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * uk.codingbadgers.bFundamentals.commands.ModuleCommand#onCommand(org.bukkit
	 * .command.CommandSender, java.lang.String, java.lang.String[])
	 */
	public boolean onCommand(CommandSender sender, String label, String[] args) {
		if (!m_loaded) {
			throw new CommandException("Command is not loaded");
		}

		handleCommand(sender, label, args);
		return true;
	}

	/**
	 * Replace minecraft colours in a string.
	 * 
	 * @param message
	 *            the message
	 * @return the string with colours replaced
	 */
	protected String replaceColors(String message) {
		while (message.indexOf("&") != -1) {
			String code = message.substring(message.indexOf("&") + 1, message.indexOf("&") + 2);
			message = message.substring(0, message.indexOf("&")) + ChatColor.getByChar(code) + message.substring(message.indexOf("&") + 2);
		}
		return message;
	}
}
