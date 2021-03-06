package mods.eln.electricaltimeout;

import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;

import org.lwjgl.opengl.GL11;

import mods.eln.Eln;
import mods.eln.cable.CableRender;
import mods.eln.cable.CableRenderDescriptor;
import mods.eln.client.ClientProxy;
import mods.eln.client.FrameTime;
import mods.eln.electricalsource.ElectricalSourceGui;
import mods.eln.heatfurnace.HeatFurnaceElement;
import mods.eln.item.MeterItemArmor;
import mods.eln.misc.Direction;
import mods.eln.misc.LRDU;
import mods.eln.misc.Obj3D;
import mods.eln.misc.PhysicalInterpolator;
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

public class ElectricalTimeoutRender extends SixNodeElementRender {

	ElectricalTimeoutDescriptor descriptor;
	long time;
	public ElectricalTimeoutRender(SixNodeEntity tileEntity, Direction side, SixNodeDescriptor descriptor) {
		super(tileEntity, side, descriptor);
		this.descriptor = (ElectricalTimeoutDescriptor) descriptor;
		time = System.currentTimeMillis();
	}

	//PhysicalInterpolator interpolator = new PhysicalInterpolator(0.2f, 2.0f, 1.5f, 0.2f);

	@Override
	public void draw() {
		super.draw();
		front.glRotateOnX();
	
		descriptor.draw(timeoutCounter / timeoutValue);
	}
	
	@Override
	public void refresh(float deltaT) {
		if(inputState == false) {
			timeoutCounter -= deltaT;
			if(timeoutCounter < 0f) timeoutCounter = 0f;
		}
	}
	
	@Override
	public boolean cameraDrawOptimisation() {
		return false;
	}

	float timeoutValue = 0, timeoutCounter = 0;
	boolean inputState;
	
	@Override
	public void publishUnserialize(DataInputStream stream) {
		super.publishUnserialize(stream);
		try {
			timeoutValue = stream.readFloat();
			timeoutCounter = stream.readFloat();
			inputState = stream.readBoolean();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	@Override
	public CableRenderDescriptor getCableRender(LRDU lrdu) {
		return Eln.instance.signalCableDescriptor.render;
	}
	
	@Override
	public GuiScreen newGuiDraw(Direction side, EntityPlayer player) {
		return new ElectricalTimeoutGui(player,this);
	}
}
