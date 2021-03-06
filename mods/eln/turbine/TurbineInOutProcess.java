package mods.eln.turbine;

import mods.eln.misc.Coordonate;
import mods.eln.misc.Utils;
import mods.eln.sim.ElectricalPowerSource;
import mods.eln.sim.IProcess;
import mods.eln.sim.PhysicalConstant;
import mods.eln.sim.ThermalLoad;
import mods.eln.sound.SoundCommand;
import mods.eln.sound.SoundLooper;
import mods.eln.sound.SoundServer;
import mods.eln.sound.SoundTrack;


public class TurbineInOutProcess implements IProcess{
	TurbineElement turbine;
	//double timeCounter = 0, soundTimerCounter = Math.random()*soundTimeOut, energyCounterGlobal = 0;
	static int staticId = 0;
	int id;
	public TurbineInOutProcess(TurbineElement t) {
		this.turbine = t;
		id = staticId++;	
		soundLooper = new SoundLooper(t) {						
			@Override
			public SoundCommand mustStart() {
				double deltaT = turbine.warmLoad.Tc - turbine.coolLoad.Tc;
				if(deltaT < 40) return null;
				float factor = (float)(deltaT / turbine.descriptor.nominalDeltaT);
				SoundCommand track = turbine.descriptor.sound.copy().mulVolume(1 * (0.1f * factor), 0.9f + 0.2f * factor);
				return track;
			}
		};
	}
	
	SoundLooper soundLooper;
	
	@Override
	public void process(double time) {
		TurbineDescriptor descriptor = turbine.descriptor;
		double deltaT = turbine.warmLoad.Tc - turbine.coolLoad.Tc;
		if(deltaT < 0) return;
		double deltaU = turbine.positiveLoad.Uc - turbine.negativeLoad.Uc;
		double targetU = descriptor.TtoU.getValue(deltaT);

		ElectricalPowerSource eps = turbine.electricalPowerSourceProcess;
		eps.setUmax(targetU);
		if(targetU - deltaU > 0)
		{
			eps.setP((targetU - deltaU) * descriptor.powerOutPerDeltaU);
		}
		else
		{
			eps.setP(0);
		}
		
		double eff  = Math.abs(1 - (turbine.coolLoad.Tc + PhysicalConstant.Tref)/(turbine.warmLoad.Tc + PhysicalConstant.Tref));
		if(eff < 0.05) eff = 0.05;
		//eff = 0.4;
		double E = eps.getEnergyCounter();
		//energyCounterGlobal += E;
		double Pout = E/time;
		double Pin = descriptor.PoutToPin.getValue(Pout) / eff;
		turbine.warmLoad.movePowerTo(-Pin);
		turbine.coolLoad.movePowerTo(Pin * (1 - eff));
		//ThermalLoad.movePower(Pin, turbine.warmLoad, turbine.coolLoad);		
		eps.clearEnergyCounter();
		/*timeCounter+=time;
		if(timeCounter >= 1.0){
			timeCounter -= 1.0;
			//Utils.println("Turbine " + id + " : " + Utils.plotPower("Pin : ", Pin) + Utils.plotPower("Pout : ", Pout) + Utils.plotEnergy("Pavg", energyCounterGlobal));
			energyCounterGlobal = 0;
		}
		
		soundTimerCounter += time;
		if (soundTimerCounter >= soundTimeOut && deltaT > 40) {
			float factor = (float)(deltaT / turbine.descriptor.nominalDeltaT);
			turbine.play(new SoundCommand(descriptor.soundName).setVolume(descriptor.nominalVolume * (0.1f * factor), 0.9f + 0.2f * factor).mediumRange());
			soundTimerCounter = 0;
		}*/
		
		
		soundLooper.process(time);
		
	}

}
