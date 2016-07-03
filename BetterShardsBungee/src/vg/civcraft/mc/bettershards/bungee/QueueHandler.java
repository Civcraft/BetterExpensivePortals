package vg.civcraft.mc.bettershards.bungee;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

import net.md_5.bungee.api.ChatColor;
import net.md_5.bungee.api.ProxyServer;
import net.md_5.bungee.api.chat.TextComponent;
import net.md_5.bungee.api.connection.ProxiedPlayer;
import vg.civcraft.mc.mercury.MercuryAPI;

public class QueueHandler {

	private static boolean isPrimaryBungee;
	private static Map<String, ArrayList<UUID>> queue = new HashMap<String, ArrayList<UUID>>();
	private static Map<UUID, String> uuidToServerMap = new HashMap<UUID, String>();
	private static Object lockingObject = new Object();
	
	public QueueHandler() {
		isPrimaryBungee = BetterShardsBungee.getInstance().getConfig().getBoolean("primary-bungee");
		ProxyServer.getInstance().getScheduler().schedule(BetterShardsBungee.getInstance(), new Runnable() {

			@Override
			public void run() {
				synchronized(lockingObject) {
					for (String server: queue.keySet()) {
						if (!queue.containsKey(server))
							queue.put(server, new ArrayList<UUID>());
						List<UUID> uuids = queue.get(server);
						int maxSlots = BetterShardsBungee.getServerCount(server);
						int currentSlots = MercuryAPI.getAllAccountsByServer(server).size();
						TextComponent message = new TextComponent("A space is now available, you are being teleported to the server.");
						message.setColor(ChatColor.GREEN);
						for (int x = 0; x < uuids.size() && x < maxSlots - currentSlots; x++) {
							ProxiedPlayer p = ProxyServer.getInstance().getPlayer(uuids.get(x));
							UUID uuid = uuids.get(x);
							uuids.remove(x);
							uuidToServerMap.remove(uuid);
							// The four lines above are important in that position.
							// Without it we get an infinite lock situation.
							if (p != null) {
								p.sendMessage(message);
								p.connect(ProxyServer.getInstance().getServerInfo(server));
								BungeeMercuryManager.playerRemoveQueue(uuids.get(x), server); // We have dealt with the player now.
							} else {
								// may be on other bungee server. Either way lets remove him and get other servers to check.
								BungeeMercuryManager.playerTransferQueue(server, uuids.get(x));
							}
							x--;
							currentSlots++;
						}
						for (int x = 0; x < uuids.size(); x++) {
							System.out.println(uuids.get(x));
							ProxiedPlayer p = ProxyServer.getInstance().getPlayer(uuids.get(x));
							if (p == null)
								continue; // Not on this Mercury server.
							message = new TextComponent("Your current position is " + (x+1) + " in line.");
							message.setColor(ChatColor.GREEN);
							p.sendMessage(message);
						}
					}
				}
			}
			
		}, 10, 10, TimeUnit.SECONDS);
		
		if (isPrimaryBungee) {
			ProxyServer.getInstance().getScheduler().schedule(BetterShardsBungee.getInstance(), new Runnable() {

				@Override
				public void run() {
					synchronized(lockingObject) {
						for (String server: queue.keySet()) {
							if (!queue.containsKey(server))
								queue.put(server, new ArrayList<UUID>());
							BungeeMercuryManager.syncPlayerList(server, queue.get(server));
						}
					}
				}
				
			}, 10, 60, TimeUnit.SECONDS);
		}
	}
	
	public static void initialize() {
		new QueueHandler();
	}
	
	public static ArrayList<UUID> getPlayerOrder(String server) {
		synchronized(lockingObject) {
			if (!queue.containsKey(server))
				queue.put(server, new ArrayList<UUID>());
			return queue.get(server);
		}
	}
	
	/**
	 * This method will determine if this bungee server is the primary one or not.
	 * If it is it will then add the player to the queue and broadcast the position the player should be in.
	 * If not it will request a position and then once received it will be correctly placed.
	 * @param uuid
	 * @param server
	 */
	public static void addPlayerToQueue(UUID uuid, String server) {
		if (isPrimaryBungee) {
			synchronized(lockingObject) {
				if (!queue.containsKey(server))
					queue.put(server, new ArrayList<UUID>());
				if (queue.get(server).contains(uuid)) {
					int pos = queue.get(server).indexOf(uuid);
					BungeeMercuryManager.addPlayerQueue(uuid, server, pos);
				} 
				else {
					queue.get(server).add(uuid);
					int pos = queue.get(server).indexOf(uuid);
					uuidToServerMap.put(uuid, server);
					BungeeMercuryManager.addPlayerQueue(uuid, server, pos);
				}
			}
		}
		else {
			// Need to be assigned a position.
			BungeeMercuryManager.playerRequestQueue(uuid, server);
		}
	}
	
	/**
	 * This should only be called by BungeeMercuryListener
	 * This method should be fired when the main Bungee proxy decides what position the player is in.
	 * @param uuid
	 * @param server
	 * @param pos
	 */
	public static void addPlayerToQueue(UUID uuid, String server, int pos) {
		synchronized(lockingObject) {
			if (!queue.containsKey(server))
				queue.put(server, new ArrayList<UUID>());
			queue.get(server).set(pos, uuid);
			uuidToServerMap.put(uuid, server);
		}
	}
	
	/**
	 * Removes the player from the queue. This method will not broadcast to other bungee servers.
	 * @param uuid
	 * @param server
	 */
	public static void removePlayerQueue(UUID uuid, String server) {
		synchronized(lockingObject) {
			if (!queue.containsKey(server))
				queue.put(server, new ArrayList<UUID>());
			queue.get(server).remove(uuid);
			uuidToServerMap.remove(uuid);
		}
	}
	
	public static String getServerName(UUID uuid) {
		synchronized(lockingObject) {
			return uuidToServerMap.get(uuid);
		}
	}
	
	public static boolean isPrimaryBungee() {
		return isPrimaryBungee;
	}
	
	public static void setServerQueue(String server, ArrayList<UUID> uuids) {
		synchronized(lockingObject) {
			if (!queue.containsKey(server))
				queue.put(server, new ArrayList<UUID>());
			queue.get(server).clear();
			queue.get(server).addAll(uuids);
			for (UUID uuid: uuids)
				uuidToServerMap.put(uuid, server);
		}
	}
}
