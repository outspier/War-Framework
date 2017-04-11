package main.java.au.edu.swin.war.framework.game;

import main.java.au.edu.swin.war.framework.WarPlayer;
import main.java.au.edu.swin.war.framework.stored.SerializedLocation;
import main.java.au.edu.swin.war.framework.util.WarManager;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.GameMode;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;
import org.bukkit.event.HandlerList;
import org.bukkit.event.Listener;
import org.bukkit.scheduler.BukkitTask;
import org.bukkit.scoreboard.Scoreboard;
import org.bukkit.scoreboard.Team;

import java.util.*;

/**
 * This extensible class handles all gamemode-related
 * function that is commonly shared among classes.
 * When creating new gamemodes, make sure they extend
 * this class otherwise the program will NOT work.
 * <p>
 * Created by Josh on 20/03/2017.
 *
 * @author s101601828 @ Swin.
 * @version 1.0
 * @since 1.0
 */
public abstract class WarMode implements Listener {

    // !! IMPORTANT !! //
    /* Ensure that these fields are initialized & freed when needed. */
    BukkitTask runtimeTask; // Global gamemode-specific runtime task.
    boolean permaDeath; // Specifies that permanent death is enabled.
    Team spec; // Holds the Spigot team extension for the spectators.
    boolean active; // Whether or not this class is active during a match.
    int timeElapsed; // Specifies the number of seconds elapsed during the match.
    Scoreboard score; // Holds the Spigot scoreboard extension that players see.
    WarMap map;

    /* HashMaps that must be initialized/freed on a match start/end. */
    protected HashMap<String, WarTeam> teams; // Temporary Key/Value set to hold maps for the match.
    protected HashMap<String, ArrayList<SerializedLocation>> teamSpawns; // Temporary Key/Value set to hold Team spawns.

    protected WarManager main; // The WarManager instance. This allows access to all other crucial modules.

    /**
     * Since this class is intialized through reflections,
     * no parameters can be included in the constructor.
     * <p>
     * To work around this, init() is called after the
     * class has actually been initialized to set values.
     */
    public WarMode() {
        // Call init() externally
    }

    /**
     * Calling this procedure is IMPORTANT. The program will
     * NOT work if you do not define the WarManager instance.
     * <p>
     * You only need to call this ONCE.
     *
     * @param main The WarManager instance.
     */
    public void init(WarManager main) {
        this.main = main;
        teams = new HashMap<>(); // The Key/Value set only needs to be cleared on a match end. Do not null or free it.
        teamSpawns = new HashMap<>(); // The same as above applies to the spawns. Please clear instead of nulling.
    }

    /**
     * Required procedure in all external classes.
     * Configure this procedure to reset everything that needs
     * to be fresh for when the gamemode is activated once
     * again. i.e. Team Death Match scores, etc.
     */
    public abstract void reset();

    /**
     * ! IMPORTANT !
     * ! DO NOT CONFUSE THIS WITH INIT()!
     * <p>
     * This procedure should be configured in a way that all
     * appropriate values are loaded into the gamemode class
     * when the map is loaded also.
     * <p>
     * Do NOT load or change any fields that are a member of
     * this abstract class. Only initialize what is needed for
     * the specific gamemode, such as the TDM scores being set to 0.
     */
    public abstract void initialize();

    /**
     * This function simply makes a call to the gamemode
     * to update every player's scoreboard. This should be
     * done whenever a score changes, such as the amount of
     * points a team has on Team Death Match.
     * <p>
     * Try not to call this too much, as it is intensive and
     * can flicker a lot if not used carefully.
     * <p>
     * If the gamemode doesn't use a scoreboard, ignore this.
     *
     * @see org.bukkit.scoreboard.Scoreboard
     */
    public abstract void updateScoreboard();

