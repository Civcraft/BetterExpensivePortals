package vg.civcraft.mc.bettershards.command.commands;

import java.util.List;

import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import net.md_5.bungee.api.ChatColor;
import vg.civcraft.mc.civmodcore.command.PlayerCommand;
import vg.civcraft.mc.mercury.MercuryAPI;
import vg.civcraft.mc.mercury.PlayerDetails;

public class GetShard extends PlayerCommand {

	public GetShard(String name) {
		super(name);
		setIdentifier("shard");
		setDescription("Gets the name of the shard you're in");
		setUsage("/shard <name>");
		setArguments(0, 1);
	}
	
	@Override
	public boolean execute(CommandSender sender, String[] args) {
		if(args.length == 0) {
			if(!(sender instanceof Player)) {
				sender.sendMessage(ChatColor.RED + "Non-players need to specify a player to look up");
				return true;
			}
			Player player = (Player) sender;
			PlayerDetails details = MercuryAPI.getServerforAccount(player.getUniqueId());
			player.sendMessage(ChatColor.GREEN + "[BetterShards]You are currently in the shard '" + details.getServerName() + "'");
			return true;
		} else {
			if(!sender.isOp()) {
				sender.sendMessage(ChatColor.RED + "You cannot look up the shard of another player!");
				return true;
			}
			PlayerDetails details = MercuryAPI.getServerforPlayer(args[0]);
			if(details == null) {
				sender.sendMessage(ChatColor.RED + "Player not found!");
				return true;
			} else {
				sender.sendMessage(ChatColor.GREEN + args[0] + " is on the shard '" + details.getServerName() + "'");
				return true;
			}
		}
	}

	@Override
	public List<String> tabComplete(CommandSender arg0, String[] arg1) {
		return null;
	}
}
