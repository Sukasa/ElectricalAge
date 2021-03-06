package mods.eln.autominer;

import java.io.DataInputStream;
import java.io.IOException;

import org.lwjgl.opengl.GL11;

import mods.eln.client.FrameTime;
import mods.eln.misc.Direction;
import mods.eln.misc.Obj3D;
import mods.eln.misc.Obj3D.Obj3DPart;
import mods.eln.node.TransparentNodeDescriptor;
import mods.eln.node.TransparentNodeElementInventory;
import mods.eln.node.TransparentNodeElementRender;
import mods.eln.node.TransparentNodeEntity;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.entity.player.EntityPlayer;

public class AutoMinerRender extends TransparentNodeElementRender {
	AutoMinerDescriptor descriptor;
	float[] buttonsState;
	boolean[] ledsAState;
	boolean[] ledsPState;

	public AutoMinerRender(TransparentNodeEntity tileEntity,
			TransparentNodeDescriptor descriptor) {
		super(tileEntity, descriptor);
		this.descriptor = (AutoMinerDescriptor) descriptor;

		buttonsState = new float[this.descriptor.buttonsCount];
		for (int idx = 0; idx < this.descriptor.buttonsCount; idx++) {
			buttonsState[idx] = (float) Math.random();
		}

		ledsAState = new boolean[this.descriptor.ledsACount];
		for (int idx = 0; idx < this.descriptor.ledsACount; idx++) {
			ledsAState[idx] = Math.random() > 0.5;
		}

		ledsPState = new boolean[this.descriptor.ledsPCount];
		for (int idx = 0; idx < this.descriptor.ledsPCount; idx++) {
			ledsPState[idx] = Math.random() > 0.5;
		}
	}

	@Override
	public void draw() {

		if (pipeLength != 0) {
			GL11.glPushMatrix();
			for (int idx = pipeLength; idx != 0; idx--) {
				if (idx != 1) {
					descriptor.pipe.draw();
				}
				else {
					descriptor.head.draw();
				}
				GL11.glTranslatef(0, -1f, 0);
			}
			GL11.glPopMatrix();

		}

		for (int idx = 0; idx < this.descriptor.buttonsCount; idx++) {
			buttonsState[idx] = idx == job.ordinal() ? 1 : 0;
		}

		front.glRotateXnRef();
		descriptor.draw(false, buttonsState, ledsAState, ledsPState);
	}

	public void refresh(float deltaT) {
		for (int idx = 0; idx < this.descriptor.ledsACount; idx++) {
			if (Math.random() < 0.2 * deltaT)
				ledsAState[idx] = !ledsAState[idx];
		}

		for (int idx = 0; idx < this.descriptor.ledsPCount; idx++) {
			if (Math.random() < 0.2 * deltaT)
				ledsPState[idx] = !ledsPState[idx];
		}

	}

	TransparentNodeElementInventory inventory = new TransparentNodeElementInventory(AutoMinerContainer.inventorySize, 64, this);

	@Override
	public GuiScreen newGuiDraw(Direction side, EntityPlayer player) {
		return new AutoMinerGuiDraw(player, inventory, this);
	}

	short pipeLength = 0;
	AutoMinerSlowProcess.jobType job;

	@Override
	public void networkUnserialize(DataInputStream stream) {
		super.networkUnserialize(stream);
		try {
			pipeLength = stream.readShort();
			job = AutoMinerSlowProcess.jobType.values()[stream.readByte()];
			//	Utils.println(job + " " + pipeLength);

		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	@Override
	public boolean cameraDrawOptimisation() {
		return false;
	}
}
