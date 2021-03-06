package mods.eln.electricalfurnace;

import java.io.DataInputStream;
import java.io.IOException;

import mods.eln.autominer.AutoMinerContainer;
import mods.eln.generic.GenericItemUsingDamage;
import mods.eln.item.HeatingCorpElement;
import mods.eln.item.ThermalIsolatorElement;
import mods.eln.item.regulator.IRegulatorDescriptor;
import mods.eln.misc.Direction;
import mods.eln.misc.LRDU;
import mods.eln.misc.Utils;
import mods.eln.node.NodeBase;
import mods.eln.node.NodeElectricalLoad;
import mods.eln.node.NodeThermalLoad;
import mods.eln.node.TransparentNode;
import mods.eln.node.TransparentNodeDescriptor;
import mods.eln.node.TransparentNodeElement;
import mods.eln.node.TransparentNodeElementInventory;
import mods.eln.sim.ElectricalLoad;
import mods.eln.sim.ElectricalResistor;
import mods.eln.sim.ElectricalResistorHeatThermalLoad;
import mods.eln.sim.ElectricalResistorWithCounter;
import mods.eln.sim.RegulatorThermalLoadToElectricalResistor;
import mods.eln.sim.RegulatorType;
import mods.eln.sim.ThermalLoad;
import mods.eln.sim.ThermalRegulator;
import mods.eln.sim.ThermalResistor;
import mods.eln.sim.ThermalWatchdogProcessForInventoryItemDamageSingleLoad;
import mods.eln.sim.VoltageWatchdogProcessForInventoryItemDamageSingleLoad;
import net.minecraft.entity.item.EntityItem;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.inventory.Container;
import net.minecraft.inventory.IInventory;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;

public class ElectricalFurnaceElement extends TransparentNodeElement {

	TransparentNodeElementInventory inventory = new TransparentNodeElementInventory(5, 64, this);

	public static final int inSlotId = 0;
	public static final int outSlotId = 1;
	public static final int heatingCorpSlotId = 2;
	public static final int thermalIsolatorSlotId = 3;
	public static final int thermalRegulatorSlotId = 4;
	
	NodeElectricalLoad electricalLoad = new NodeElectricalLoad("electricalLoad");
	ElectricalResistor heatingCorpResistor = new ElectricalResistor(electricalLoad, ElectricalLoad.groundLoad);
	
	NodeThermalLoad thermalLoad = new NodeThermalLoad("thermalLoad");
	ThermalResistor smeltResistor = new ThermalResistor(thermalLoad, ThermalLoad.externalLoad);

	RegulatorThermalLoadToElectricalResistor thermalRegulator = new RegulatorThermalLoadToElectricalResistor("thermalRegulator", thermalLoad, heatingCorpResistor);
	
	ElectricalResistorHeatThermalLoad heatingCorpResistorHeatThermalLoad = new ElectricalResistorHeatThermalLoad(heatingCorpResistor, thermalLoad);
	ElectricalFurnaceProcess slowRefreshProcess = new ElectricalFurnaceProcess(this);
	
	VoltageWatchdogProcessForInventoryItemDamageSingleLoad HeatingCorpWatchDog = new VoltageWatchdogProcessForInventoryItemDamageSingleLoad(inventory, heatingCorpSlotId, electricalLoad);
	
	ThermalWatchdogProcessForInventoryItemDamageSingleLoad thermalIsolationWatchdog = new ThermalWatchdogProcessForInventoryItemDamageSingleLoad(inventory, thermalIsolatorSlotId, thermalLoad); 
	
	boolean powerOn = false;
	boolean autoShutDown = true;
	ElectricalFurnaceDescriptor descriptor;
	
	public ElectricalFurnaceElement(TransparentNode transparentNode, TransparentNodeDescriptor descriptor) {
		super(transparentNode,descriptor);
		this.descriptor = (ElectricalFurnaceDescriptor) descriptor;
		//NOT SIMULATED electricalLoadList.add(negativeLoad);
		electricalLoadList.add(electricalLoad);
		electricalProcessList.add(heatingCorpResistor);
		
		slowProcessList.add(HeatingCorpWatchDog);
		slowProcessList.add(thermalIsolationWatchdog);
		
		thermalLoadList.add(thermalLoad);
		thermalProcessList.add(smeltResistor);
		
		thermalProcessList.add(heatingCorpResistorHeatThermalLoad);
		thermalProcessList.add(thermalRegulator);
		
		slowProcessList.add(slowRefreshProcess);
	}

	@Override
	public IInventory getInventory() {
		return inventory;
	}
	
	@Override
	public boolean hasGui() {
		return true;
	}
	
	@Override
	public Container newContainer(Direction side, EntityPlayer player) {
		return new ElectricalFurnaceContainer(this.node, player, inventory);
	}
	
	@Override
	public ElectricalLoad getElectricalLoad(Direction side, LRDU lrdu) {
		return electricalLoad;
	}

	@Override
	public ThermalLoad getThermalLoad(Direction side, LRDU lrdu) {
		return null;
	}

	@Override
	public int getConnectionMask(Direction side, LRDU lrdu) {
		if(side == front.getInverse() && lrdu == LRDU.Down)
			return NodeBase.maskElectricalPower;
		return 0;
	}

