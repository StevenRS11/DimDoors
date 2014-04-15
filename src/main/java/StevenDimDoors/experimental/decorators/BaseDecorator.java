package StevenDimDoors.experimental.decorators;

import java.util.Random;

import net.minecraft.world.World;
import StevenDimDoors.experimental.RoomData;
import StevenDimDoors.mod_pocketDim.Point3D;
import StevenDimDoors.mod_pocketDim.config.DDProperties;

public abstract class BaseDecorator
{
	public BaseDecorator() { }
	
	public abstract boolean canDecorate(RoomData room);
	
	public abstract void decorate(RoomData room, World world, Point3D offset, Random random, DDProperties properties);
}
