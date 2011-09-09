/* This program is free software: you can redistribute it and/or modify
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
package info.somethingodd.bukkit.OddItem;

import com.nijiko.permissions.PermissionHandler;
import com.nijikokun.bukkit.Permissions.Permissions;
import info.somethingodd.bukkit.OddItem.bktree.BKTree;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.PluginDescriptionFile;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.util.config.Configuration;
import org.bukkit.util.config.ConfigurationNode;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URL;
import java.net.URLConnection;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.SortedSet;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.ConcurrentNavigableMap;
import java.util.concurrent.ConcurrentSkipListMap;
import java.util.concurrent.ConcurrentSkipListSet;
import java.util.logging.Logger;

/**
 * @author Gordon Pettey (petteyg359@gmail.com)
 */
public class OddItem extends JavaPlugin {
    protected static Logger log = null;
	protected static Configuration configuration = null;
    protected static ConcurrentNavigableMap<String, SortedSet<String>> items = null;
    protected static ConcurrentHashMap<String, OddItemGroup> groups = null;
    protected static ConcurrentMap<String, ItemStack> itemMap = null;
    protected static String logPrefix = null;
    private static PermissionHandler ph = null;
    private static BKTree<String> bktree = null;
    private static String configurationFile = null;
	private static PluginDescriptionFile info;

    /**
     * Compares two ItemStack material and durability, ignoring quantity
     * @param a ItemStack to compare
     * @param b ItemStack to compare
     * @return ItemStack are equal
     */
    public static Boolean compare(ItemStack a, ItemStack b) {
        return compare(a, b, true, false);
    }

    /**
     * Compares two ItemStack material, durability, and quantity
     * @param a ItemStack to compare
     * @param b ItemStack to compare
     * @param quantity whether to compare quantity
     * @return ItemStack are equal
     */
    public static Boolean compare(ItemStack a, ItemStack b, Boolean quantity) {
        return compare(a, b, true, quantity);
    }

    /**
     * Compares two ItemStack
     * @param a ItemStack to compare
     * @param b ItemStack to compare
     * @param quantity whether to compare quantity
     * @param durability whether to compare durability
     * @return ItemStack are equal
     */
    public static Boolean compare(ItemStack a, ItemStack b, Boolean durability, Boolean quantity) {
        Boolean ret = true;
        if (durability) ret &= (a.getDurability() == b.getDurability());
        if (ret && quantity) ret &= (a.getAmount() == b.getAmount());
        return ret;
    }

