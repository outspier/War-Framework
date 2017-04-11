package main.java.au.edu.swin.war.framework.game;

import main.java.au.edu.swin.war.framework.WarPlayer;
import main.java.au.edu.swin.war.framework.stored.SerializedLocation;
import main.java.au.edu.swin.war.framework.util.WarManager;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.entity.Hanging;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.*;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.EntityExplodeEvent;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.hanging.HangingBreakEvent;
import org.bukkit.inventory.ItemStack;

import java.util.*;

/**
 * This extensible class stores all &amp; handles
 * some map data. Most map data is manipulated at
 * match runtime if the selected map is playing.
 * <p>
 * Do NOT use WarMap as a direct extension for
 * your map configurations. Certain procedures must
 * be defined on an extra map subclass in the
 * program that actually extends this framework.
 * <p>
 * Check out activate() and deactivate().
 * You must have this defined on another subclass,
 * and not defined in each individual map extension.
 * <p>
 * Created by Josh on 09/04/2017.
 *
 * @author s101601828 @ Swin.
 * @version 1.0
 * @since 1.0
 */
public abstract class WarMap implements Listener {

    /* Do not interfere with these values! */
    protected WarManager main; // The WarManager instance. This allows access to all other crucial modules.
    boolean active = false; // Specified if this map is currently being played.
    boolean wasSet = false; // Was this map manually set out of rotation?

    /* Team-related data. */
    HashMap<String, Object> attributes; // Custom map attributes can be set here if needed.
    HashMap<String, WarTeam> teams; // The list of defined teams available in this map.
    HashMap<String, ArrayList<SerializedLocation>> teamSpawns; // A key/value set defining all team spawns.
    SerializedLocation specSpawn; // The location at which all spectators will initially spawn.

    /* Designation attributes. */
    UUID[] creators = new UUID[]{}; // An array of map creator UUIDs, if applicable.
    String mapName; // The name of the map. For example, "Awesome Arena II"!
    Material[] disabledDrops = new Material[]{}; // A list of disabled drops. One is automatically applied.

    /**
     * Since this class is intialized through reflections,
     * no parameters can be included in the constructor.
     * <p>
     * To work around this, init() is called after the
     * class has actually been initialized to set values.
     *
     * @see main.java.au.edu.swin.war.framework.game.WarMode
     */
    public WarMap() {
        /* Initialize things that need to be ready for the configuration. */
        teams = new HashMap<>(); // The Key/Value set only needs to be cleared on a match end. Do not null or free it.
        teamSpawns = new HashMap<>(); // The same as above applies to the spawns. Please clear instead of nulling.

        /* Here are some default values which can be modified by extended configurations. */
        attributes.put("allDamage", true); // allDamage allows players to take physical PvP damage;
        attributes.put("blockBreak", true); // blockBreak allows players to break blocks;
        attributes.put("blockPlace", true); // blockPlace allows players to place blocks;
        attributes.put("blockExplode", true); // blockExplode allows blocks to be destroyed from an explosion;
        attributes.put("pearlDamage", true); // pearlDamage allows players to take enderpearl collision damage;
        attributes.put("fireSpread", false); // fireSpread allows fire to burn, destroy, and spread to other blocks;
        // -ONLY IF THEY ARE SET TO TRUE!

        attributes.put("matchDuration", 900); // Defines the default match duration as 15 minutes. (900 seconds)
        attributes.put("ffaKills", 20); // Defines the default kill cap needed to win an FFA. (20 kills)
        attributes.put("captureRequirement", 3); // Defines the default amount of flag captures to win a CTF. (3 caps)

        // For the love of god, please call init()!
        // When configuring a map, please call super(); before doing anything else!
        // Review the template if you are confused.
    }

    /**
     * Calling this procedure is IMPORTANT. The program will
     * NOT work if you do not define the WarManager instance.
     * <p>
     * You only need to call this ONCE.
     *
     * @param main The WarManager instance.
     * @see main.java.au.edu.swin.war.framework.game.WarMode
     */
    public void init(WarManager main) {
        this.main = main;
    }

