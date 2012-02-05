/* This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, "either" version 3 of the License, "or"
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, "see" <http://www.gnu.org/licenses/>.
 */
package info.somethingodd.bukkit.OddItem;

import info.somethingodd.bukkit.OddItem.bktree.BKTree;
import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.inventory.ItemStack;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;

/**
 * @author Gordon Pettey (petteyg359@gmail.com)
 */
public class OddItemConfiguration {
    private OddItemBase oddItemBase;
    private static String comparator = "l";

    public OddItemConfiguration(OddItemBase oddItemBase) {
        this.oddItemBase = oddItemBase;
    }

    public void configure() {
        File configurationFile = new File(oddItemBase.getDataFolder() + File.separator + "OddItem.yml");
        YamlConfiguration defaultConfiguration = new YamlConfiguration();
        try {
            defaultConfiguration.load(oddItemBase.getResource("OddItem.yml"));
        } catch (Exception e) {
            oddItemBase.log.warning(oddItemBase.logPrefix + "Error loading default configuration: " + e.getMessage());
            e.printStackTrace();
        }
        YamlConfiguration configuration = new YamlConfiguration();
        try {
            configuration.load(configurationFile);
            configuration.setDefaults(defaultConfiguration);
            configuration.save(configurationFile);
        } catch (Exception e) {
            oddItemBase.log.warning(oddItemBase.logPrefix + "Error loading configuration: " + e.getMessage());
            e.printStackTrace();
        }

        OddItem.itemMap = Collections.synchronizedMap(new HashMap<String, ItemStack>());
        OddItem.items = Collections.synchronizedMap(new HashMap<String, Set<String>>());
        OddItem.groups = Collections.synchronizedMap(new HashMap<String, OddItemGroup>());

        String comparator = configuration.getString("comparator");
        if (comparator.equalsIgnoreCase("c") || comparator.equalsIgnoreCase("caverphone")) {
            OddItem.bktree = new BKTree<String>("c");
            oddItemBase.log.info(oddItemBase.logPrefix + "Using Caverphone for suggestions.");
        } else if (comparator.equalsIgnoreCase("k") || comparator.equalsIgnoreCase("cologne")) {
            OddItem.bktree = new BKTree<String>("k");
            oddItemBase.log.info(oddItemBase.logPrefix + "Using ColognePhonetic for suggestions.");
        } else if (comparator.equalsIgnoreCase("m") || comparator.equalsIgnoreCase("metaphone")) {
            OddItem.bktree = new BKTree<String>("m");
            oddItemBase.log.info(oddItemBase.logPrefix + "Using Metaphone for suggestions.");
        } else if (comparator.equalsIgnoreCase("s") || comparator.equalsIgnoreCase("soundex")) {
            OddItem.bktree = new BKTree<String>("s");
            oddItemBase.log.info(oddItemBase.logPrefix + "Using SoundEx for suggestions.");
        } else if (comparator.equalsIgnoreCase("r") || comparator.equalsIgnoreCase("refinedsoundex")) {
            OddItem.bktree = new BKTree<String>("r");
            oddItemBase.log.info(oddItemBase.logPrefix + "Using RefinedSoundEx for suggestions.");
        } else {
            OddItem.bktree = new BKTree<String>("l");

            oddItemBase.log.info(oddItemBase.logPrefix + "Using Levenshtein for suggestions.");
        }

        ConfigurationSection itemsSection = configuration.getConfigurationSection("items");
        for (String i : itemsSection.getKeys(false)) {
            int id;
            short d = 0;
            Material m;
            if (i.contains(";")) {
                try {
                    d = Short.parseShort(i.substring(i.indexOf(";") + 1));
                    id = Integer.parseInt(i.substring(0, i.indexOf(";")));
                    m = Material.getMaterial(id);
                } catch (NumberFormatException e) {
                    m = Material.getMaterial(i.substring(0, i.indexOf(";")));
                    id = m.getId();
                }
            } else {
                try {
                    id = Integer.decode(i);
                    m = Material.getMaterial(id);
                } catch (NumberFormatException e) {
                    m = Material.getMaterial(i);
                    id = m.getId();
                }
            }
            if (OddItem.items.get(i) == null)
                OddItem.items.put(i, Collections.synchronizedSet(new TreeSet<String>(String.CASE_INSENSITIVE_ORDER)));
            List<String> itemAliases = new ArrayList<String>();
            itemAliases.addAll(itemsSection.getStringList(i));
            itemAliases.add(id + ";" + d);
            // Add all aliases
            OddItem.items.get(i).addAll(itemAliases);
            if (m != null) {
                for (String itemAlias : itemAliases) {
                    OddItem.itemMap.put(itemAlias, new ItemStack(m, 1, d));
                    OddItem.bktree.add(itemAlias);
                }
            } else {
                oddItemBase.log.warning(oddItemBase.logPrefix + "Invalid format: " + i);
            }
        }

        ConfigurationSection groupsSection = configuration.getConfigurationSection("groups");
        if (OddItem.groups != null) {
            for (String g : groupsSection.getKeys(false)) {
                List<String> i = new ArrayList<String>();
                if (groupsSection.getConfigurationSection(g) == null) {
                    i.addAll(groupsSection.getStringList(g));
                    ConfigurationSection gS = groupsSection.createSection(g);
                    gS.set("items", i);
                    gS.set("data", null);
                    groupsSection.set(g, gS);
                } else {
                    i.addAll(groupsSection.getConfigurationSection(g).getStringList("items"));
                }
                List<ItemStack> itemStackList = new ArrayList<ItemStack>();
                for (String is : i) {
                    ItemStack itemStack;
                    Integer q = null;
                    try {
                        if (is.contains(",")) {
                            q = Integer.valueOf(is.substring(is.indexOf(",") + 1));
                            is = is.substring(0, is.indexOf(","));
                            itemStack = OddItem.getItemStack(is, q);
                        } else {
                            itemStack = OddItem.getItemStack(is);
                        }
                        oddItemBase.log.info(oddItemBase.logPrefix + "Adding " + is + (q != null ? " x" + q : "") + " to group \"" + g + "\"");
                        if (itemStack != null) itemStackList.add(itemStack);
                    } catch (IllegalArgumentException e) {
                        oddItemBase.log.warning(oddItemBase.logPrefix + "Invalid item \"" + is + "\" in group \"" + g + "\"");
                        OddItem.groups.remove(g);
                    } catch (NullPointerException e) {
                        oddItemBase.log.warning(oddItemBase.logPrefix + "NPE adding ItemStack \"" + is + "\" to group " + g);
                    }
                    OddItem.groups.put(g, new OddItemGroup(itemStackList, groupsSection.getConfigurationSection("data")));
                }
                if (OddItem.groups.get(g) != null)
                    oddItemBase.log.info(oddItemBase.logPrefix + "Group " + g + " added.");
            }
        }
    }

    public static String getComparator() {
        return new String(comparator);
    }
}
