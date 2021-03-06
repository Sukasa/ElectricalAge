package mods.eln.electricasensor;

import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;

import org.lwjgl.opengl.GL11;

import mods.eln.Eln;
import mods.eln.cable.CableRender;
import mods.eln.cable.CableRenderDescriptor;
import mods.eln.client.ClientProxy;
import mods.eln.electricalcable.ElectricalCableDescriptor;
import mods.eln.electricalsource.ElectricalSourceGui;
import mods.eln.generic.GenericItemBlockUsingDamageDescriptor;
import mods.eln.generic.GenericItemUsingDamageDescriptor;
import mods.eln.heatfurnace.HeatFurnaceElement;
import mods.eln.item.MeterItemArmor;
import mods.eln.misc.Direction;
import mods.eln.misc.LRDU;
import mods.eln.misc.Obj3D;
import mods.eln.misc.Utils;
import mods.eln.misc.Obj3D.Obj3DPart;
import mods.eln.node.NodeBase;
import mods.eln.node.SixNodeDescriptor;
import mods.eln.node.SixNodeElementInventory;
import mods.eln.node.SixNodeElementRender;
import mods.eln.node.SixNodeEntity;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;


public class ElectricalSensorRender extends SixNodeElementRender{

	SixNodeElementInventory inventory = new SixNodeElementInventory(1,64,this);
	ElectricalSensorDescriptor descriptor;
	long time;
	public ElectricalSensorRender(SixNodeEntity tileEntity, Direction side,
			SixNodeDescriptor descriptor) {
		super(tileEntity, side, descriptor);
		this.descriptor = (ElectricalSensorDescriptor) descriptor;
		time = System.currentTimeMillis();
	}




	@Override
	public void draw() {
		super.draw();
		front.glRotateOnX();
		descriptor.draw();

	}
	
	/*
	@Override
	public CableRenderDescriptor getCableRender(LRDU lrdu) {
		// TODO Auto-generated method stub
		return descriptor.cableRender;
	}
	*/
	

	int typeOfSensor = 0; 
	float lowValue = 0,highValue = 50;
	byte dirType;
	CableRenderDescriptor cableRender = null;
	@Override
	public void publishUnserialize(DataInputStream stream) {
		// TODO Auto-generated method stub
		super.publishUnserialize(stream);
		try {
			Byte b;
			b = stream.readByte();
			typeOfSensor = b & 0x3;
			lowValue = stream.readFloat();
			highValue = stream.readFloat();
			dirType = stream.readByte();
			cableRender = ElectricalCableDescriptor.getCableRender(Utils.unserialiseItemStack(stream));
			
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}	

	}
	
	@Override
	public CableRenderDescriptor getCableRender(LRDU lrdu) {
		if(descriptor.voltageOnly)
		{
			if(lrdu == front) return Eln.instance.signalCableDescriptor.render;
			if(lrdu == front.inverse()) return cableRender;
			
		}
		else
		{
			if(lrdu == front) return Eln.instance.signalCableDescriptor.render;
			if(lrdu == front.left() || lrdu == front.right()) return cableRender;
		}
		return super.getCableRender(lrdu);
	}
	
	
	@Override
	public GuiScreen newGuiDraw(Direction side, EntityPlayer player) {
		// TODO Auto-generated method stub
		return new ElectricalSensorGui(player,inventory,this);
	}
}