	@Override
	public String multiMeterString(Direction side) {
		return Utils.plotUIP(electricalLoad.Uc, electricalLoad.getCurrent());
	}
	
	@Override
	public String thermoMeterString(Direction side) {
		return Utils.plotCelsius("T:", thermalLoad.Tc);
	}

	@Override
	public void initialize() {
		//ElectricalCableDescriptor.list[Eln.lowVoltageCableId].setElectricalLoad(positiveLoad, false);
		//ElectricalCableDescriptor.list[Eln.lowVoltageCableId].setElectricalLoad(negativeLoad, grounded);
		
		//thermalLoad.C = 10.0;
		
		descriptor.applyTo(thermalLoad);
		inventoryChange(getInventory());
		
		smeltResistor.highImpedance();
		slowRefreshProcess.process(0.05);
		
	//	ItemStack stack = new ItemStack(Item.coal);
	//	EntityItem entity = new EntityItem(node.coordonate.world(), node.coordonate.x + 0.5, node.coordonate.y + 0.5, node.coordonate.z + 1.5, stack);
	//	node.coordonate.world().spawnEntityInWorld(entity);
		
		connect();
	}
	
	@Override
	public void inventoryChange(IInventory inventory) {
		super.inventoryChange(inventory);
		setPhysicalValue();
		needPublish();
	}
	
	public void setPhysicalValue() {
		ItemStack itemStack;
		
		itemStack = inventory.getStackInSlot(heatingCorpSlotId);
		if(itemStack == null) {
			thermalRegulator.setRmin(1000000000.0);
		}
		else {
			HeatingCorpElement element = ((GenericItemUsingDamage<HeatingCorpElement>)itemStack.getItem()).getDescriptor(itemStack);
			element.applyTo(thermalRegulator);
			element.applyTo(electricalLoad);
/*10W*/		electricalLoad.setRp(element.electricalNominalU * element.electricalNominalU / 10);
			if(!powerOn) {
				electricalLoad.setRs(1000000000.0);
			}
		}
		
		itemStack = inventory.getStackInSlot(thermalRegulatorSlotId);
		if(itemStack == null) {
			thermalRegulator.setNone();
		}
		else {
			IRegulatorDescriptor element = ((GenericItemUsingDamage<IRegulatorDescriptor>)itemStack.getItem()).getDescriptor(itemStack);
			element.applyTo(thermalRegulator, 500.0, 10.0, 0.1, 0.1);
		}	
	}
	
	@Override
	public boolean onBlockActivated(EntityPlayer entityPlayer, Direction side, float vx, float vy, float vz) {
		return false;
	}

	public void networkSerialize(java.io.DataOutputStream stream) {
		super.networkSerialize(stream);
		try {
			stream.writeByte((powerOn ? 1 : 0) + (heatingCorpResistor.getP() > 5 ? 2 : 0));
			
			stream.writeShort((int) thermalRegulator.getTarget());
			stream.writeShort((int) thermalLoad.Tc);
			
			ItemStack stack;
			if((stack = inventory.getStackInSlot(inSlotId)) == null) {
				stream.writeShort(-1);
				stream.writeShort(-1);
			}
			else {
				stream.writeShort(Item.getIdFromItem(stack.getItem()));
				stream.writeShort(stack.getItemDamage());				
			}
			
			stream.writeShort((int) heatingCorpResistor.getP());
			stream.writeFloat((float) electricalLoad.Uc);
			stream.writeFloat((float) slowRefreshProcess.processState());
			stream.writeFloat((float) slowRefreshProcess.processStatePerSecond());
			
			stream.writeBoolean(autoShutDown);
			
		} catch (IOException e) {
			e.printStackTrace();
		}	
	}
	
	@Override
	public void writeToNBT(NBTTagCompound nbt) {
		super.writeToNBT(nbt);
		nbt.setBoolean("powerOn", powerOn);
		nbt.setBoolean("autoShutDown", autoShutDown);
	}
	
	@Override
	public void readFromNBT(NBTTagCompound nbt) {
		super.readFromNBT(nbt);
		powerOn = nbt.getBoolean("powerOn");
		autoShutDown = nbt.getBoolean("autoShutDown");
	}
	
	public void setPowerOn(boolean value) {
		if(powerOn != value) {
			powerOn = value;
			setPhysicalValue();
			needPublish();
		}
	}
	
	public static final byte unserializePowerOnId = 1;
	public static final byte unserializeTemperatureTarget = 2;
	public static final byte unserializeAutoShutDownId = 3;
	
	@Override
	public byte networkUnserialize(DataInputStream stream) {

		byte packetType = super.networkUnserialize(stream);
		try {
			switch(packetType) {
			case unserializePowerOnId:			
				setPowerOn(stream.readByte() != 0);
				break;
			case unserializeAutoShutDownId:			
				autoShutDown = ! autoShutDown;
				needPublish();
				break;
			case unserializeTemperatureTarget:
				thermalRegulator.setTarget(stream.readFloat());
				needPublish();
				break;
			default:
				return packetType;
			}
		} catch (IOException e) {
			e.printStackTrace();
		}
		return unserializeNulldId;
	}
}