    /**
     * Extend this procedure in another abstract map class to
     * define actions needed to be taken prior to the match
     * starting, such as enabling listeners and activating the map.
     */
    public abstract void activate();

    /**
     * Extend this procedure to handle any changed attributes in
     * the map class since the class itself is NOT cloned.
     * Disable listeners, clear lists, reset values, the usual stuff.
     */
    public abstract void deactivate();

    /**
     * This is the procedure that must be kept abstract for map
     * configuration classes. This procedure will write to the
     * class, defining teams, spawns, names, and other attributes.
     */
    protected abstract void readyAttributes();

    /**
     * Extend this procedure also to define team spawns after
     * defining the attributes in the above abstract procedure.
     */
    protected abstract void readySpawns();

    /**
     * If your configuration requires map-specific instantiations,
     * feel free to override this procedure and write your own code.
     * <p>
     * An example usage of this procedure would be to enable listeners
     * and initialize values to get a map-specific ability to work.
     */
    public void postStart() {
    }

    /**
     * Applies a player's inventory then updates it.
     * <p>
     * This is the procedure your actual program should
     * use, as it clears their inventory and updates it.
     *
     * @param target The player to apply.
     */
    public void applyInv(WarPlayer target) {
        main.items().clear(target);
        applyInventory(target);
        target.getPlayer().updateInventory();
    }

    /**
     * Applies a player's inventory for the map.
     * <p>
     * Extend this procedure to give the relevant player
     * their kit for this map when they spawn or respawn.
     *
     * @param target The player to apply.
     */
    protected abstract void applyInventory(WarPlayer target);

    /**
     * Quick procedure to set both the block placing and breaking rules.
     *
     * @param blockBreak Whether block breaking is allowed.
     * @param blockPlace Whether block placing is allowed.
     */
    public void setAllowBuild(boolean blockBreak, boolean blockPlace) {
        attributes.put("blockBreak", blockBreak);
        attributes.put("blockPlace", blockPlace);
    }

    /**
     * Defines whether or not Minecraft monsters/animals can
     * naturally spawn on the map terrain while a round is in progress.
     *
     * @param mobSpawning Mob spawning definition rule.
     * @see org.bukkit.entity.LivingEntity
     * @deprecated Using the gamerule doMobSpawning = false is more effective!
     */
    @Deprecated
    public void setMobSpawning(boolean mobSpawning) {
        attributes.put("mobSpawning", mobSpawning);
    }

    /**
     * Defines the time in the map world manually,
     * and then locks the time permanently to that.
     *
     * @param timeLockTime The time at which to be locked to the map.
     * @deprecated Using the gamerule doDaylightCycle = false is more effective!
     */
    @Deprecated
    public void setTimeLockTime(long timeLockTime) {
        attributes.put("timeLockTime", timeLockTime);
        attributes.put("timeLock", true);
    }

    /**
     * Sets an array of items for which to are
     * not be dropped by a player when they die.
     *
     * @param disabledDrops Items that will not be dropped on death.
     * @see PlayerDeathEvent
     */
    public void setDisabledDrops(Material[] disabledDrops) {
        this.disabledDrops = disabledDrops;
    }