    protected static void configure() {
        itemMap = new ConcurrentHashMap<String, ItemStack>();
        items = new ConcurrentSkipListMap<String, SortedSet<String>>(String.CASE_INSENSITIVE_ORDER);
        groups = new ConcurrentHashMap<String, OddItemGroup>();
        File configFile = new File(configurationFile);
        if (!configFile.exists())
            if (!writeConfig()) return;
        configuration = new Configuration(configFile);
        configuration.load();
        String comparator = configuration.getString("comparator");
        if (comparator != null) {
            if (comparator.equalsIgnoreCase("c") || comparator.equalsIgnoreCase("caverphone")) {
                bktree = new BKTree<String>("c");
                log.info(logPrefix + "Using Caverphone for suggestions.");
            } else if (comparator.equalsIgnoreCase("k") || comparator.equalsIgnoreCase("cologne")) {
                bktree = new BKTree<String>("k");
                log.info(logPrefix + "Using ColognePhonetic for suggestions.");
            } else if (comparator.equalsIgnoreCase("m") || comparator.equalsIgnoreCase("metaphone")) {
                bktree = new BKTree<String>("m");
                log.info(logPrefix + "Using Metaphone for suggestions.");
            } else if (comparator.equalsIgnoreCase("s") || comparator.equalsIgnoreCase("soundex")) {
                bktree = new BKTree<String>("s");
                log.info(logPrefix + "Using SoundEx for suggestions.");
            } else if (comparator.equalsIgnoreCase("r") || comparator.equalsIgnoreCase("refinedsoundex")) {
                bktree = new BKTree<String>("r");
                log.info(logPrefix + "Using RefinedSoundEx for suggestions.");
            } else {
                bktree = new BKTree<String>("l");
                log.info(logPrefix + "Using Levenshtein for suggestions.");
            }
        } else {
            bktree = new BKTree<String>("l");
            log.info(logPrefix + "Using Levenshtein for suggestions.");
        }
        ConfigurationNode itemsNode = configuration.getNode("items");
        for(String i : itemsNode.getKeys()) {
            if (items.get(i) == null)
                items.put(i, new ConcurrentSkipListSet<String>(String.CASE_INSENSITIVE_ORDER));
            ArrayList<String> j = new ArrayList<String>();
            j.addAll(configuration.getStringList("items." + i, new ArrayList<String>()));
            j.add(i);
            // Add all aliases
            items.get(i).addAll(j);
            Integer id;
            Short d = 0;
            Material m;
            if (i.contains(";")) {
                try {
                    d = Short.parseShort(i.substring(i.indexOf(";") + 1));
                    id = Integer.parseInt(i.substring(0, i.indexOf(";")));
                    m = Material.getMaterial(id);
                } catch (NumberFormatException e) {
                    m = Material.getMaterial(i.substring(0, i.indexOf(";")));
                }
            } else {
                try {
                    id = Integer.decode(i);
                    m = Material.getMaterial(id);
                } catch (NumberFormatException e) {
                    m = Material.getMaterial(i);
                }
            }
            if (m == null) {
                log.warning(logPrefix + "Invalid format: " + i);
                continue;
            }
            for (String item : j) {
                itemMap.put(item, new ItemStack(m, 1, d));
                bktree.add(item);
            }
        }
        ConfigurationNode groupsNode = configuration.getNode("groups");
        if (groups != null) {
            for (String g : groupsNode.getKeys()) {
                List<String> i = new ArrayList<String>();
                if (groupsNode.getKeys(g) == null) {
                    i.addAll(groupsNode.getStringList(g, new ArrayList<String>()));
                    groupsNode.removeProperty(g);
                    groupsNode.setProperty(g+".items", i);
                    groupsNode.setProperty(g+".data", "null");
                } else {
                    i.addAll(groupsNode.getStringList(g+".items", new ArrayList<String>()));
                }
                List<ItemStack> itemStackList = new ArrayList<ItemStack>();
                for (String is : i) {
                    ItemStack itemStack;
                    Integer q = null;
                    try {
                        if (is.contains(",")) {
                            q = Integer.valueOf(is.substring(is.indexOf(",")+1));
                            is = is.substring(0, is.indexOf(","));
                            itemStack = getItemStack(is, q);
                        } else {
                            itemStack = getItemStack(is);
                        }
                        log.info(logPrefix + "Adding " + is + (q != null ? " x" + q : "") + " to group \"" + g + "\"");
                        if (itemStack != null) itemStackList.add(itemStack);
                    } catch (IllegalArgumentException e) {
                        log.warning(logPrefix + "Invalid item \"" + is + "\" in group \"" + g + "\"");
                        groups.remove(g);
                    } catch (NullPointerException e) {
                        log.warning(logPrefix + "NPE adding ItemStack \"" + is + "\" to group " + g);
                    }
                    groups.put(g, new OddItemGroup(itemStackList, groupsNode.getNode(g + ".data")));
                }
                if (groups.get(g) != null) log.info(logPrefix + "Group " + g + " added.");
            }
        }
        if (!configuration.getBoolean("bukkitpermissions", true)) {
            try {
            Bukkit.getPluginManager().enablePlugin(Bukkit.getPluginManager().getPlugin("Permissions"));
            ph = ((Permissions) Bukkit.getPluginManager().getPlugin("Permissions")).getHandler();
            } catch (NullPointerException e) {
                log.warning(logPrefix + "Couldn't load Permissions, reverting to Bukkit.");
            }
        }
        if (configuration.getBoolean("phonehome", true)) phoneHome();
        configuration.save();
    }

    /**
     * Gets all aliases for an item
     * @param query name of item
     * @return names of aliases
     * @throws IllegalArgumentException exception if no such item exists
     */
    public static List<String> getAliases(String query) throws IllegalArgumentException {
        List<String> s = new ArrayList<String>();
        ItemStack i = itemMap.get(query);
        if (i == null)
            throw new IllegalArgumentException("no such item");
        String b = Integer.toString(i.getTypeId());
        int d = i.getDurability();
        if (d != 0)
            b += ";" + Integer.toString(i.getDurability());
        if (items.get(b) != null)
            s.addAll(items.get(b));
        if (d == 0 && items.get(b + ";0") != null)
            s.addAll(items.get(b + ";0"));
        return s;
    }

    /**
     * @return list of all groups
     */
    public static List<String> getGroups() {
        return getGroups("");
    }

    /**
     * Returns group names that start with string
     * @param group name to look for
     * @return list of matching groups
     */
    public static List<String> getGroups(String group) {
        List<String> gs = new ArrayList<String>();
        for (String g : groups.keySet()) {
            if (group.equals("") || (g.length() >= group.length() && g.regionMatches(true, 0, group, 0, group.length())))
                gs.add(g);
        }
        return gs;
    }

