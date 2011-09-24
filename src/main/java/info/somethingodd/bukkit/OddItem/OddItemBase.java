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

import org.bukkit.Bukkit;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.logging.Logger;

/**
 * @author Gordon Pettey (petteyg359@gmail.com)
 */
public class OddItemBase extends JavaPlugin {
    protected Logger log = null;
    protected String logPrefix = null;
    protected String configFile = null;

    @Override
    public void onDisable() {
        log.info(logPrefix + "disabled");
        OddItem.clear();
        logPrefix = null;
        log = null;
        configFile = null;
    }

    @Override
    public void onEnable() {
        log = Bukkit.getServer().getLogger();
        logPrefix = "[" + getDescription().getName() + "] ";
        log.info(logPrefix + getDescription().getVersion() + " enabled");
        configFile = getDataFolder() + System.getProperty("file.separator") + "OddItem.yml";
        try {
            OddItemConfiguration configuration = new OddItemConfiguration(this);
            configuration.configure(configFile);
        } catch (Exception e) {
            log.severe(logPrefix + "Configuration error!");
            e.printStackTrace();
        }
        getCommand("odditem").setExecutor(new OddItemCommandExecutor(this));
        log.info(logPrefix + OddItem.itemMap.size() + " aliases loaded.");
    }
}