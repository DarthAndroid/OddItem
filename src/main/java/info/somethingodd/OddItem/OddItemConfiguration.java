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
package info.somethingodd.OddItem;

import info.somethingodd.OddItem.configuration.OddItemAliases;
import info.somethingodd.OddItem.configuration.OddItemGroup;
import info.somethingodd.OddItem.configuration.OddItemGroups;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.configuration.serialization.ConfigurationSerialization;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;

/**
 * @author Gordon Pettey (petteyg359@gmail.com)
 */
public class OddItemConfiguration {
    private static String comparator;
    private static int maxBlockId;
    private final OddItemBase oddItemBase;

    public OddItemConfiguration(OddItemBase oddItemBase) {
        this.oddItemBase = oddItemBase;
    }

    public static String getComparator() {
        return comparator;
    }

    public static int getMaxBlockId() {
        return maxBlockId;
    }

    public void configure() {
        try {
            initialConfig("config.yml");
            initialConfig("groups.yml");
            initialConfig("items.yml");
        } catch (Exception e) {
            oddItemBase.log.warning("Exception writing initial configuration files: " + e.getMessage());
            e.printStackTrace();
        }
        YamlConfiguration yamlConfiguration = (YamlConfiguration) oddItemBase.getConfig();
        comparator = yamlConfiguration.getString("comparator", "r");
        maxBlockId = yamlConfiguration.getInt("maxBlockId", 256);

        ConfigurationSerialization.registerClass(OddItemAliases.class);

        ConfigurationSerialization.registerClass(OddItemAliases.class);
        YamlConfiguration itemConfiguration = new YamlConfiguration();
        try {
            itemConfiguration.load(new File(oddItemBase.getDataFolder(), "items.yml"));
        } catch (Exception e) {
            oddItemBase.log.warning("Error opening items.yml!");
            e.printStackTrace();
        }
        YamlConfiguration itemConfigurationDefault = new YamlConfiguration();
        try {
            itemConfigurationDefault.load(oddItemBase.getResource("items.yml"));
            itemConfiguration.setDefaults(itemConfigurationDefault);
        } catch (Exception e) {
            oddItemBase.log.warning("Error opening default resource for items.yml!");
            e.printStackTrace();
        }
        OddItem.items = OddItemAliases.valueOf(itemConfiguration.getConfigurationSection("items").getValues(false));

        ConfigurationSerialization.registerClass(OddItemGroup.class);
        ConfigurationSerialization.registerClass(OddItemGroups.class);
        YamlConfiguration groupConfiguration = new YamlConfiguration();
        try {
            groupConfiguration.load(new File(oddItemBase.getDataFolder(), "groups.yml"));
        } catch (Exception e) {
            oddItemBase.log.warning("Error opening groups.yml!");
            e.printStackTrace();
        }
        OddItem.groups = OddItemGroups.valueOf(groupConfiguration.getConfigurationSection("groups").getValues(false));
    }

    private void initialConfig(String filename) throws IOException {
        File file = new File(oddItemBase.getDataFolder(), filename);
        if (!file.exists()) {
            BufferedReader src = null;
            BufferedWriter dst = null;
            try {
                file.getParentFile().mkdirs();
                file.createNewFile();
                src = new BufferedReader(new InputStreamReader(getClass().getResourceAsStream("/" + filename)));
                dst = new BufferedWriter(new FileWriter(file));
                String line = src.readLine();
                while (line != null) {
                    dst.write(line + "\n");
                    line = src.readLine();
                }
                src.close();
                dst.close();
                oddItemBase.log.info("Wrote default " + filename);
            } catch (IOException e) {
                oddItemBase.log.warning("Error writing default " + filename);
            } finally {
                try {
                    src.close();
                    dst.close();
                } catch (Exception e) {
                    oddItemBase.log.severe("Your system is whack.");
                }
            }
        }
    }
}
