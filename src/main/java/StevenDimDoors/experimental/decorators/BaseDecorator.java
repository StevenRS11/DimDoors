package StevenDimDoors.experimental.decorators;

import java.util.Random;

import StevenDimDoors.experimental.RoomData;
import StevenDimDoors.mod_pocketDim.config.DDProperties;

public abstract class BaseDecorator
{
	public BaseDecorator() { }
	
	public abstract boolean canDecorate(RoomData room);
	
	public abstract boolean decorate(RoomData room, Random random, DDProperties properties);
}
