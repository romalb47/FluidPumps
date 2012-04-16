package mindless728.FluidPumps;

import org.bukkit.block.Block;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitScheduler;
import org.bukkit.World;

import mindless728.RealFluids.RealFluids;
import mindless728.RealFluids.RealFluidsBlock;

import java.io.File;
import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.FileNotFoundException;
import java.io.IOException;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.Scanner;

public class FluidPumps extends JavaPlugin implements Runnable {
	RealFluids realFluids;
	BukkitScheduler scheduler;
	FluidPumpsBlockListener blockListener;
	HashMap<RealFluidsBlock, Integer> fluidPumpIds;
	String dataFile;

	int repeatRate = 1;
	int pumpId = 35;

	public FluidPumps() {
		realFluids = null;
		fluidPumpIds = new HashMap<RealFluidsBlock, Integer>();
	}

	public void onEnable() {
		Object temp = (RealFluids)getServer().getPluginManager().getPlugin("RealFluids");
		if((temp == null) || !(temp instanceof RealFluids)) {
			System.out.println("RealFluids not found, disabling...");
			getServer().getPluginManager().disablePlugin(this);
			return;
		}
		realFluids = (RealFluids)temp;

		getDataFolder().mkdir();
		dataFile = getDataFolder().getPath()+File.separatorChar+"FluidPumps.txt";
		loadPumps();

		scheduler = this.getServer().getScheduler();
		scheduler.scheduleSyncRepeatingTask(this, this, 20, repeatRate);

		blockListener = new FluidPumpsBlockListener(this);

		System.out.println(getDescription().getName()+" version "+getDescription().getVersion()+" enabled");
	}

	public void onDisable() {
		savePumps();
		System.out.println(getDescription().getName()+" version "+getDescription().getVersion()+" disabled");
	}

	public void loadPumps() {
		Scanner scanner;
		World world;
		int x,y,z,f;
		RealFluidsBlock rfb;

		try {
			scanner = new Scanner(new File(dataFile));
			while(scanner.hasNext()) {
				world = getServer().getWorld(scanner.next());
				x = scanner.nextInt();
				y = scanner.nextInt();
				z = scanner.nextInt();
				System.out.println(world.getName()+"@("+x+","+y+","+z+")");
				f = scanner.nextInt();
				if(world == null)
					continue;
				rfb = realFluids.getBlock(x,y,z,world);
				fluidPumpIds.put(rfb,f);
			}
			scanner.close();
		} catch(FileNotFoundException fnfe) {
			//do nothing, the file doesn't exist
		} catch(Exception e) {
			System.out.println("*** FluidPumps: Error in data file ***");
		}
	}

	public void savePumps() {
		try {
			BufferedWriter writer = new BufferedWriter(new FileWriter(dataFile));
			for(RealFluidsBlock rfb : fluidPumpIds.keySet()) {
				writer.write(rfb.getWorld().getName()+" "+rfb.getX()+" "+rfb.getY()+" "+rfb.getZ()+" "+fluidPumpIds.get(rfb).intValue());
				writer.newLine();
			}
			writer.close();
		} catch(IOException ioe) {System.out.println("*** FluidPumps: Error in saving data file ***");}
	}

	public int getPumpId(RealFluidsBlock rfb) {
		return fluidPumpIds.get(rfb).intValue();
	}

	public void setPumpId(RealFluidsBlock rfb, int fluidId) {
		fluidPumpIds.put(rfb,fluidId);
	}

	public void addPump(Block block) {
		RealFluidsBlock rfb = realFluids.getBlock(block.getLocation());
		fluidPumpIds.put(rfb,0);
	}

	public void waterSource(RealFluidsBlock block) {
		LinkedList<RealFluidsBlock> lrfb = realFluids.getHorizontalAdjacentBlocks(block);
		int rfbId = 0;

		lrfb.addLast(realFluids.getBelowBlock(block));
		if(lrfb.getLast() == null)
			lrfb.removeLast();

		for(RealFluidsBlock rfb : lrfb) {
			rfbId = rfb.getTypeId();
			if(rfbId == 8) {
				rfb.setLevel(realFluids.getWaterStartLevel());
				realFluids.addFlowEvent(rfb);
			} else if(realFluids.isOverwrittable(8, rfbId)) {
				realFluids.overwriteBlock(rfb,8);
				rfb.setLevel(realFluids.getWaterStartLevel());
				realFluids.addFlowEvent(rfb);
			}
		}
	}