    /**
     * This documentation will explain the 4 below functions:
     * <p>
     * 1. Returns the shortened abbreviation of a gamemode. i.e. TDM.
     * 2. Returns the full gamemode name. i.e. Team Death Match.
     * 3. Returns the offensive tactic. i.e. Kill enemy players!
     * 4. Returns the defensive tactic. i.e. Protect your teammates!
     *
     * @return The relevant result for the requested function.
     */
    public abstract String getName();

    public abstract String getFullName();

    public abstract String getOffensive();

    public abstract String getDefensive();

    /**
     * Returns the correct grammar of the WarMode for broadcasts.
     * For example, 'a' TDM, or 'an' FFA.
     * 1. The current match is a TDM at This Map!
     * 2. The current match is an FFA at This Map!
     * <p>
     * You wouldn't say an TDM or a FFA, would you?
     *
     * @return The correct grammar of the DreamMode.
     */
    public abstract String getGrammar();

    /**
     * A procedure that is run when a player dies.
     * You must configure this yourself in an external gamemode class.
     * <p>
     * This procedure is automatically called when a player dies.
     *
     * @param killed The player who died.
     * @param killer The player's killer, if any.
     * @see org.bukkit.event.entity.PlayerDeathEvent below.
     * <p>
     * An example of this procedure would be to credit the killer's team 1 point in TDM.
     */
    public abstract void onKill(WarPlayer killed, WarPlayer killer);

    /**
     * Called when a player leaves.
     * You must configure this yourself in an external gamemode class.
     * As stated above, this is called automatically.
     * <p>
     * An example of this procedure would be to penalize a team or modify
     * the match in some way to compensate for the player leaving.
     * i.e. a flagholder in CTF leaving the match.
     *
     * @param left The player who left.
     */
    public abstract void onLeave(WarPlayer left);

    /**
     * This performs the opposite of above, so read the documentation
     * that is provided above. Please extend and utilise.
     * <p>
     * An example of this procedure would be to modify the match in
     * some way to compensate for the player joining.
     * <p>
     * i.e. to display appropriate data to the
     * player depending on what team they joined.
     *
     * @param joined The player who joined.
     */
    public abstract void onJoin(WarPlayer joined);

    /**
     * A function that is run when the match is ended forcibly.
     * This is not an essential function in gamemode management,
     * but acts as more of a debug if an operator wants to end a
     * match early to perhaps test cycling or other maps loaded.
     * <p>
     * Once again, please configure this function if you are going to use it.
     *
     * @return Whether or not the match was able to be safely ended.
     */
    public abstract boolean onForceEnd();

    /**
     * Returns the gamemode's loaded teams, if any.
     * Teams should only be loaded in this gamemode
     * if there is a match running and this was the
     * gamemode that was selected.
     *
     * @return The active teams Key/Value set.
     */
    public Collection<WarTeam> getTeams() {
        return teams.values();
    }

    /**
     * This procedure should be called from the external class
     * once a match has fulfilled its criteria.
     * <p>
     * i.e. time running out in TDM,
     * i.e. reaching score cap in FFA, etc.
     */
    public void finish() {
        setActive(false); // Sets the gamemode instance to inactive.
        if (teamSpawns != null)
            teamSpawns.clear(); // CLEAR the Key/Value set, do not free it.
        //DreamSequence.getInstance().cycle(s());
    }

    /**
     * Increases the time elapsed in the match by 1.
     * This procedure is automatically called every
     * second by the runtimeTask.
     *
     * @see org.bukkit.scheduler.BukkitRunnable;
     */
    public void incrementTimeElapsed() {
        timeElapsed = timeElapsed + 1;
    }

    /**
     * Sets -specifically- the amount of time elapsed
     * in the match. Mainly for debugging purposes.
     */
    public void setTimeElapsed(int timeElapsed) {
        this.timeElapsed = timeElapsed;
    }

    /**
     * Returns the amount of time elapsed during this match.
     *
     * @return The amount of time elapsed.
     */
    public int getTimeElapsed() {
        return timeElapsed;
    }

