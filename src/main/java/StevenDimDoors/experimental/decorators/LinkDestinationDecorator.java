package StevenDimDoors.experimental.decorators;

import java.util.Random;

import net.minecraft.world.World;

import StevenDimDoors.experimental.LinkPlan;
import StevenDimDoors.experimental.PartitionNode;
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
		// Set the center of the room as the destination for all inbound links
		PartitionNode<RoomData> partition = room.getPartitionNode();
		Point3D destination = partition.minCorner().clone();
		destination.add(
				offset.getX() + partition.width() / 2,
				offset.getY() + 2,
				offset.getZ() + partition.length() / 2);
		
		for (LinkPlan plan : room.getInboundLinks())
		{
			plan.setDestinationPoint(destination);
		}
	}
}
