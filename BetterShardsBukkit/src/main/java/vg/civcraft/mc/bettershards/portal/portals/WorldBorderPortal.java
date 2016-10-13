package vg.civcraft.mc.bettershards.portal.portals;

import java.util.ArrayList;
import java.util.List;

import org.bukkit.Effect;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;

import vg.civcraft.mc.bettershards.BetterShardsAPI;
import vg.civcraft.mc.bettershards.BetterShardsPlugin;
import vg.civcraft.mc.bettershards.events.PlayerChangeServerReason;
import vg.civcraft.mc.bettershards.manager.RandomSpawnManager;
import vg.civcraft.mc.bettershards.misc.LocationWrapper;
import vg.civcraft.mc.bettershards.misc.PlayerStillDeadException;
import vg.civcraft.mc.bettershards.portal.Portal;

public class WorldBorderPortal extends Portal {

	private Location mapCenter;
	private LocationWrapper first;
	private LocationWrapper second;
	private double wbRange;
	private double fAngle;
	private double sAngle;
	private double arcLength;
	private double particleIncrement;
	private static List <Material> ignoreMaterials;
	
	private static int id = -1;

	/**
	 * So without complication everything needlessly, note that all border begin/ends
	 * should be listed in clockwise order. If you fail to adhere to this, the border
	 * will instead be everything you meant to be outside the border.
	 */
	public WorldBorderPortal() {
		
	}
	
	public LocationWrapper getFirst() {
		return first;
	}
	
	public LocationWrapper getSecond() {
		return second;
	}

	public boolean inPortal(Location loc) {
		return getXZDistance(loc) > wbRange && 
				getArcPosition(loc) >= 0.0;
	}

	private double getXZDistance(Location loc) {
		double x = loc.getX() - mapCenter.getX();
		double z = loc.getZ() - mapCenter.getZ();
		return Math.sqrt(x * x + z * z);
	}

	/**
	 * Strictly returns an angle in radians between -PI and PI.
	 */
	private double getAdjustedAngle(Location loc) {
		double x = loc.getX() - mapCenter.getX();
		double z = loc.getZ() - mapCenter.getZ();
		return Math.atan2(z,x);
	}
	
	/**
	 * Just returns a spawn location somewhere along the WB arc
	 */
	public Location findSpawnLocation() {
		return calculateSpawnLocation(Math.random());
	}
	
	/**
	 * For a given location, gives a value 0..1 if the location is within the arc
	 * described by this world border portal. Otherwise returns -1.0
	 */
	public double getArcPosition(Location loc) {
		double locAngle = getAdjustedAngle(loc);
		if (fAngle == sAngle) {
			return (Math.PI + locAngle) / arcLength;
		} else if ((fAngle > sAngle && (locAngle >= fAngle || locAngle <= sAngle)) || 
				(fAngle < sAngle && locAngle >= fAngle && locAngle <= sAngle)) {
			if (fAngle > sAngle && locAngle <= sAngle) {
				locAngle += 2.0*Math.PI;
			}
			return (locAngle - fAngle) / arcLength;
		}
			
		return -1.0;
	}
	
	/**
	 * Takes an arc position %, and computes a Location that falls within the arc that
	 * percentage along the arc.
	 * If a value is received > 1.0 or < 0.0, null is returned. Y for the returned location will be 0
	 */
	
	public Location convertArcPositionToLocation(double arcPosition) {
	    return convertArcPositionToLocation(arcPosition, 0);
	}
	
	
	public Location convertArcPositionToLocation(double arcPosition, int y) {
	    if (arcPosition > 1.0 || arcPosition < 0.0) return null;
		double theta = fAngle + arcLength * arcPosition;
		int x = (int) ((wbRange-2.0) * Math.cos(theta));
		int z = (int) ((wbRange-2.0) * Math.sin(theta));
		// TODO strengthen this
		return new Location(mapCenter.getWorld(), x, y , z);
	}
	