    /**
     * Instantiates and fills an array of items for which
     * are to not be dropped by a player when they die.
     * <p>
     * The items listed are amongst the most common that
     * will be used. Configurations can have their own unique
     * drop lists if needed.
     *
     * @see PlayerDeathEvent;
     */
    public Material[] defaultDisabledDrops() {
        return new Material[]{Material.LEATHER_BOOTS, Material.LEATHER_LEGGINGS, Material.LEATHER_CHESTPLATE,
                Material.LEATHER_HELMET, Material.WOOD_SWORD, Material.STONE_SWORD, Material.IRON_BOOTS, Material.IRON_LEGGINGS,
                Material.IRON_CHESTPLATE, Material.IRON_HELMET, Material.IRON_SWORD, Material.GOLD_BOOTS, Material.GOLD_LEGGINGS,
                Material.GOLD_CHESTPLATE, Material.GOLD_HELMET, Material.GOLD_SWORD, Material.BOW, Material.DIAMOND_BOOTS,
                Material.DIAMOND_LEGGINGS, Material.DIAMOND_CHESTPLATE, Material.DIAMOND_HELMET, Material.DIAMOND_SWORD, Material.ARROW,
                Material.FISHING_ROD, Material.GLASS_BOTTLE, Material.WOOL, Material.CHAINMAIL_BOOTS, Material.CHAINMAIL_LEGGINGS,
                Material.CHAINMAIL_CHESTPLATE, Material.CHAINMAIL_HELMET, Material.IRON_AXE, Material.IRON_PICKAXE, Material.STONE_PICKAXE,
                Material.STONE_AXE, Material.WOOD_HOE, Material.STONE_HOE, Material.GOLD_HOE, Material.IRON_HOE,
                Material.DIAMOND_HOE, Material.DIAMOND_PICKAXE};
    }

    /**
     * Returns the maximum duration of the match, in seconds.
     * By default, this is set to 900 seconds. (15 minutes)
     * <p>
     * As documented, the match automatically ends if this time out
     * out, even if the objective of the gamemode is not fulfilled.
     *
     * @return The maximum duration of the match.
     */
    public long getMatchDuration() {
        return (Long) attributes.get("matchDuration");
    }

    /**
     * This function should conventionally return a converted
     * form of the specSpawn field. This should be done by
     * your program's extension of this map framework class.
     *
     * @return The spectator spawn for the map.
     */
    public abstract Location getSpectatorSpawn();

    /**
     * Sets where the spectators will spawn before, during, and after the match.
     * Extend this procedure in your readySpawns() procedure for consistency.
     *
     * @param spectatorSpawn The spectator spawn.
     */
    public void setSpectatorSpawn(SerializedLocation spectatorSpawn) {
        this.specSpawn = spectatorSpawn;
    }

    /**
     * After a WarTeam is created, it should be registered into the Key/Value set.
     * These should never be modified, since they are used by the gamemode runtime.
     *
     * @param team The team to register.
     */
    public void registerTeam(WarTeam team) {
        if (team == null) {
            main.plugin().log("We failed to load the team as it was null!");
            return;
        }
        teams.put(team.toString(), team);
    }

    /**
     * Returns all registered teams.
     * This should be used by the active gamemode to handle teams.
     *
     * @return All registered teams.
     * @see WarMode
     */
    public Collection<WarTeam> getTeams() {
        return teams.values();
    }

    /**
     * Returns the list of spawns for a team.
     * You may insert the Team object as the parameter,
     * since toString() returns the Team's name.
     *
     * @param team The team's scoreboard name.
     * @return The team's spawn list.
     */
    public ArrayList<SerializedLocation> getTeamSpawns(String team) {
        return teamSpawns.get(team);
    }

    /**
     * Adds a spawnpoint to a team.
     * !IMPORTANT! Never do this before registering teams! Bad things will happen!
     *
     * @param team  The team to add the spawn to.
     * @param spawn The team's spawn.
     */
    public void addTeamSpawn(WarTeam team, SerializedLocation spawn) {
        if (!teamSpawns.containsKey(team.toString()))
            teamSpawns.put(team.toString(), new ArrayList<SerializedLocation>()); // Initialize the array list first!
        teamSpawns.get(team.toString()).add(spawn); // Add the team spawn to the team's spawn list.
    }

    /**
     * Returns the designated name for this map.
     *
     * @return The map name.
     */
    public String getMapName() {
        return mapName;
    }

    /**
     * Sets the designated name for this map.
     *
     * @param mapName The name of the map.
     */
    public void setMapName(String mapName) {
        this.mapName = mapName;
    }

