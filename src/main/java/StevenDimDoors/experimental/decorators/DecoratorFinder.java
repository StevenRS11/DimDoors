package StevenDimDoors.experimental.decorators;

import java.util.ArrayList;
import java.util.Random;

import StevenDimDoors.experimental.RoomData;

public class DecoratorFinder
{
	private static ArrayList<BaseDecorator> decorators = null;
	
	private DecoratorFinder() { }
	
	public static BaseDecorator find(RoomData room, Random random)
	{
		if (decorators == null)
		{
			load();
		}
		
		// Since there are only a few decorators right now, we just iterate
		// over the list and check them all. If we add a lot, we'll need to
		// switch to a more efficient approach.
		ArrayList<BaseDecorator> matches = new ArrayList<BaseDecorator>();
		for (BaseDecorator decorator : decorators)
		{
			if (decorator.canDecorate(room))
			{
				matches.add(decorator);
			}
		}
		
		if (matches.isEmpty())
		{
			return null;
		}
		else
		{
			return matches.get( random.nextInt(matches.size()) );
		}
	}
	
	private static void load()
	{
		// List all the decorators we have
		decorators = new ArrayList<BaseDecorator>();
		decorators.add(new LinkDestinationDecorator());
		decorators.add(new DefaultDoorDecorator());
		decorators.add(new TorchDecorator());
	}
}
