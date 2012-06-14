package me.lukas.skyblockmultiplayer;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.HashMap;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.Player;

public class SQLInstructions {
	private static Connection conn;
	private static Statement stat;

	public static int bool2int(boolean b) {
		if (b)
			return 1;
		return 0;
	}

	public static void initializeConnections() throws ClassNotFoundException {
		try {
			Class.forName("org.sqlite.JDBC");
			SQLInstructions.conn = DriverManager.getConnection("jdbc:sqlite:" + SkyBlockMultiplayer.getInstance().fileSQLite);
			SQLInstructions.stat = conn.createStatement();
		} catch (SQLException e) {
			e.printStackTrace();
		}
	}

	public static void closeConnections() throws SQLException {
		SQLInstructions.stat.close();
		SQLInstructions.conn.close();
	}

	// @formatter:off
	public static void createTables() throws SQLException {

		stat.execute("CREATE TABLE IF NOT EXISTS metadata (" +
						"version integer, "+
						"info varchar);");
	
		stat.execute("CREATE TABLE IF NOT EXISTS players (" +
						"playerName varchar primary key,"+
						"isOnIsland integer,"+
						"isDead integer,"+
						"livesLeft integer,"+
						"islandsLeft integer," +
						"homeLocation varchar);");
		
		stat.execute("CREATE TABLE IF NOT EXISTS islands (" +
					"islandNumber integer primary key AUTOINCREMENT,"+
					"islandLocation varchar,"+
					"x integer,"+
					"z integer,"+
					"playerName varchar);");
		
		stat.execute("CREATE TABLE IF NOT EXISTS oldWorld (" +
						"playerName varchar primary key REFERENCES player(playerName),"+
						"location varchar,"+
						"inventory varchar,"+
						"armor varchar,"+
						"health integer,"+
						"food integer,"+
						"exp real,"+
						"level integer);");
		
		stat.execute("CREATE TABLE IF NOT EXISTS skyblockWorld (" +
						"playerName varchar primary key REFERENCES player(playerName),"+
						"location varchar,"+
						"inventory varchar,"+
						"armor varchar,"+
						"health integer,"+
						"food integer,"+
						"exp real,"+
						"level integer);");
		
		stat.execute("CREATE TABLE IF NOT EXISTS friends (" +
						"playerName varchar REFERENCES player(playerName),"+
						"friendName varchar REFERENCES player(playerName))");
		
		
		
	}
	
	public static boolean writePartialPlayerData(PlayerData pdata){
		try {
			stat.execute("INSERT OR REPLACE INTO players (" +
					"playerName," +
					"isOnIsland," +
					"isDead," +
					"livesLeft," +
					"islandsLeft) VALUES (" +
					"'"+pdata.getPlayerName()+"',"+
					SQLInstructions.bool2int(pdata.isOnIsland())+","+
					SQLInstructions.bool2int(pdata.isDead())+","+
					pdata.getLivesLeft()+","+
					pdata.getIslandsLeft() + ");");
			stat.execute("DELETE from friends WHERE playerName='"+pdata.getPlayerName()+"';");
			for (PlayerData friend : pdata.getFriends().values()){
				stat.execute("INSERT INTO friends (" +
						"playerName,friendName) VALUES (" +
						"'"+pdata.getPlayerName()+"',"+
						"'"+friend.getPlayerName()+"');");
			}
			
			
			return true;
		} catch (SQLException e) {
			e.printStackTrace();
			return false;
		}
	}
	
	public static boolean writeIslandData(PlayerData pdata){
		try {
			stat.execute("INSERT OR REPLACE INTO skyblockWorld (" +
					"playerName,"+
					"location,"+
					"inventory,"+
					"armor,"+
					"health,"+
					"food,"+
					"exp,"+
					"level) VALUES ("+
					"'"+pdata.getPlayerName()+"',"+
					"'"+SkyBlockMultiplayer.getInstance().LocationToString(pdata.getIslandLocation())+"',"+
					"'"+ItemParser.InventoryToString(pdata.getIslandInventory())+"',"+
					"'"+ItemParser.InventoryToString(pdata.getIslandArmor())+"',"+
					pdata.getIslandHealth()+","+
					pdata.getIslandFood()+","+
					pdata.getIslandExp()+","+
					pdata.getIslandLevel()+");");
			
			
			return true;
		} catch (SQLException e) {
			e.printStackTrace();
			return false;
		}
	}
	
	public static boolean writeOldWorldData(PlayerData pdata){
		try {
			stat.execute("INSERT OR REPLACE INTO oldWorld (" +
					"playerName,"+
					"location,"+
					"inventory,"+
					"armor,"+
					"health,"+
					"food,"+
					"exp,"+
					"level) VALUES ("+
					"'"+pdata.getPlayerName()+"',"+
					"'"+SkyBlockMultiplayer.getInstance().LocationToString(pdata.getOldLocation())+"',"+
					"'"+ItemParser.InventoryToString(pdata.getOldInventory())+"',"+
					"'"+ItemParser.InventoryToString(pdata.getOldArmor())+"',"+
					pdata.getOldHealth()+","+
					pdata.getOldFood()+","+
					pdata.getOldExp()+","+
					pdata.getOldLevel()+");");
			
			return true;
		} catch (SQLException e) {
			e.printStackTrace();
			return false;
		}
	}
	
