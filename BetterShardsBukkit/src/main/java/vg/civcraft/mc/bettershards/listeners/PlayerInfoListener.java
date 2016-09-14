package vg.civcraft.mc.bettershards.listeners;

import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;

import com.comphenix.protocol.PacketType;
import com.comphenix.protocol.ProtocolLibrary;
import com.comphenix.protocol.events.ListenerPriority;
import com.comphenix.protocol.events.PacketAdapter;
import com.comphenix.protocol.events.PacketContainer;
import com.comphenix.protocol.events.PacketEvent;

import vg.civcraft.mc.bettershards.BetterShardsPlugin;
import vg.civcraft.mc.mercury.MercuryAPI;
import vg.civcraft.mc.mercury.events.AsyncPluginBroadcastMessageEvent;

public class PlayerInfoListener extends PacketAdapter implements Listener {

	public PlayerInfoListener(BetterShardsPlugin plugin) {
		super(plugin, ListenerPriority.NORMAL, PacketType.Play.Server.PLAYER_INFO);
		ProtocolLibrary.getProtocolManager().addPacketListener(this);
	}
	
	@Override
	public void onPacketSending(PacketEvent event) {
		boolean online = event.getPacket().getSpecificModifier(boolean.class).read(0);
		if(!online && MercuryAPI.getAllPlayers().contains(event.getPacket().getStrings().read(0))) {
			event.setCancelled(true);
		}
	}
	
	@EventHandler
	public void onAsyncPluginBroadcastMessage(AsyncPluginBroadcastMessageEvent event) {
		String channel = event.getChannel();
		if(!channel.equals("BetterShards")) {
			return;
		}
		String message = event.getMessage();
		final String[] content = message.split("\\|");
		if(content[0].equals("playerinfo")) {
			String player = content[2];
			boolean on = content[1].equals("on");
			sendPlayerInfoPacket(player, on);
		}
	}
	
	private void sendPlayerInfoPacket(String player, boolean online) {
		PacketContainer packet = ProtocolLibrary.getProtocolManager().createPacket(PacketType.Play.Server.PLAYER_INFO);
		packet.getModifier().writeDefaults();
		packet.getStrings().write(0, player);
		packet.getSpecificModifier(boolean.class).write(0, online);
		ProtocolLibrary.getProtocolManager().broadcastServerPacket(packet);
	}
	
	@EventHandler
	public void onPlayerJoin(PlayerJoinEvent event) {
		MercuryAPI.sendGlobalMessage("playerinfo|on|" + event.getPlayer().getName(), "BetterShards");
	}
	
	@EventHandler
	public void onPlayerLeave(PlayerQuitEvent event) {
		MercuryAPI.sendGlobalMessage("playerinfo|off|" + event.getPlayer().getName(), "BetterShards");
	}
}