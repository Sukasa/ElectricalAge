package mods.eln.heatfurnace;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;

import mods.eln.Eln;
import mods.eln.item.regulator.IRegulatorDescriptor;
import mods.eln.misc.Direction;
import mods.eln.misc.LRDU;
import mods.eln.misc.Utils;
import mods.eln.node.NodeBase;
import mods.eln.node.NodeElectricalGateInput;
import mods.eln.node.NodeElectricalLoad;
import mods.eln.node.NodeFurnaceProcess;
import mods.eln.node.NodePeriodicPublishProcess;
import mods.eln.node.NodeThermalLoad;
import mods.eln.node.TransparentNode;
import mods.eln.node.TransparentNodeDescriptor;
import mods.eln.node.TransparentNodeElement;
import mods.eln.node.TransparentNodeElementInventory;
import mods.eln.sim.ElectricalLoad;
import mods.eln.sim.RegulatorFurnaceProcess;
import mods.eln.sim.ThermalLoad;
import mods.eln.sim.ThermalWatchdogProcessForInventory;
import mods.eln.sim.ThermalWatchdogProcessForInventoryItemDamageSingleLoad;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.inventory.Container;
import net.minecraft.inventory.IInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;

/* 5s/item @ 500 C
 * s 
 * 
 */


public class HeatFurnaceElement extends TransparentNodeElement{
	
	public NodeElectricalGateInput electricalCmdLoad = new NodeElectricalGateInput("electricalCmdLoad",true);
	public NodeThermalLoad thermalLoad = new NodeThermalLoad("thermalLoad");
	public NodeFurnaceProcess furnaceProcess = new NodeFurnaceProcess("furnaceProcess",thermalLoad);
	public HeatFurnaceInventoryProcess inventoryProcess = new HeatFurnaceInventoryProcess(this);
	
	TransparentNodeElementInventory inventory = new TransparentNodeElementInventory(4, 64, this);

	HeatFurnaceThermalProcess regulator = new HeatFurnaceThermalProcess("regulator",furnaceProcess,this);
	
	HeatFurnaceDescriptor descriptor;
	
	ThermalWatchdogProcessForInventoryItemDamageSingleLoad isolatorWatchdog = new ThermalWatchdogProcessForInventoryItemDamageSingleLoad(inventory,HeatFurnaceContainer.isolatorId,thermalLoad);
	
	public HeatFurnaceElement(TransparentNode transparentNode,TransparentNodeDescriptor descriptor) {
		super(transparentNode,descriptor);
		this.descriptor = (HeatFurnaceDescriptor) descriptor;
		
		furnaceProcess.setGainMin(0.1);
		
		thermalLoadList.add(thermalLoad);
		thermalProcessList.add(furnaceProcess);
		slowProcessList.add(inventoryProcess);
		thermalProcessList.add(regulator);
		thermalProcessList.add(isolatorWatchdog);
		electricalLoadList.add(electricalCmdLoad);
		slowProcessList.add(new NodePeriodicPublishProcess(transparentNode, 2.0, 1.0));
	}

	@Override
	public ElectricalLoad getElectricalLoad(Direction side, LRDU lrdu) {
		// TODO Auto-generated method stub
		return electricalCmdLoad;
	}

	@Override
	public ThermalLoad getThermalLoad(Direction side, LRDU lrdu) {
		if(side == front.getInverse() && lrdu == LRDU.Down) return thermalLoad;
		return null;
	}

	@Override
	public int getConnectionMask(Direction side, LRDU lrdu) {
		// TODO Auto-generated method stub
		if((side == front.left() || side == front.right()) && lrdu == LRDU.Down)  return NodeBase.maskElectricalInputGate;
		if(side == front.getInverse() && lrdu == LRDU.Down)	return NodeBase.maskThermal;
		return 0;
	}

	@Override
	public String multiMeterString(Direction side) {
		// TODO Auto-generated method stub
		return "";
	}

	@Override
	public String thermoMeterString(Direction side) {
		// TODO Auto-generated method stub
		return Utils.plotCelsius("T:", thermalLoad.Tc);
	}

	@Override
	public void initialize() {
		descriptor.applyTo(thermalLoad);
    	descriptor.applyTo(furnaceProcess);
    	computeInventory();

    	connect();
    	
    	
    	inventoryProcess.process(1/20.0);
	}

