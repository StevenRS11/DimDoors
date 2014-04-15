package StevenDimDoors.experimental.decorators;

import java.util.Random;

import net.minecraft.world.World;

import StevenDimDoors.experimental.RoomData;
import StevenDimDoors.mod_pocketDim.Point3D;
import StevenDimDoors.mod_pocketDim.config.DDProperties;

public class LinkDestinationDecorator extends BaseDecorator
{
	@Override
	public boolean canDecorate(RoomData room)
	{
		return room.getOutboundLinks().isEmpty() && !room.getInboundLinks().isEmpty();
	}

	@Override
	public void decorate(RoomData room, World world, Point3D offset, Random random, DDProperties properties)
	{
		
	}
}
