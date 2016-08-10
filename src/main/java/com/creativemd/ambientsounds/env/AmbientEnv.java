package com.creativemd.ambientsounds.env;

import java.util.ArrayList;

import com.creativemd.ambientsounds.AmbientSound;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.world.World;

public abstract class AmbientEnv {
	
	public static final ArrayList<AmbientEnv> envs = new ArrayList<>();
	
	public static HeightEnv height = new HeightEnv();
	public static BiomeEnv biome = new BiomeEnv();
	
	public int tickTime = 60;
	public int ticker = 0;
	
	public AmbientEnv()
	{
		envs.add(this);
	}
	
	public void resetTick()
	{
		ticker = tickTime;
	}
	
	public void tick(World world, EntityPlayer player)
	{
		if(ticker <= 0)
		{
			update(world, player);
			resetTick();
		}
		ticker--;
	}
	
	protected abstract void update(World world, EntityPlayer player);
	
}
