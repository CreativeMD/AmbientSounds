package com.creativemd.ambientsounds.env;

import java.util.HashMap;

import org.apache.commons.lang3.tuple.Pair;

import com.creativemd.ambientsounds.env.HeightEnv.HeightArea;
import com.creativemd.ambientsounds.sound.HeightSound;

import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.Blocks;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

public class HeightEnv extends AmbientEnv {
	
	public HashMap<HeightArea, Float> currentHeight = new HashMap<>();
	
	public static class HeightPosition {
		
		public final boolean isAbsolute;
		public final int value;
		
		public HeightPosition(boolean isAbsolute, int value) {
			this.isAbsolute = isAbsolute;
			this.value = value;
		}
		
		public float getValue(float y, float relativeY)
		{
			if(isAbsolute)
				return y-value;
			return relativeY-value;
		}
		
	}
	
	public static enum HeightArea {
		
		Underworld(-1, new HeightPosition(true, Integer.MIN_VALUE), new HeightPosition(true, 0), 0),
		Cave(0, new HeightPosition(true, 0), new HeightPosition(false, -15), 5),
		Surface(1, new HeightPosition(false, -5), new HeightPosition(false, 15), 5),
		Sky(2, new HeightPosition(false, 30), new HeightPosition(true, 4000), 10),
		Space(3, new HeightPosition(true, 4000), new HeightPosition(true, Integer.MAX_VALUE), 100);
		
		public final int index;
		public final HeightPosition minHeight;
		public final HeightPosition maxHeight;
		public final int fadeValue;
		
		HeightArea(int index, HeightPosition minHeight, HeightPosition maxHeight, int fadeValue) {
			this.index = index;
			this.minHeight = minHeight;
			this.maxHeight = maxHeight;
			this.fadeValue = fadeValue;
		}
	}

	@Override
	protected void update(World world, EntityPlayer player) {
		float sum = 0;
		int count = 0;
		
		for (int x = -2; x < 3; x++) {
			for (int z = -2; z < 3; z++) {
				int posX = (int) (player.posX+x*2);
				int posZ = (int) (player.posZ+z*2);
				int height = getHeightBlock(world, posX, posZ);
				
				sum += height;
				count++;
			}
		}
		float average = sum/count;
		
		float y = (float) player.posY;
		float relativeHeight = y-average;
		
		currentHeight.clear();
		
		for (int i = 0; i < HeightArea.values().length; i++) {
			HeightArea area = HeightArea.values()[i];
			float valueMin = area.minHeight.getValue(y, relativeHeight);
			float valueMax = area.maxHeight.getValue(y, relativeHeight);
			float volume = 0;
			if(valueMin >= 0 && valueMax <= 0)
				volume = 1;
			else if(valueMin > -area.fadeValue && valueMin < 0)
				volume = 1-(valueMin/-area.fadeValue);
			else if(valueMax < area.fadeValue && valueMax > 0)
				volume = 1-(valueMax/area.fadeValue);
			if(volume > 0)
				currentHeight.put(area, volume);
		}
		
		//System.out.println(currentHeight);
	}
	
	public static int getHeightBlock(World world, int x, int z)
    {
        int y;
        int heighest = 2;

        for (y = 45; y < 256; ++y)
        {
        	IBlockState state = world.getBlockState(new BlockPos(x, y, z));
            if(state.isBlockNormalCube() || state.getBlock() == Blocks.WATER)
            	heighest = y;
        }

        return heighest;
    }

}
