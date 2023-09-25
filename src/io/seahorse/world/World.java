package io.seahorse.world;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.awt.image.RenderedImage;
import java.io.File;
import java.io.IOException;
import java.util.LinkedList;
import java.util.Queue;

public class World {
	
	private static final int CACHE_SIZE = 15;
	private static final int SMALLEST_CACHED_WORLD_GRAIN_AMOUNT = 20;
	private static final int CACHED_WORLD_GRAIN_AMOUNT_FACTOR = 2;
	private static final World[] CACHED_WORLDS = new World[CACHE_SIZE];
	
	private static final int EXPENSE_LENGTH = 10;
	private static final long TOPPLE_GRAIN_AMOUNT = 4;
	
	private static final long POSITION_COORDINATE_MASK = 0xFFFFFFFFL;
	private static final int POSITION_COORDINATE_OFFSET = 32;
	
	private static final int[] GRAIN_AMOUNT_COLORS = new int[] {
			0x000000,
			0x0AB4B4,
			0xFF00FF,
			0xFFFFFF
	};
	
	static {
		int grainAmount = SMALLEST_CACHED_WORLD_GRAIN_AMOUNT;
		
		for(int i = 0; i < CACHE_SIZE; i++) {
			CACHED_WORLDS[i] = new World(grainAmount);
			grainAmount *= CACHED_WORLD_GRAIN_AMOUNT_FACTOR;
		}
	}
	
	private final int length;
	private final long grainAmount;
	private final int centerX;
	private final int centerY;
	
	private final long[][] grainAmounts;
	
	private final Queue<Long> topplePositions = new LinkedList<>();
	
	public World(long grainAmount) {
		this((int) Math.sqrt(grainAmount) + EXPENSE_LENGTH, grainAmount);
	}
	
	private World(int length, long grainAmount) {
		this.length = length;
		this.grainAmount = grainAmount;
		this.centerX = length / 2;
		this.centerY = length / 2;
		this.grainAmounts = new long[length][length];
		
		addCachedGrains(centerX, centerY, grainAmount);
		topple();
	}
	
	public void saveAsImage(String path, String imageFormatName) {
		RenderedImage image = asImage();
		File file = new File(path);
		
		try {
			
			ImageIO.write(image, imageFormatName, file);
			
		} catch (IOException exception) {
			
			String message = String.format("An exception occurred while saving world as image: " + exception);
			System.out.println(message);
		}
	}
	
	public RenderedImage asImage() {
		BufferedImage image = new BufferedImage(length, length, BufferedImage.TYPE_INT_RGB);
		
		for(int x = 0; x < length; x++) {
			for(int y = 0; y < length; y++) {
				long amount = grainAmounts[x][y];
				int color = GRAIN_AMOUNT_COLORS[(int) amount];
				image.setRGB(x, y, color);
			}
		}
		
		return image;
	}
	
	private void topple() {
		while(!topplePositions.isEmpty()) {
			long position = topplePositions.poll();
			int x = positionX(position);
			int y = positionY(position);
			
			topplePosition(x, y);
		}
	}
	
	private void topplePosition(int x, int y) {
		long amount = grainAmounts[x][y];
		if(amount < TOPPLE_GRAIN_AMOUNT) return;
		
		grainAmounts[x][y] = amount % TOPPLE_GRAIN_AMOUNT;
		amount /= TOPPLE_GRAIN_AMOUNT;
		
		addGrains(x - 1, y, amount);
		addGrains(x + 1, y, amount);
		addGrains(x, y - 1, amount);
		addGrains(x, y + 1, amount);
	}
	
	private void addCachedGrains(int x, int y, long amount) {
		while(amount > 0) {
			World world = getCachedWorld(amount);
			if(world == null) {
				
				addGrains(x, y, amount);
				return;
			}
			
			for(int i = 0; i < world.getLength(); i++) {
				for(int j = 0; j < world.getLength(); j++) {
					
					int dx = i - world.getCenterX();
					int dy = j - world.getCenterY();
					long a = world.getGrainAmount(i, j);
					if(a > 0) addGrains(x + dx, y + dy, a);
				}
			}
			
			amount -= world.getGrainAmount();
		}
	}
	
	private void addGrains(int x, int y, long amount) {
		long a = grainAmounts[x][y];
		boolean topple = a >= TOPPLE_GRAIN_AMOUNT;
		
		a += amount;
		grainAmounts[x][y] = a;
		
		if(!topple && a >= TOPPLE_GRAIN_AMOUNT) {
			
			long position = position(x, y);
			topplePositions.add(position);
		}
	}
	
	public int getLength() {
		return length;
	}
	
	public int getCenterX() {
		return centerX;
	}
	
	public int getCenterY() {
		return centerY;
	}
	
	public long getGrainAmount(int x, int y) {
		return grainAmounts[x][y];
	}
	
	public long getGrainAmount() {
		return grainAmount;
	}
	
	private static int positionX(long position) {
		return (int) ((position >> POSITION_COORDINATE_OFFSET) & POSITION_COORDINATE_MASK);
	}
	
	private static int positionY(long position) {
		return (int) (position & POSITION_COORDINATE_MASK);
	}
	
	private static long position(int x, int y) {
		return (long) x << POSITION_COORDINATE_OFFSET | y;
	}
	
	public static World getCachedWorld(long grainAmount) {
		if(grainAmount < SMALLEST_CACHED_WORLD_GRAIN_AMOUNT) return null;
		
		for(int i = CACHED_WORLDS.length - 1; i >= 0; i--) {
			World world = CACHED_WORLDS[i];
			
			if(world != null && world.getGrainAmount() <= grainAmount) return world;
		}
		
		return null;
	}
	
}