    /**
     * @param query item group name
     * @return OddItemGroup
     * @throws IllegalArgumentException exception if no such group exists
     */
    public static OddItemGroup getItemGroup(String query) throws IllegalArgumentException {
        if (groups.get(query) == null) throw new IllegalArgumentException("no such group");
        return groups.get(query);
    }

    /**
     * @return Set&lt;ItemStack&gt; all defined items
     */
    public static Set<ItemStack> getItemStacks() {
        return new HashSet<ItemStack>(itemMap.values());
    }

    /**
     * Returns an ItemStack of quantity 1 of alias query
     * @param query item name
     * @return ItemStack
     * @throws IllegalArgumentException exception if item not found, message contains closest match
     */
    public static ItemStack getItemStack(String query) throws IllegalArgumentException {
        return getItemStack(query, 1);
    }

    /**
     * Returns an ItemStack of specific quantity of alias query
     * @param query item name
     * @param quantity quantity
     * @return ItemStack
     * @throws IllegalArgumentException exception if item not found, message contains closest match
     */
    public static ItemStack getItemStack(String query, Integer quantity) throws IllegalArgumentException {
        ItemStack i;
        if (query.startsWith("map")) {
            try {
                i = new ItemStack(Material.MAP, 1, (query.contains(";") ? Short.valueOf(query.substring(query.indexOf(";")+1)) : 0));
            } catch (NumberFormatException e) {
                i = new ItemStack(Material.MAP, 1, (short) 0);
            }
        } else {
            i = itemMap.get(query);
        }
        if (i != null && !query.startsWith("map")) {
            i.setAmount(quantity);
            return i;
        }
        throw new IllegalArgumentException(bktree.findBestWordMatch(query));
    }

    @Override
    public void onDisable() {
        log.info(logPrefix + "disabled");
    }

    @Override
    public void onEnable() {
        log.info(logPrefix + info.getVersion() + " enabled");
        getCommand("odditem").setExecutor(new OddItemCommandExecutor());
        log.info(logPrefix + itemMap.size() + " aliases loaded.");
        configurationFile = this.getDataFolder() + System.getProperty("file.separator") + "OddItem.yml";
        configure();
    }

    @Override
    public void onLoad() {
        info = getDescription();
        log = getServer().getLogger();
        logPrefix = "[" + info.getName() + "] ";
    }

    private static boolean writeConfig() {
        FileWriter fw;
        try {
            Bukkit.getPluginManager().getPlugin("OddItem").getDataFolder().mkdir();
            fw = new FileWriter(configurationFile);
        } catch (SecurityException e) {
            log.severe(logPrefix + "Couldn't create config dir: " + e.getMessage());
            Bukkit.getPluginManager().disablePlugin(Bukkit.getPluginManager().getPlugin("OddItem"));
            return false;
        } catch (IOException e) {
            log.severe(logPrefix + "Couldn't write config file: " + e.getMessage());
            Bukkit.getPluginManager().disablePlugin(Bukkit.getPluginManager().getPlugin("OddItem"));
            return false;
        }
        BufferedReader i = new BufferedReader(new InputStreamReader(Bukkit.getPluginManager().getPlugin("OddItem").getClass().getResourceAsStream("/OddItem.yml")));
        BufferedWriter o = new BufferedWriter(fw);
        try {
            String line = i.readLine();
            while (line != null) {
                o.write(line + System.getProperty("line.separator"));
                line = i.readLine();
            }
            log.info(logPrefix + "Wrote default config");
        } catch (IOException e) {
            log.severe(logPrefix + "Error writing config: " + e.getMessage());
        } finally {
            try {
                o.close();
                i.close();
            } catch (IOException e) {
                log.severe(logPrefix + "Error saving config: " + e.getMessage());
                Bukkit.getPluginManager().disablePlugin(Bukkit.getPluginManager().getPlugin("OddItem"));
            }
        }
        return true;
    }

    protected void save() {
        configuration = new Configuration(new File(configurationFile));
        configuration.setProperty("items", items);
        configuration.setProperty("groups", groups);
        configuration.save();
    }

    protected static boolean uglyPermission(Player player, String permission) {
        if (ph != null) {
            return ph.has(player, permission);
        }
        return player.hasPermission(permission);
    }

    private static void phoneHome() {
        String url = String.format("http://plugins.blockface.org/usage/update.php?name=%s&build=%s&plugin=%s&port=%s",
                Bukkit.getServer().getName(),
                info.getVersion(),
                info.getName(),
                Bukkit.getServer().getPort());
        try {
            URL blockface = new URL(url);
            URLConnection con = blockface.openConnection();
        } catch (Exception e) {
            return;
        }
    }
}