package mods.eln.TreeResinCollector;

import java.io.DataOutputStream;
import java.io.IOException;

import net.minecraft.block.Block;
import net.minecraft.block.BlockLeavesBase;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.Blocks;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.world.World;
import mods.eln.Eln;
import mods.eln.misc.Coordonate;
import mods.eln.misc.Direction;
import mods.eln.misc.LRDU;
import mods.eln.misc.Utils;

import mods.eln.node.NodeBase;
import mods.eln.node.NodeManager;
import mods.eln.node.NodePeriodicPublishProcess;
import mods.eln.node.SixNode;
import mods.eln.node.SixNodeDescriptor;
import mods.eln.node.SixNodeElement;
import mods.eln.sim.ElectricalLoad;
import mods.eln.sim.IProcess;
import mods.eln.sim.ThermalLoad;

public class TreeResinCollectorElement extends SixNodeElement{

	TreeResinCollectorSlowProcess slowProcess = new TreeResinCollectorSlowProcess(this);
	
	public TreeResinCollectorElement(SixNode sixNode, Direction side,
			SixNodeDescriptor descriptor) {
		super(sixNode, side, descriptor);
		slowProcessList.add(slowProcess);
		slowProcessList.add(new NodePeriodicPublishProcess(sixNode, 4f, 4f));
	}

	@Override
	public ElectricalLoad getElectricalLoad(LRDU lrdu) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public ThermalLoad getThermalLoad(LRDU lrdu) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public int getConnectionMask(LRDU lrdu) {
		// TODO Auto-generated method stub
		return 0;
	}

	@Override
	public String multiMeterString() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public String thermoMeterString() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public void initialize() {
		// TODO Auto-generated method stub
		
	}

	final float occupancyMax = 2f;
	final float occupancyProductPerSecondPerTreeBlock = 3f/5f/(60f*24f); //3 par jour, pour 5 tronc de haut
	
	@Override
	public boolean onBlockActivated(EntityPlayer entityPlayer, Direction side,
			float vx, float vy, float vz) {
		
		double occupancy;
		Coordonate coord = sixNode.coordonate;
		World worldObj = coord.world();
		int[] posWood = new int[3];
		int[] posCollector = new int[3];
		Direction woodDirection = side;
		posWood[0] = coord.x;posWood[1] = coord.y;posWood[2] = coord.z;
		posCollector[0] = coord.x;posCollector[1] = coord.y;posCollector[2] = coord.z;
		woodDirection.applyTo(posWood, 1);
		int leafCount = 0;
		int yStart,yEnd;
		
		while(worldObj.getBlock(posWood[0],posWood[1]-1,posWood[2]) == Blocks.log || worldObj.getBlock(posWood[0],posWood[1]-1,posWood[2]) == Blocks.log2)
		{
			posWood[1]--;
		}
		yStart = posWood[1];
		
		posWood[1] = coord.y;
		//timeCounter-= timeTarget;
		while(worldObj.getBlock(posWood[0],posWood[1]+1,posWood[2]) == Blocks.log || worldObj.getBlock(posWood[0],posWood[1]+1,posWood[2]) == Blocks.log2)
		{
			if(worldObj.getBlock(posCollector[0],posWood[1]+1,posCollector[2]) instanceof BlockLeavesBase) leafCount++;
			posWood[1]++;
		}
		yEnd = posWood[1];
		
		int collectiorCount = 0;
		Coordonate coordTemp = new Coordonate(posCollector[0],0,posCollector[2],worldObj);
		posCollector[1] = yStart;
		for(posCollector[1] = yStart;posCollector[1] <= yEnd;posCollector[1]++)
		{
			coordTemp.y = posCollector[1];
			//if(worldObj.getBlockId(posCollector[0],posCollector[1]+1,posCollector[2]) == Eln.treeResinCollectorBlock.blockID)
			NodeBase node = NodeManager.instance.getNodeFromCoordonate(coordTemp);
			if(node instanceof SixNode)
			{
				SixNode six = (SixNode) node;
				if(six.getElement(side) != null && six.getElement(side) instanceof TreeResinCollectorElement)
				{
					collectiorCount++;
				}
			}
		}
		if(collectiorCount == 0)
		{
			collectiorCount++;
			Utils.println("ASSERT collectiorCount == 0");
		}
		double leaf = leafCount >= 1 ? 1 : 0.000000001;
		double productPerSeconde = Math.min(0.05,occupancyProductPerSecondPerTreeBlock * (yEnd - yStart + 1) / collectiorCount)*leaf;
		double product = productPerSeconde * timeFromLastActivated;
		int productI = 0;
		if(product > occupancyMax){
			productI = (int) occupancyMax;
			timeFromLastActivated = 0;
		}
		else
		{
			productI = (int) product;			
			timeFromLastActivated -= (productI) / productPerSeconde; 	

		}
	
		for(int idx = 0;idx < productI;idx++)
		{
			sixNode.dropItem(Eln.treeResin.newItemStack(1));
		}
		
		Utils.addChatMessage(entityPlayer,"Tree Resin in pot : " + String.format("%1.2f",productPerSeconde * timeFromLastActivated));
		needPublish();
		return true;
	}

	
	
	
	double timeFromLastActivated = 0;
	@Override
	public void readFromNBT(NBTTagCompound nbt) {
		// TODO Auto-generated method stub
		super.readFromNBT(nbt);
		timeFromLastActivated = nbt.getDouble( "timeFromLastActivated");
	}
	
