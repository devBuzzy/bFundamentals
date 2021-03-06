/**
 * bInfoBooks 1.2-SNAPSHOT
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
package uk.codingbadgers.binfobooks;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.HashMap;
import java.util.logging.Level;

import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.Event.Result;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryAction;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryType;
import org.bukkit.event.player.PlayerDropItemEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;
import org.bukkit.inventory.meta.BookMeta;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.JSONValue;

import uk.codingbadgers.bFundamentals.module.Module;
import uk.codingbadgers.binfobooks.commands.CommandBook;

public class bInfoBooks extends Module implements Listener {
	
	private HashMap<String, InfoBook> m_books = new HashMap<String, InfoBook>();

	/**
	 * Called when the module is disabled.
	 */
	public void onDisable() {
	}

	/**
	 * Called when the module is loaded.
	 */
	public void onEnable() {
		loadBooks();
		
		register(this);
		registerCommand(new CommandBook(this));
	}
	
	@EventHandler (priority=EventPriority.NORMAL)
	public void onInventoryClick(InventoryClickEvent event) {
		
		if (!isItemInfoBook(event.getCursor())) {
			return;
		}
			
		if (!isPlace(event.getAction())) {
			return;
		}
		
		if (isInventory(event.getInventory())) {
			return;
		}
		
		if ((event.getRawSlot() - event.getView().getTopInventory().getSize()) >= 0) {
			return;
		}
		
		Module.sendMessage("bInfoBooks", (Player)event.getWhoClicked(), "You can't store InfoBooks. Please drop the InfoBook to remove it from your inventory.");
		Module.sendMessage("bInfoBooks", (Player)event.getWhoClicked(), "You can get another copy of the book via the '/book' command.");
		event.setResult(Result.DENY);
	}
	
	private boolean isInventory(Inventory topInventory) {
		return topInventory instanceof PlayerInventory || 
				topInventory.getType() == InventoryType.PLAYER || 
				topInventory.getType() == InventoryType.CREATIVE;
	}

	private boolean isPlace(InventoryAction action) {
		return action == InventoryAction.DROP_ALL_CURSOR || 
				action == InventoryAction.DROP_ALL_SLOT ||
				action == InventoryAction.DROP_ONE_CURSOR ||
				action == InventoryAction.DROP_ONE_SLOT ||
				action == InventoryAction.PLACE_ALL ||
				action == InventoryAction.PLACE_ONE ||
				action == InventoryAction.PLACE_SOME ||
				action == InventoryAction.SWAP_WITH_CURSOR;
	}

	@EventHandler (priority=EventPriority.NORMAL)
    public void onPlayerDropItem(PlayerDropItemEvent event) {
		
		ItemStack item = event.getItemDrop().getItemStack();
		if (!isItemInfoBook(item)) {
			return;
		}

		event.getItemDrop().remove();
	}
	
	private boolean isItemInfoBook(ItemStack item) {
		
		if (item == null) {
			return false;
		}
		
		if (item.getType() != Material.WRITTEN_BOOK) {
			return false;
		}
		
		final BookMeta bookmeta = (BookMeta)item.getItemMeta();
		final String bookName = bookmeta.getDisplayName();
		for (InfoBook book : m_books.values()) {
			if (bookName.equalsIgnoreCase(book.getName())) {
				return true;
			}
		}
		
		return false;
		
	}
	
	private void loadBooks() {
		
		m_books.clear();
		
		File bookFolder = new File(this.getDataFolder() + File.separator + "books");
		if (!bookFolder.exists()) {
			// make the 'books' folder
			bookFolder.mkdirs();
			
			// make a default book
			makeExampleBook(bookFolder);
		}
		
		for (File file : bookFolder.listFiles()) {
			final String fileName = file.getName();
			if (fileName.substring(fileName.lastIndexOf(".")).contains("json")) {
				
				this.log(Level.INFO, "Loading Book: " + fileName);
				
				String jsonContents = "";
				try {
					BufferedReader in = new BufferedReader(new InputStreamReader(new FileInputStream(file)));
				
					String inputLine;
		            while ((inputLine = in.readLine()) != null) {
		                jsonContents += inputLine;
		            }
		            
		            in.close();
				
				} catch (FileNotFoundException e) {
					e.printStackTrace();
				} catch (IOException e) {
					e.printStackTrace();
				}
				
				JSONObject bookJSON = (JSONObject)JSONValue.parse(jsonContents);
				InfoBook newBook = new InfoBook(bookJSON);
				m_books.put(newBook.getName().toLowerCase(), newBook);
			}
		}
		
	}

	@SuppressWarnings("unchecked")
	private void makeExampleBook(File booksfolder) {
		
		JSONObject book = new JSONObject();
		
		book.put("name", "Example Book");
		book.put("author", "McBadgerCraft");
		
		JSONArray pages = new JSONArray();
		pages.add("This is an example book");
		pages.add("Using bInfoBooks created by");
		pages.add("TheCodingBadgers");
		book.put("pages", pages);
		
		JSONArray taglines = new JSONArray();
		taglines.add("bInfoBooks");
		taglines.add("By TheCodingBadgers");
		book.put("taglines", taglines);
		
		String contents = book.toJSONString();
		try {
			 BufferedWriter out = new BufferedWriter(new FileWriter(booksfolder.getAbsolutePath() + File.separator + "examplebook.json"));
			 out.write(contents);
			 out.close();		
		} catch (Exception ex) {
			ex.printStackTrace();
		}
	}

	public InfoBook bookExists(String bookName) {
		if (m_books.containsKey(bookName.toLowerCase()))
			return m_books.get(bookName.toLowerCase());
		
		for (InfoBook book : m_books.values())
		{
			final String currentBook = book.getName().toLowerCase();
			if (currentBook.startsWith(bookName.toLowerCase())) {
				return book;
			}
			if (currentBook.equalsIgnoreCase(bookName.toLowerCase())) {
				return book;
			}
		}
		
		return null;
	}
	
	public void listBooks(Player player) {
		for (InfoBook book : m_books.values()) {
			Module.sendMessage("bInfoBooks", player, " - " + book.getName() + " by " + book.getAuthor());
		}
	}
	
	public boolean playerHasBook(Player player, InfoBook infobook) {
		
		final PlayerInventory invent = player.getInventory();
		
		if (invent.contains(Material.WRITTEN_BOOK)) {
			for (ItemStack item : invent.getContents()) {
				if (item == null) {
					continue;
				}
				
				if (item.getType() == Material.WRITTEN_BOOK) {
					final BookMeta bookmeta = (BookMeta)item.getItemMeta();

					if (!infobook.getAuthor().equalsIgnoreCase(bookmeta.getAuthor())) {
						continue;
					}
					
					if (!infobook.getName().equalsIgnoreCase(bookmeta.getTitle())) {
						continue;
					}
					
					// The player already has a copy of the given book
					return true;
				}
			}
		}
		
		return false;
	}

	public boolean givePlayerBook(Player player, InfoBook infobook) {
		
		ItemStack book = new ItemStack(Material.WRITTEN_BOOK, 1);
		BookMeta bookmeta = (BookMeta)book.getItemMeta();
		
		// fill in book info meta
		bookmeta.setAuthor(infobook.getAuthor());
		bookmeta.setTitle(infobook.getName());
		bookmeta.setLore(infobook.getTagLines());
		bookmeta.setPages(infobook.getPages(player));		
		bookmeta.setDisplayName(infobook.getName());
		
		book.setItemMeta(bookmeta);
		player.getInventory().addItem(book);
		return true;
	}
}