    /**
     * Returns the current map associated with this gamemode
     * during the current match. This is stored temporarily
     * so certain attributes in the WarMap class can be accessed
     * during runtime.
     *
     * @return The current associated map.
     */
    public WarMap map() {
        return map;
    }

    /**
     * Awaken this gamemode for the match. A -LOT- of things
     * will be done automatically here, and will be documented.
     * <p>
     * In a nutshell, once everything is good to go and the match
     * has started, this will awaken the gamemode and objectives
     * will become available.
     *
     * @see java.lang.Runnable
     * @see org.bukkit.scheduler.BukkitScheduler
     */
    @SuppressWarnings("unchecked")
    public void activate() {
        main.plugin().getServer().getPluginManager().registerEvents(this, main.plugin()); // Allows the server to listen in on events for this gamemode class.
        targetedMap = DreamModeMain.getInstance().getCurrentMap();

        main.plugin().log("DEBUG: We have activated the match!");

        for (WarTeam team : map().getTeams())
            // Copies every WarTeam defined in the map over to the gamemode!
            teams.put(team.getTeamName(), team.clone());

        // Copies every spawnpoint for every team defined in the map also!
        teamSpawns = (HashMap<String, ArrayList<SerializedLocation>>) map().teamSpawns.clone();

        setActive(true); // Sets this gamemode as active and will be recognised as so by the program.

        for (WarTeam team : teams.values()) {
            Team lTeam = score.registerNewTeam(team.getTeamName()); // Creates a Spigot Team instance for this team.
            team.setBukkitTeam(lTeam); // Assigns the Spigot Team to the copied WarTeam instance.
            lTeam.setCanSeeFriendlyInvisibles(true); // Allows teammates to see each other when visible. (Spigot)
            lTeam.setAllowFriendlyFire(false); // Disables friendly fire for teammates. (Spigot)
            lTeam.setPrefix(team.getTeamColor() + ""); // Sets the player's name color to the team's color. (Spigot)
        }

        spec = score.registerNewTeam("Spectators"); // Manually defines the spectator team. (Spigot)
        spec.setCanSeeFriendlyInvisibles(true); // Allows spectators to see each other. (Spigot)
        spec.setAllowFriendlyFire(false); // Disables friendly fire. (Spigot)
        spec.setPrefix(ChatColor.LIGHT_PURPLE + ""); // Spectators are purple!!! (Spigot)

        for (WarPlayer wp : main.getWarPlayers().values())
            spec.addPlayer(wp.getPlayer()); // Adds every player to the spectator team by default. (Spigot)

        initialize(); // Initializes everything in the external gamemode class!

        runtimeTask = Bukkit.getScheduler().runTaskTimer( // Runs a task timer at a regular interval. (Spigot)
                main.plugin(), new Runnable() { // Defines the plugin executing the timer and the runnable interface. (Spigot)
                    public void run() {
                        /*if (DreamSequence.getInstance().status != DreamSequence.Status.STARTED) {
                            genTask.cancel();
                            return;
                        }*/
                        incrementTimeElapsed(); // Increments the time elapsed, every second!

                        int timeLeft = getMatchDuration() - getTimeElapsed(); // Calculates the amount of time remaining.
                        if (timeLeft % 60 == 0 && timeLeft != 0) { // Checks that the time is a remainder of
                            int minutes = (timeLeft / 60); // Calculates number of minutes remaining.
                            String s = (minutes == 1 ? "" : "s"); // Should it be 'minute' or 'minutes'?

                            // Broadcasts the amount of minutes remaining.
                            Bukkit.broadcastMessage("There is " + minutes + " minute" + s + " remaining!");

                            //DreamUtil.countdownTune(2);
                        } else if (timeLeft == 30) {
                            // Broadcasts that there is 30 seconds remaining.
                            Bukkit.broadcastMessage("There is " + timeLeft + " seconds remaining!");

                            //DreamUtil.countdownTune(2);
                        } else if (timeLeft < 6 && timeLeft > 0) {
                            String s = (timeLeft == 1 ? "" : "s"); // Calculates number of seconds remaining.

                            // Broadcasts the amount of seconds.
                            Bukkit.broadcastMessage("There is " + timeLeft + " second" + s + " remaining!");

                            // DreamUtil.countdownTune(2);
                        }

                        tick(); // Allows the external class to execute certain procedures every second too.

                        if (getTimeElapsed() == getMatchDuration())
                            onForceEnd(); // If the time is up, force end the match even if the objective is not complete.
                    }
                }, 0L, 20L); // Have a 0 tick delay before starting the task, and repeat every 20 ticks.
        // ! IMPORTANT ! A 'tick' is a 20th of a second. Minecraft servers run at 20 ticks per second. (TPS)
    }