	public static boolean existsPlayer(String playerName){
		ResultSet rs = null;
		try {
			rs = stat.executeQuery("SELECT COUNT(*) FROM players WHERE playerName = '"+playerName+"';");
		rs.next();
		if (rs.getInt(1) == 0) {
			rs.close();
			return false;
		}
		rs.close();
		return true;
		} catch (SQLException e) {
			e.printStackTrace();
			return false;
		}
	}
	
	public static boolean loadAllPlayersPartial(){
		Settings.players = new HashMap<String,PlayerData>();

		try {
			ResultSet rs;
			rs = stat.executeQuery("SELECT players.*,islands.islandNumber,islands.islandLocation FROM players" +
									" JOIN islands ON islands.playerName = players.playerName;");
			while (rs.next()){
				PlayerData pdata = new PlayerData(rs.getString("playerName"));
				pdata.setHasIsland(rs.getBoolean("islandNumber"));
				pdata.setDeathStatus(rs.getBoolean("isDead"));
				pdata.setIslandsLeft(rs.getInt("islandsLeft"));
				pdata.setLivesLeft(rs.getInt("livesLeft"));
				pdata.setHomeLocation(SkyBlockMultiplayer.getInstance().StringToLocation(rs.getString("homeLocation")));
				pdata.setIslandLocation(SkyBlockMultiplayer.getInstance().StringToLocation(rs.getString("islandLocation")));
				Settings.players.put(pdata.getPlayerName(), pdata);
			}
			rs.close();
			return true;
		} catch (SQLException e) {
			e.printStackTrace();
			return false;
		}
	}
	
	public static boolean loadPartialPlayerData(PlayerData pdata){
		ResultSet rs;
		try {
			rs = stat.executeQuery("SELECT * FROM players" +
									" JOIN islands ON islands.playerName = players.playerName" +
									" WHERE players.playerName = '"+pdata.getPlayerName()+"';");
		if (rs.next() == false) {
			rs.close();
			return true;
		}

		pdata.setHasIsland(rs.getBoolean("islandNumber"));
		pdata.setDeathStatus(rs.getBoolean("isDead"));
		pdata.setIslandsLeft(rs.getInt("islandsLeft"));
		pdata.setLivesLeft(rs.getInt("livesLeft"));
		pdata.setHomeLocation(SkyBlockMultiplayer.getInstance().StringToLocation(rs.getString("homeLocation")));
		
		rs.close();
		return true;
		} catch (SQLException e) {
			e.printStackTrace();
			return false;
		}
	}
	
	public static boolean loadOldWorldData(PlayerData pdata){
		ResultSet rs;	
		try {
			rs = stat.executeQuery("SELECT * FROM oldWorld " +
					"WHERE playerName = '"+pdata.getPlayerName()+"';");
		if (rs.next() == false) {
			rs.close();
			return true;
		}

		pdata.setOldLocation(SkyBlockMultiplayer.getInstance().StringToLocation(rs.getString("location")));
		pdata.setOldInventory(ItemParser.StringToInventory(rs.getString("inventory"), 36));
		pdata.setOldArmor(ItemParser.StringToInventory(rs.getString("armor"), 4));
		pdata.setOldHealth(rs.getInt("health"));
		pdata.setOldFood(rs.getInt("food"));
		pdata.setOldExp(rs.getFloat("exp"));
		pdata.setOldLevel(rs.getInt("level"));
		
		rs.close();		
		return true;
		} catch (SQLException e) {
			e.printStackTrace();
			return false;
		}
	}
	
	public static boolean loadIslandData(PlayerData pdata){
		ResultSet rs;	
		try {
			rs = stat.executeQuery("SELECT * FROM skyblockWorld " +
					"WHERE playerName = '"+pdata.getPlayerName()+"';");
		if (rs.next() == false)
			return true;

		pdata.setIslandLocation(SkyBlockMultiplayer.getInstance().StringToLocation(rs.getString("location")));
		pdata.setIslandInventory(ItemParser.StringToInventory(rs.getString("inventory"), 36));
		pdata.setIslandArmor(ItemParser.StringToInventory(rs.getString("armor"), 4));
		pdata.setIslandHealth(rs.getInt("health"));
		pdata.setIslandFood(rs.getInt("food"));
		pdata.setIslandExp(rs.getFloat("exp"));
		pdata.setIslandLevel(rs.getInt("level"));
		
		rs.close();
		return true;
		} catch (SQLException e) {
			e.printStackTrace();
			return false;
		}
	}