    /**
     * Returns the creators of this map.
     *
     * @return The creators of this map.
     */
    public UUID[] getCreators() {
        return creators;
    }

    /**
     * Sets the creators of this map. Please ensure you know how
     * universially unique idenfiers work and how to get your IGN's
     * UUID.
     *
     * @param creators The creators of this map.
     * @see UUID
     */
    public void setCreators(UUID[] creators) {
        this.creators = creators;
    }

    /**
     * Returns whether the map is currently being played this match.
     * This active state is automatically controlled by the gamemode.
     *
     * @return Whether the map is active or not.
     */
    public boolean isActive() {
        return active;
    }

    /**
     * Sets this map to 'active', designating that it is the map being
     * played this match. The gamemode controls this attribute.
     *
     * @param active Whether the map is active or not.
     */
    public void setActive(boolean active) {
        this.active = active;
    }

    /**
     * If block breaking is disabled, and explosions are disabled,
     * explosions will not damage the terrain of the map.
     *
     * @param event An event called by the server.
     */
    @EventHandler
    public void entityExplode(EntityExplodeEvent event) {
        if (!(Boolean) attributes.get("blockBreak") && !(Boolean) attributes.get("blockExplode"))
            event.blockList().clear();
    }

    /**
     * Causes any disabled drops to be removed from the "drop list"
     * when a player dies and drops all their items.
     *
     * @param event An event called by the server.
     */
    @EventHandler
    public void entityDeath(PlayerDeathEvent event) {
        for (ItemStack drop : event.getDrops()) {
            if (Arrays.asList(disabledDrops).contains(drop.getType()))
                drop.setType(Material.AIR);
        }
    }

    /**
     * If fire spreading is disabled, blocks will not catch on fire or spread.
     *
     * @param event An event called by the server.
     */
    @EventHandler
    public void onBlockSpread(BlockSpreadEvent event) {
        if (event.getSource().getType() == Material.FIRE && !(Boolean) attributes.get("fireSpread"))
            event.setCancelled(true);
    }

    /**
     * This procedure shares the same rule as onBlockSpread(event);
     *
     * @param event An event called by the server.
     */
    @EventHandler
    public void onIgnite(BlockIgniteEvent event) {
        if (event.getCause() == BlockIgniteEvent.IgniteCause.SPREAD && !(Boolean) attributes.get("fireSpread"))
            event.setCancelled(true);
    }

    /**
     * This procedure shares the same rule as onBlockSpread(event);
     *
     * @param event An event called by the server.
     */
    @EventHandler
    public void onBurn(BlockBurnEvent event) {
        if (!(Boolean) attributes.get("fireSpread")) event.setCancelled(true);
    }

    /**
     * If block breaking is disabled, paintings/item frames cannot be broken.
     *
     * @param event An event called by the server.
     */
    @EventHandler
    public void hngbrk(HangingBreakEvent event) {
        if (!(Boolean) attributes.get("blockBreak")) event.setCancelled(true);
    }

    /**
     * If block breaking is disabled, paintings/item frames cannot be broken.
     *
     * @param event An event called by the server.
     */
    @EventHandler
    public void hngbrk(EntityDamageEvent event) {
        if (!(Boolean) attributes.get("blockBreak") && event.getEntity() instanceof Hanging) event.setCancelled(true);
    }

    /**
     * If block breaking is disabled, blocks will re-appear when broken.
     *
     * @param event An event called by the server.
     */
    @EventHandler
    public void brk(BlockBreakEvent event) {
        if (!(Boolean) attributes.get("blockBreak")) event.setCancelled(true);
    }

    /**
     * If block placing is disabled, blocks will disappear when placed.
     *
     * @param event An event called by the server.
     */
    @EventHandler
    public void plc(BlockPlaceEvent event) {
        if (!(Boolean) attributes.get("blockPlace")) event.setCancelled(true);
    }
}