    /**
     * This procedure is automatically called by the runtimeTask
     * every 20 ticks, or every 1 second. You must configure this
     * procedure, but you don't have to use it if it isn't needed.
     * <p>
     * An example usage of this would be to shoot up a firework every
     * 20 ticks at a flagholder's location to show everyone else where
     * they currently are.
     */
    public abstract void tick();

    /**
     * This procedure incapacitates the gamemode after the match
     * has been completed. Listeners are disabled, and all fields
     * that were changed during the match are reset.
     */
    public void deactivate() {
        if (runtimeTask != null) runtimeTask.cancel(); // If the task isn't null already, cancel the task first.
        runtimeTask = null; // Free up the task in memory.
        HandlerList.unregisterAll(this); // Unregister all listener handlers for this class. (Spigot)
        setActive(false); // Sets this gamemode as inactive and will be ignored by the program.
        reset(); // Resets any other values in the external class.
        resetLocalValues(); // Resets values defined in this class as stated below.
        map = null; // Frees up the currently playing map's assignment in memory.
    }

    /**
     * Similar to reset(), this procedure will automatically
     * reset any values that are commonly shared amongst all
     * gamemode classes, such as the time elapsed.
     */
    private void resetLocalValues() {
        timeElapsed = 0; // Sets time elapsed back to 0 seconds.
        permaDeath = false; // Sets permadeath for this gamemode back to the default of false.

        for (WarTeam team : teams.values()) {
            for (OfflinePlayer pl : team.getBukkitTeam().getPlayers())
                team.getBukkitTeam().removePlayer(pl); // Removes the player from the defined Spigot team. (Spigot)
            team.setBukkitTeam(null); // Sets the associated Spigot team to null to free up memory.
        }
        spec = null; // Removes the spectator team to free up memory.
        score = Bukkit.getScoreboardManager().getNewScoreboard(); // Re-assign the scoreboard field with a fresh one.
        if (teams != null)
            teams.clear(); // Clear associated teams to free up memory.
    }

    /**
     * Quick function to return the global Scoreboard.
     * This is to make the code look cleaner.
     *
     * @return The scoreboard associated with this gamemode.
     */
    public Scoreboard s() {
        return score;
    }

    /**
     * Returns whether or not this gamemode is marked
     * as active or not. A gamemode should only be marked
     * as active when the match playing is the gamemode
     * in question.
     *
     * @return Whether the gamemode is active or not.
     */
    public boolean isActive() {
        return active;
    }

    /**
     * Sets the gamemode as 'active' or not.
     * As stated above, only the associated gamemode
     * should be marked as active during a match.
     *
     * @param active Whether the core is active or not.
     */
    public void setActive(boolean active) {
        this.active = active;
    }

