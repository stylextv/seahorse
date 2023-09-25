package io.seahorse;

import io.seahorse.world.World;

public class Seahorse {
	
	private static final int WORLD_GRAIN_AMOUNT = 100000;
	
	public static void main(String[] args) {
		long time = System.currentTimeMillis();
		
		World world = new World(WORLD_GRAIN_AMOUNT);
		world.saveAsImage("world.png", "png");
		
		System.out.println(System.currentTimeMillis() - time);
	}
	
}