	public void lavaSource(RealFluidsBlock block) {
		LinkedList<RealFluidsBlock> lrfb = realFluids.getHorizontalAdjacentBlocks(block);
		int rfbId = 0;

		lrfb.addLast(realFluids.getBelowBlock(block));
		if(lrfb.getLast() == null)
			lrfb.removeLast();

		for(RealFluidsBlock rfb : lrfb) {
			rfbId = rfb.getTypeId();
			if(rfbId == 10) {
				rfb.setLevel(realFluids.getLavaStartLevel());
				realFluids.addFlowEvent(rfb);
			} else if(realFluids.isOverwrittable(9, rfbId)) {
				realFluids.overwriteBlock(rfb,10);
				rfb.setLevel(realFluids.getLavaStartLevel());
				realFluids.addFlowEvent(rfb);
			}
		}
	}

	public void pumpFluids(RealFluidsBlock pump, RealFluidsBlock from, RealFluidsBlock to) {
		//gets some data
		int fromId = from.getTypeId();
		int toId = to.getTypeId();
		int fluidId = getPumpId(pump);

		if(pump.getLocation().getBlock().isBlockPowered()!=true) return;


		//pump fluid out of pump
		if((toId == 0 || toId == fluidId || toId == fluidId+1) && (pump.getLevel() > 0)) {
			to.setLevel(to.getLevel()+pump.getLevel());
			to.setTypeId(fluidId);
			pump.setLevel(0);
			setPumpId(pump,0);
			realFluids.addFlowEvent(to);
		} else if((toId == pumpId) && (fluidId == getPumpId(to) || getPumpId(to) == 0)) {
			to.setLevel(to.getLevel()+pump.getLevel());
			pump.setLevel(0);
			setPumpId(to,fluidId);
			setPumpId(pump,0);
		}

		fluidId = getPumpId(pump);
		fromId -= fromId % 2;

		//pump fluid into pump
		if((fromId == 8 || fromId == 10) && (fluidId == 0 || fluidId == fromId) && (from.getLevel() > 0)) {
			pump.setLevel(pump.getLevel()+from.getLevel());
			setPumpId(pump, fromId);
			from.setLevel(0);
			realFluids.addFlowEvent(from);
		}
	}

	public void pumpNorth(RealFluidsBlock block) {
		pumpFluids(block, realFluids.getSouthBlock(block), realFluids.getNorthBlock(block));
	}

	public void pumpSouth(RealFluidsBlock block) {
		pumpFluids(block, realFluids.getNorthBlock(block), realFluids.getSouthBlock(block));
	}

	public void pumpEast(RealFluidsBlock block) {
		pumpFluids(block, realFluids.getWestBlock(block), realFluids.getEastBlock(block));
	}

	public void pumpWest(RealFluidsBlock block) {
		pumpFluids(block, realFluids.getEastBlock(block), realFluids.getWestBlock(block));
	}

	public void pumpUp(RealFluidsBlock block) {
		pumpFluids(block, realFluids.getBelowBlock(block), realFluids.getAboveBlock(block));
	}

	public void pumpDown(RealFluidsBlock block) {
		pumpFluids(block, realFluids.getAboveBlock(block), realFluids.getBelowBlock(block));
	}

	public void superSponge(RealFluidsBlock block) {
		LinkedList<RealFluidsBlock> adjacentBlocks = realFluids.getAdjacentBlocks(block);
		int id = 0;

		for(RealFluidsBlock b : adjacentBlocks) {
			id = b.getTypeId();
			if(id >= 8 && id <= 11) {
				b.setLevel(0);
				realFluids.addFlowEvent(b);
			}
		}
	}

	public void run() {
		byte data;
		LinkedList<RealFluidsBlock> invalidBlocks = new LinkedList<RealFluidsBlock>();
		for(RealFluidsBlock block : fluidPumpIds.keySet()) {
			if(block.getTypeId() == pumpId) {
				data = block.getLocation().getBlock().getData();/*
				if(data == 11)
					waterSource(block);
				else if(data == 14)
					lavaSource(block);
				else if(data == 1)
					pumpNorth(block);
				else if(data == 13)
					pumpSouth(block);
				else if(data == 4)
					pumpEast(block);
				else if(data == 10)
					pumpWest(block);*/
				if(data == 11)
					pumpUp(block);
				/*else if(data == 7)
					pumpDown(block);
				else if(data == 15)
					superSponge(block);*/
				else {
					invalidBlocks.add(block);
					System.out.println("FluidPump removed because of bad data");
				}
			} else {
				invalidBlocks.add(block);
				System.out.println("FluidPump removed because of bad block id");
			}
		}

		for(RealFluidsBlock rfb : invalidBlocks)
			fluidPumpIds.remove(rfb);
	}
}