	@Override
	public void writeToNBT(NBTTagCompound nbt) {
		// TODO Auto-generated method stub
		super.writeToNBT(nbt);
		nbt.setDouble( "timeFromLastActivated", timeFromLastActivated);
	}
	
	class TreeResinCollectorSlowProcess implements IProcess
	{
		TreeResinCollectorElement element;
		public TreeResinCollectorSlowProcess(TreeResinCollectorElement element) {
			this.element = element;
		}

		@Override
		public void process(double time) {
			// TODO Auto-generated method stub
			element.timeFromLastActivated += time;
		}
		
	}
	
	
	
	@Override
	public void networkSerialize(DataOutputStream stream) {
		// TODO Auto-generated method stub
		super.networkSerialize(stream);
		double occupancy;
		Coordonate coord = sixNode.coordonate;
		World worldObj = coord.world();
		int[] posWood = new int[3];
		int[] posCollector = new int[3];
		Direction woodDirection = side;
		posWood[0] = coord.x;posWood[1] = coord.y;posWood[2] = coord.z;
		posCollector[0] = coord.x;posCollector[1] = coord.y;posCollector[2] = coord.z;
		woodDirection.applyTo(posWood, 1);
		
		int yStart,yEnd;
		
		while(worldObj.getBlock(posWood[0],posWood[1]-1,posWood[2]) == Blocks.log)
		{
			posWood[1]--;
		}
		yStart = posWood[1];
		
		posWood[1] = coord.y;
		//timeCounter-= timeTarget;
		while(worldObj.getBlock(posWood[0],posWood[1]+1,posWood[2]) == Blocks.log)
		{
			posWood[1]++;
		}
		yEnd = posWood[1];
		
		int collectiorCount = 0;
		Coordonate coordTemp = new Coordonate(posCollector[0],0,posCollector[2],worldObj);
		posCollector[1] = yStart;
		for(posCollector[1] = yStart;posCollector[1] <= yEnd;posCollector[1]++)
		{
			coordTemp.y = posCollector[1];
			//if(worldObj.getBlockId(posCollector[0],posCollector[1]+1,posCollector[2]) == Eln.treeResinCollectorBlock.blockID)
			NodeBase node = NodeManager.instance.getNodeFromCoordonate(coordTemp);
			if(node instanceof SixNode)
			{
				SixNode six = (SixNode) node;
				if(six.getElement(side) != null && six.getElement(side) instanceof TreeResinCollectorElement)
				{
					collectiorCount++;
				}
			}
		}
		if(collectiorCount == 0)
		{
			collectiorCount++;
			Utils.println("ASSERT collectiorCount == 0");
		}
		double productPerSeconde = Math.min(0.05,occupancyProductPerSecondPerTreeBlock * (yEnd - yStart + 1) / collectiorCount);
		double product = productPerSeconde * timeFromLastActivated;
		
		try {
			stream.writeFloat((float) product);
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
}