    /**
     * Handles entry into and out of the match.
     * If the player tries to enter a match during permadeath, deny it.
     * If the player joins, assign their team and call onJoin().
     * If the player leaves, disassociate them and call onLeave();
     * If the match is permadeath, do not message the player.
     *
     * @param dp The player to handle.
     */
    public void entryHandle(WarPlayer dp) {
        Player pl = dp.getPlayer(); // Returns Spigot's implementation of Player. (Spigot)
        if (!isActive()) return; // If this gamemode is not active, do not execute anything.
        if (permaDeath && dp.isJoined()) {
            // Alert the player that permanent death is enabled and cancel the entry.
            pl.sendMessage("You are too late to join!");
            dp.setJoined(false);
        } else if (dp.isJoined()) {
            // Assign the player to their team and call onJoin() for the external class.
            carryOutTeam(dp, getSmallestTeam());
            onJoin(dp);
        } else { // If the player did not join, execute a leaving handle.
            if (!permaDeath)
                pl.sendMessage("You have left the match!"); // Alert the player physically if this is not a permadeath match.
            WarTeam team = dp.getCurrentTeam(); // Returns the player's associated team for temporary use.
            dp.setCurrentTeam(null); // Disassociates the player with their team.
            pl.teleport(map().getSpectatorSpawn()); // Teleports the player to the map's spectator spawnpoint. (Spigot)
            pl.setGameMode(GameMode.SPECTATOR); // Sets the player to spectator mode. (Spigot)
            team.getBukkitTeam().removePlayer(pl); // Removes the player from their Spigot team. (Spigot)
            spec.addPlayer(pl); // Assigns the player to the spectator team. (Spigot).
            //DreamUtil.clear(dp);
            //DreamManager.getInstance().giveSpectatorKit(dp);
            onLeave(dp); // Calls onLeave() for the external class.
        }
        //dp.updateDisplayName();
    }

    /**
     * If the player is joining the match, this procedure
     * acts to carry out the player to an assigned team.
     * <p>
     * This procedure assigns the player to a team.
     * <p>
     * //TODO: Add team preference for debug?
     *
     * @param dp   The player to assign a team.
     * @param team The team to assign to a player.
     */
    private void carryOutTeam(WarPlayer dp, WarTeam team) {
        Player pl = dp.getPlayer(); // Assigns Spigot player implementation.
        pl.sendMessage("You have joined the " + team.getTeamColor() + team.getTeamName()); // Alerts player.
        pl.teleport(randomSpawnFrom(teamSpawns.get(team.getTeamName())).toLocation(DreamSequence.getInstance().getCurrentWorld(), true)); // Teleports player to random team spawnpoint. (Spigot)
        pl.setGameMode(GameMode.SURVIVAL); // Sets the player's gamemode to survival. (Spigot)
        dp.setCurrentTeam(team); // Assigns the player's team.
        spec.removePlayer(pl); // Removes the player from the spectator team. (Spigot)
        team.getBukkitTeam().addPlayer(pl); // Assigns the player to the team's Spigot team. (Spigot)
        map().applyInv(dp); // Applies the map's inventory to the player.
        //dp.updateDisplayName();
    }

    /**
     * Searches through all current teams in the match for
     * the team with the least amount of members.
     *
     * @return The team with the least members.
     */
    private WarTeam getSmallestTeam() {
        WarTeam found = null; // The 'result' field.
        int size = -1; // The initial 'highest' size.
        for (WarTeam team : teams.values()) { // Loops through every team.
            if (size == -1) {
                found = team; // Recognises this team as the one with the least amount of members.
                size = team.getBukkitTeam().getPlayers().size(); // Assigns the lowest amount of members to the amount of members in this team.
            } else if (team.getBukkitTeam().getPlayers().size() < size) {
                found = team; // Sets this as the smallest team.
                size = team.getBukkitTeam().getPlayers().size(); // Assigns new smallest team size.
            }
        }
        return found;
    }

    /**
     * Returns how long the map is configured to run for.
     * All maps must end after a certain period of time.
     *
     * @return The duration of the map.
     */
    public Integer getMatchDuration() {
        return (Integer) map().attributes.get("matchDuration");
    }

    /**
     * Randomly picks a value from a list.
     * This is just to randomly return a team spawn.
     *
     * @param array The list.
     * @return The value.
     */
    protected SerializedLocation randomSpawnFrom(List<SerializedLocation> array) {
        Random picker = new Random();
        return array.get(picker.nextInt(array.size()));
    }
}
