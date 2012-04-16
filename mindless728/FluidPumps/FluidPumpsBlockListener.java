package mindless728.FluidPumps;

import org.bukkit.Bukkit;

import org.bukkit.block.Block;

import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;

import org.bukkit.event.block.BlockPlaceEvent;

public class FluidPumpsBlockListener implements Listener  {
	FluidPumps plugin;
	
	public FluidPumpsBlockListener(FluidPumps p) {
		plugin = p;
		Bukkit.getServer().getPluginManager().registerEvents(this, plugin);
	}
	
	@EventHandler(priority = EventPriority.NORMAL)
	public void onBlockPlace(BlockPlaceEvent event) {
		int blockId = event.getBlock().getTypeId();
		
		if(blockId == 35) {
			plugin.addPump(event.getBlock());
		}
	}
}