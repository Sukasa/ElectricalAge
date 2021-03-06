package mods.eln.electricalredstoneoutput;

import mods.eln.misc.Coordonate;
import mods.eln.sim.IProcess;

public class ElectricalRedstoneOutputSlowProcess implements IProcess {
	ElectricalRedstoneOutputElement element;
	
	public ElectricalRedstoneOutputSlowProcess(ElectricalRedstoneOutputElement element) {
		this.element = element;
	}

	double sleepCounter = 0;
	static final double sleepDuration = 0.2;

	@Override
	public void process(double time) {
		if(sleepCounter == 0.0) {
			if(element.refreshRedstone())
				sleepCounter = sleepDuration;	
		}
		else {
			sleepCounter -= time;
			if(sleepCounter < 0.0) sleepCounter = 0.0;
		}
	}
}
