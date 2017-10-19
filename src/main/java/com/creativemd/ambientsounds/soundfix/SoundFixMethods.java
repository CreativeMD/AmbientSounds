package com.creativemd.ambientsounds.soundfix;

import java.lang.reflect.Field;
import java.util.Iterator;

import net.minecraftforge.fml.relauncher.ReflectionHelper;
import paulscode.sound.Source;

public class SoundFixMethods {
	
	public static Field removed = ReflectionHelper.findField(Source.class, "removed");
	
	public static void test(Iterator<Source> iter)
	{
		Source source = iter.next();
		source = removeSource(source);
	}
	
	public static Source removeSource(Source source)
	{
		try {
			if(removed.getBoolean(source))
			{
				source.cleanup();
				return null;
			}
		} catch (IllegalArgumentException | IllegalAccessException e) {
			e.printStackTrace();
		}
		return source;
	}
	
	public static void cleanupSource(Source source)
	{
		if(source.toStream)
			try {
				removed.setBoolean(source, true);
			} catch (IllegalArgumentException | IllegalAccessException e) {
				e.printStackTrace();
			}
		else
			source.cleanup();
	}
	
}
