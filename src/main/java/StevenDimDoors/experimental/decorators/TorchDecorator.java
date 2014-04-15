package StevenDimDoors.experimental.decorators;

import java.util.Random;

import net.minecraft.block.Block;
import net.minecraft.util.MathHelper;
import net.minecraft.world.World;

import StevenDimDoors.experimental.PartitionNode;
import StevenDimDoors.experimental.RoomData;
import StevenDimDoors.mod_pocketDim.Point3D;
import StevenDimDoors.mod_pocketDim.config.DDProperties;

public class TorchDecorator extends BaseDecorator
{
	@Override
	public boolean canDecorate(RoomData room)
	{
		return !room.isProtected();
	}

	@Override
	public void decorate(RoomData room, World world, Point3D offset, Random random, DDProperties properties)
	{
		// SenseiKiwi: Place a single random torch along the walls.
		// We could do more complex arrangements but I feel that a single
		// torches here and there will be a little unsettling.
		// The walls might be broken by passages or decay, so this will
		// require trial and error.
		
		final int MAX_ATTEMPTS = 5;
		
		int x;
		int z;
		int attempts = 0;
		PartitionNode<RoomData> partition = room.getPartitionNode();
		int minX = partition.minCorner().getX() + offset.getX();
		int minZ = partition.minCorner().getZ() + offset.getZ();
		int maxX = partition.maxCorner().getX() + offset.getX();
		int maxZ = partition.maxCorner().getZ() + offset.getZ();
		int torchLevel = partition.minCorner().getY() + offset.getY() + 2;
		
		for (; attempts < MAX_ATTEMPTS; attempts++)
		{
			// Choose a random side of the room to place the torch. The sides are numbered arbitrarily here.
			// Then choose a random position along the wall and check if there is a block there to place the
			// torch against. We assume that all blocks are bricks and thus valid.
			switch (random.nextInt(4))
			{
				case 0: // Positive X side
					z = MathHelper.getRandomIntegerInRange(random, minZ + 1, maxZ - 1);
					if (!world.isAirBlock(maxX, torchLevel, z))
					{
						world.setBlock(maxX - 1, torchLevel, z, Block.torchWood.blockID, 2, 0);
						return;
					}
					break;
				case 1: // Negative X side
					z = MathHelper.getRandomIntegerInRange(random, minZ + 1, maxZ - 1);
					if (!world.isAirBlock(minX, torchLevel, z))
					{
						world.setBlock(minX + 1, torchLevel, z, Block.torchWood.blockID, 1, 0);
						return;
					}
					break;
				case 2: // Positive Z side
					x = MathHelper.getRandomIntegerInRange(random, minX + 1, maxX - 1);
					if (!world.isAirBlock(x, torchLevel, maxZ))
					{
						world.setBlock(x, torchLevel, maxZ - 1, Block.torchWood.blockID, 4, 0);
						return;
					}
					break;
				case 3: // Negative Z side
					x = MathHelper.getRandomIntegerInRange(random, minX + 1, maxX - 1);
					if (!world.isAirBlock(x, torchLevel, minZ))
					{
						world.setBlock(x, torchLevel, minZ + 1, Block.torchWood.blockID, 3, 0);
						return;
					}
					break;
			}
		}
	}

}
