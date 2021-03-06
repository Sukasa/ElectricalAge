package mods.eln.electricalregulator;

import mods.eln.Eln;
import mods.eln.misc.Direction;
import mods.eln.misc.LRDU;
import mods.eln.node.NodeElectricalLoad;
import mods.eln.node.NodeElectricalSourceWithCurrentLimitationProcess;
import mods.eln.node.SixNode;
import mods.eln.node.SixNodeDescriptor;
import mods.eln.node.SixNodeElement;
import mods.eln.sim.ElectricalLoad;
import mods.eln.sim.ThermalLoad;
import net.minecraft.entity.player.EntityPlayer;

public class ElectricalRegulatorElement extends SixNodeElement {

	ElectricalRegulatorDescriptor descriptor;
	NodeElectricalLoad outputGateLoad = new NodeElectricalLoad("outputGateLoad");
	NodeElectricalSourceWithCurrentLimitationProcess outputGateProcess = new NodeElectricalSourceWithCurrentLimitationProcess("outputGateProcess", outputGateLoad, ElectricalLoad.groundLoad, 0, Eln.gateOutputCurrent);
	
	public ElectricalRegulatorElement(SixNode sixNode, Direction side, SixNodeDescriptor descriptor) {
		super(sixNode, side, descriptor);
		this.descriptor = (ElectricalRegulatorDescriptor) descriptor;
		
		electricalLoadList.add(outputGateLoad);
		electricalProcessList.add(outputGateProcess);
	}

	@Override
	public ElectricalLoad getElectricalLoad(LRDU lrdu) {
		return null;
	}

	@Override
	public ThermalLoad getThermalLoad(LRDU lrdu) {
		return null;
	}

	@Override
	public int getConnectionMask(LRDU lrdu) {
		return 0;
	}

	@Override
	public String multiMeterString() {
		return null;
	}

	@Override
	public String thermoMeterString() {
		return null;
	}

	@Override
	public void initialize() {
	}

	@Override
	public boolean onBlockActivated(EntityPlayer entityPlayer, Direction side, float vx, float vy, float vz) {
		return false;
	}
}