	@Override
	public boolean onBlockActivated(EntityPlayer entityPlayer, Direction side,
			float vx, float vy, float vz) {
		// TODO Auto-generated method stub
		return false;
	}
	
	


	
	@Override
	public void networkSerialize(DataOutputStream stream) {
		// TODO Auto-generated method stub
		super.networkSerialize(stream);
		try {
			stream.writeBoolean(getControlExternal());
			stream.writeBoolean(getTakeFuel());
			stream.writeShort((short) (thermalLoad.Tc*NodeBase.networkSerializeTFactor));
			stream.writeFloat((float) furnaceProcess.getGain());
			stream.writeFloat((float)regulator.getTarget());
			stream.writeShort((int) furnaceProcess.getP());

			serialiseItemStack(stream,inventory.getStackInSlot(HeatFurnaceContainer.combustibleId));

		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	
	@Override
	public boolean hasGui() {
		// TODO Auto-generated method stub
		return true;
	}
	
	@Override
	public Container newContainer(Direction side, EntityPlayer player) {
		// TODO Auto-generated method stub
		return new HeatFurnaceContainer(node,player, inventory,descriptor);
	}
	
	@Override
	public IInventory getInventory() {
		// TODO Auto-generated method stub
		return inventory;
	}

	public static final byte unserializeGain = 1;
	public static final byte unserializeTemperatureTarget = 2;
	public static final byte unserializeToogleControlExternalId = 3;
	public static final byte unserializeToogleTakeFuelId = 4;
	@Override
	public byte networkUnserialize(DataInputStream stream) {
		byte packetType = super.networkUnserialize(stream);
		try {
			switch(packetType)
			{
			case unserializeGain:
				if(inventory.getStackInSlot(HeatFurnaceContainer.regulatorId) == null)
				{
					furnaceProcess.setGain(stream.readFloat());				
				}
				needPublish();
				break;
			case unserializeTemperatureTarget:
				//if(inventory.getStackInSlot(HeatFurnaceContainer.regulatorId) == null)
				{
					regulator.setTarget(stream.readFloat());				
				}
				needPublish();
				break;
			case unserializeToogleControlExternalId:
				regulator.setTarget(0);
				setControlExternal(! getControlExternal());			
				break;
			case unserializeToogleTakeFuelId:
				setTakeFuel(! getTakeFuel());			
				break;
			default:
				return packetType;
			}
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return unserializeNulldId;
	}
	
	public boolean controlExternal = false,takeFuel = false;
	
	public boolean getControlExternal()
	{
		return controlExternal;
	}
	public void setControlExternal(boolean value)
	{
		if(value != controlExternal) needPublish();
		controlExternal = value;
		computeInventory();
	}
	public boolean getTakeFuel()
	{
		return takeFuel;
	}
	public void setTakeFuel(boolean value)
	{
		if(value != takeFuel) needPublish();
		takeFuel = value;
		
	}
	@Override
	public void inventoryChange(IInventory inventory) {
		// TODO Auto-generated method stub
		super.inventoryChange(inventory);
		

		computeInventory();
		needPublish();
	}
	
	void computeInventory()
	{
		
		ItemStack regulatorStack = inventory.getStackInSlot(HeatFurnaceContainer.regulatorId);
		
		if(regulatorStack != null && controlExternal == false)
		{
			IRegulatorDescriptor regulator = (IRegulatorDescriptor) Utils.getItemObject(regulatorStack);
			
			regulator.applyTo(this.regulator,500.0,10.0,0.1,0.1);
		//	furnace.regulator.target = 240;
		}
		else
		{
			regulator.setManuel();
		}	
	}
	
	@Override
	public void writeToNBT(NBTTagCompound nbt) {
		// TODO Auto-generated method stub
		super.writeToNBT(nbt);
		
		nbt.setBoolean("takeFuel", takeFuel);
		nbt.setBoolean("controlExternal", controlExternal);
	}
	
	@Override
	public void readFromNBT(NBTTagCompound nbt) {
		// TODO Auto-generated method stub
		super.readFromNBT(nbt);
		takeFuel = nbt.getBoolean("takeFuel");
		controlExternal = nbt.getBoolean("controlExternal");
	}
	
	
	
}