	public static boolean writeNewIsland(PlayerData pdata, CreateNewIsland island){
		try {			
			stat.execute("INSERT OR REPLACE INTO islands (" +
					"islandLocation," +
					"x," +
					"z,"+
					"playerName) VALUES ("+
					"'"+SkyBlockMultiplayer.getInstance().LocationToString(island.Islandlocation)+"',"+
					island.Islandlocation.getBlockX()+","+
					island.Islandlocation.getBlockZ()+","+
					"'"+pdata.getPlayerName()+"');");
			

			return true;
		} catch (SQLException e) {
			e.printStackTrace();
			return false;
		} 
	}
	
	public static String getOwner(Location click, int distance){
		int xmin = click.getBlockX()-distance;
		int xmax = click.getBlockX()+distance;
		int zmin = click.getBlockZ()-distance;
		int zmax = click.getBlockZ()+distance;
		
		try {
			ResultSet rs = stat.executeQuery("SELECT playerName from islands" +
												" WHERE x >= "+xmin+
												" AND x <= "+xmax+
												" AND z >= "+zmin+
												" AND z <= "+zmax+";");
			if (!rs.next()) {
				rs.close();
				return null;
			}
			String res = rs.getString("playerName");
			rs.close();
			return res;
			
		} catch (SQLException e) {
			e.printStackTrace();
			return null;
		}
	}
	
	public static boolean loadFriendList(PlayerData pdata){
		try {
		ResultSet rs;	
			rs = stat.executeQuery("SELECT friendname FROM friends " +
						"WHERE playerName = '"+pdata.getPlayerName()+"';");
		
			while(rs.next()){
				if (!Settings.players.containsKey(rs.getString("friendName")))
					continue;
				PlayerData friend = Settings.players.get(rs.getString("friendName"));
				pdata.addFriendsToOwnIsland(friend);
				friend.addOwnBuildPermission(pdata);
			}
			rs.close();
			return true;
		} catch (SQLException e) {
			e.printStackTrace();
			return false;
		}
	}
	
	// @formatter:on

	public static PlayerData loadOrCreatePlayer(String playername) {
		boolean inList = Settings.players.containsKey(playername);
		boolean inDB = SQLInstructions.existsPlayer(playername);
		if (inList && inDB) {
			return Settings.players.get(playername);
		}
		if (!inList && !inDB) {
			// player exists nowhere, thus create
			PlayerData pdata = new PlayerData(playername);
			// check if a playerfile exists
			PlayerInfo pi = SkyBlockMultiplayer.getInstance().readPlayerFile(playername);
			Player player = Bukkit.getPlayer(playername);
			if (pi != null && player != null) { // load existing playerinfo only if player is online
				pdata.setDeathStatus(pi.isDead());
				pdata.setHasIsland(pi.getHasIsland());
				pdata.setHomeLocation(pi.getHomeLocation());
				pdata.setIslandsLeft(pi.getIslandsLeft());
				pdata.setLivesLeft(pi.getLivesLeft());

				// check isOnIslandVariable
				if (player.getWorld().getName().equals(SkyBlockMultiplayer.getSkyBlockWorld().getName()) && !SkyBlockMultiplayer.getInstance().playerIsOnTower(player)) {
					pdata.setIsOnIslandStatus(true);
				} else {
					pdata.setIsOnIslandStatus(false);
				}

				pdata.setIslandArmor(pi.getIslandArmor());
				pdata.setIslandExp(pi.getIslandExp());
				pdata.setIslandFood(pi.getIslandFood());
				pdata.setIslandHealth(pi.getIslandHealth());
				pdata.setIslandInventory(pi.getIslandInventory());
				pdata.setIslandLevel(pi.getIslandLevel());
				pdata.setIslandLocation(pi.getIslandLocation());

				pdata.setOldArmor(pi.getOldArmor());
				pdata.setOldExp(pi.getOldExp());
				pdata.setOldFood(pi.getOldFood());
				pdata.setOldHealth(pi.getOldHealth());
				pdata.setOldInventory(pi.getOldInventory());
				pdata.setOldLevel(pi.getOldLevel());
				pdata.setOldLocation(pi.getOldLocation());

			}
			if (player != null) {
				SQLInstructions.writePartialPlayerData(pdata);
				Settings.players.put(playername, pdata);
			}

			return pdata;
		}
		if (!inList) { // player is missing in list but exists in DB
			PlayerData pdata = new PlayerData(playername);
			SQLInstructions.loadPartialPlayerData(pdata);
			SQLInstructions.loadOldWorldData(pdata);
			SQLInstructions.loadIslandData(pdata);
			SQLInstructions.loadFriendList(pdata);
			Settings.players.put(playername, pdata);
			return pdata;
		}
		// player exists in list, but is missing in DB
		PlayerData pdata = Settings.players.get(playername);
		SQLInstructions.writePartialPlayerData(pdata);
		SQLInstructions.writeOldWorldData(pdata);
		SQLInstructions.writeIslandData(pdata);
		return pdata;
	}
}