	public Location calculateSpawnLocation(double arcPosition) {
		Location loc = convertArcPositionToLocation(arcPosition);
		if (loc == null) {
		    return null;
		}
		int x = loc.getBlockX();
		int z = loc.getBlockZ();
		Block upper = mapCenter.getWorld().getHighestBlockAt(x, z);
		Block eyes = upper.getRelative(0,1,0);
		World w = eyes.getWorld();
		if (eyes.getY() >= 254 || eyes.getY() <= 1) {
		    for(int y = 254; y > 0; y--) {
				if (w.getBlockAt(x, y, z).getType().isSolid() && !ignoreMaterials.contains(w.getBlockAt(x, y, z).getType())
					&& w.getBlockAt(x, y + 1, z).getType() == Material.AIR
					&& w.getBlockAt(x, y + 2, z).getType() == Material.AIR) {
					return RandomSpawnManager.centerLocation(new Location(w, x, y + 1, z)); //+1 because player position is in lower body half
				}
		    }
		  //no valid spot at all, so create one
		    BetterShardsPlugin.getInstance().warning("Found no valid portal spawning spot  at x=" + loc.getBlockX() 
			    + " z=" + loc.getBlockZ() + ". Creating one instead");
		    if (!w.getBlockAt(x, 0, z).getType().isSolid()) {
		    	w.getBlockAt(x, 0, z).setType(Material.STONE);
		    }
		    w.getBlockAt(x, 1, z).setType(Material.AIR);
		    w.getBlockAt(x, 2, z).setType(Material.AIR);
		    return RandomSpawnManager.centerLocation(new Location(w, x, 1, z));
		}
		return RandomSpawnManager.centerLocation(eyes.getLocation());
	}
	
	public void teleport(Player p) {
		if (connection == null)
			return;
		Portal portal = BetterShardsPlugin.getPortalManager().getPortal(connection);
		Double relativeArcPosition = getArcPosition(p.getLocation());
		if (portal.getServerName().equals(BetterShardsAPI.getServerName())) {
			p.teleport(((WorldBorderPortal)portal).calculateSpawnLocation(relativeArcPosition));
			return;
		}
		try {
			BetterShardsAPI.connectPlayer(p, portal,
					PlayerChangeServerReason.PORTAL, relativeArcPosition);
		} catch (PlayerStillDeadException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	@Override
	public void showParticles(Player p) {
	    Location loc = p.getLocation();
	    if (getXZDistance(loc) >= wbRange - PARTICLE_SIGHT_RANGE && getArcPosition(loc) >= 0.0) {
		double angle = getArcPosition(loc) - (PARTICLE_RANGE * particleIncrement); 
		for(int i = 0;i <= PARTICLE_RANGE * 2 + 1; i++) {
		    if (angle < 0.0) {
			angle += particleIncrement;
			continue;
		    }
		    if (angle > 1.0) {
			break;
		    }
		    for(int y = loc.getBlockY() - PARTICLE_RANGE; y <= loc.getBlockY() + PARTICLE_RANGE; y++) {
			Location particeLoc = convertArcPositionToLocation(angle, y);
			if (particeLoc == null) {
			    continue;
			}
			p.spigot().playEffect(particeLoc, Effect.FLYING_GLYPH, 0, 0, 0, 0, 0, 1, 3, PARTICLE_SIGHT_RANGE);
		    }
		    angle += particleIncrement;
		}
	    }
	    
	}

	@Override
	public String getTypeName() {
		return "WorldBorder";
	}

	@Override
	public void valuesPopulated() {
		this.mapCenter = new Location(first.getFakeLocation().getWorld(), 0, 0, 0);

		double fRadius = getXZDistance(first.getFakeLocation());
		double sRadius = getXZDistance(second.getFakeLocation());
		this.wbRange = Math.min(fRadius, sRadius);

		this.fAngle = getAdjustedAngle(first.getFakeLocation());
		this.sAngle = getAdjustedAngle(second.getFakeLocation());

		this.arcLength = (fAngle == sAngle) ? 2 * Math.PI : 
				(fAngle > sAngle) ? 2 * Math.PI - fAngle + sAngle :
				sAngle - fAngle;
		//(circumference  in blocks) * (percentage of circumference the portal takes up)
		//= (wbRange * 2 * PI) * (arcLength / (PI * 2))
		//= wbRange * arcLength
		double blocksInPortal = arcLength * wbRange;
		this.particleIncrement = 1.0 / blocksInPortal;
		List <String> ignoreMats = BetterShardsPlugin.getInstance().GetConfig().get("randomspawn.ignoreMaterials").getStringList();
		if (ignoreMaterials == null) {
			ignoreMaterials = new ArrayList<Material>();
			for(String ign : ignoreMats) {
			    try {
					Material m = Material.valueOf(ign);
					BetterShardsPlugin.getInstance().info("Ignoring " + m.toString() + " for portal spawning");
					ignoreMaterials.add(m);
			    }
			    catch (IllegalArgumentException e) {
			    	BetterShardsPlugin.getInstance().warning("The portal spawn ignore material specified as " + ign + " is not valid. It was ignored");
			    }
			}
		}
	}

	@Override
	public int getPortalID() {
		if (id == -1) {
			id = BetterShardsPlugin.getDatabaseManager().getPortalID(BetterShardsPlugin.getInstance().getName(), 1);
		}
		return id;
	}
}
