package StevenDimDoors.experimental;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Random;
import java.util.Stack;

public class SectionData
{
	// Specifies the chance of selecting a destination from protectedRooms
	// rather than from destinationRooms (which will then become protected)
	private static final int PROTECTED_DESTINATION_CHANCE = 4;
	private static final int MAX_PROTECTED_DESTINATION_CHANCE = 5;
	
	private int capacity;
	private ArrayList<RoomData> sourceRooms;
	private ArrayList<RoomData> protectedRooms;
	private ArrayList<RoomData> destinationRooms;
	private ArrayList<LinkPlan> reservations; 
	
	private SectionData(ArrayList<RoomData> sourceRooms, ArrayList<RoomData> destinationRooms, int capacity)
	{
		this.capacity = capacity;
		this.sourceRooms = sourceRooms;
		this.destinationRooms = destinationRooms;
		this.protectedRooms = new ArrayList<RoomData>();
		this.reservations = new ArrayList<LinkPlan>();
	}
	
	public static SectionData createFromCore(IGraphNode<RoomData, DoorwayData> core)
	{
		int capacity = 0;
		ArrayList<RoomData> sourceRooms = new ArrayList<RoomData>();
		ArrayList<RoomData> destinationRooms = new ArrayList<RoomData>();
		
		boolean hasHoles;
		RoomData currentRoom;
		IGraphNode<RoomData, DoorwayData> current;
		IGraphNode<RoomData, DoorwayData> neighbor;
		Stack<IGraphNode<RoomData, DoorwayData>> ordering = new Stack<IGraphNode<RoomData, DoorwayData>>();
		HashSet<IGraphNode<RoomData, DoorwayData>> visited = new HashSet<IGraphNode<RoomData, DoorwayData>>();
		
		visited.add(core);
		ordering.add(core);
		while (!ordering.isEmpty())
		{
			current = ordering.pop();
			hasHoles = false;
			
			for (IEdge<RoomData, DoorwayData> edge : current.outbound())
			{
				neighbor = edge.tail();
				if (visited.add(neighbor))
				{
					ordering.add(neighbor);
				}
			}
			for (IEdge<RoomData, DoorwayData> edge : current.inbound())
			{
				neighbor = edge.head();
				if (visited.add(neighbor))
				{
					ordering.add(neighbor);
				}
				if (edge.data().axis() == DoorwayData.Y_AXIS)
				{
					hasHoles = true;
				}
			}
			
			if (!hasHoles)
			{
				currentRoom = current.data();
				destinationRooms.add(currentRoom);
				if (currentRoom.estimateDoorCapacity() > 0)
				{
					capacity += currentRoom.estimateDoorCapacity();
					sourceRooms.add(currentRoom);
				}
			}
		}
		return new SectionData(sourceRooms, destinationRooms, capacity);
	}
	
	public int capacity()
	{
		return capacity;
	}
	
	public void createEntranceLink(Random random)
	{
		int index = random.nextInt(sourceRooms.size());
		RoomData room = sourceRooms.get(index);
		LinkPlan.createEntranceLink(room);
		if (room.getRemainingDoorCapacity() == 0)
		{
			sourceRooms.remove(index);
		}
		// It's okay to check containment in this list because
		// the number of protected rooms is expected to be small
		if (!protectedRooms.contains(room))
		{
			protectedRooms.add(room);
		}
		capacity--;
	}

	public void createDungeonLink(Random random)
	{
		int index = random.nextInt(sourceRooms.size());
		RoomData room = sourceRooms.get(index);
		LinkPlan.createDungeonLink(room);
		if (room.getRemainingDoorCapacity() == 0)
		{
			sourceRooms.remove(index);
		}
		// It's okay to check containment in this list because
		// the number of protected rooms is expected to be small
		if (!protectedRooms.contains(room))
		{
			protectedRooms.add(room);
		}
		capacity--;
	}

	public void reserveSectionLink(SectionData destination, Random random)
	{
		// This method "reserves" a link by decrementing the capacity of this
		// section and assigning a source room to the link. However, assigning
		// its destination in a particular section is deferred. Why?
		
		// We favor using source rooms as destinations to cut down the number
		// of rooms that have to be marked as protected against decay effects.
		// We defer assigning a destination until after all source rooms are
		// known so that we have that information available. Otherwise,
		// destination selection would be biased toward non-source rooms and
		// rooms with dungeon doors, which are placed before section links.
		
		int index = random.nextInt(sourceRooms.size());
		RoomData room = sourceRooms.get(index);
		destination.reserveDestination(LinkPlan.createInternalLink(room));
		if (room.getRemainingDoorCapacity() == 0)
		{
			sourceRooms.remove(index);
		}
		// It's okay to check containment in this list because
		// the number of protected rooms is expected to be small
		if (!protectedRooms.contains(room))
		{
			protectedRooms.add(room);
		}
		capacity--;
	}

	public void processReservedLinks(Random random)
	{
		for (LinkPlan link : reservations)
		{
			link.setDestination( getLinkDestination(random) );
		}
		reservations.clear();
	}
	
	private void reserveDestination(LinkPlan link)
	{
		reservations.add(link);
	}
	
	private RoomData getLinkDestination(Random random)
	{
		RoomData destination;
		
		// Choose whether to select a room that is already protected or select
		// from all possible destination rooms. Note that some destination rooms
		// may also be protected rooms already.
		if (random.nextInt(MAX_PROTECTED_DESTINATION_CHANCE) < PROTECTED_DESTINATION_CHANCE)
		{
			destination = protectedRooms.get( random.nextInt(protectedRooms.size()) );
		}
		else
		{
			destination = destinationRooms.get( random.nextInt(destinationRooms.size()) );
			// It's okay to check containment in this list because
			// the number of protected rooms is expected to be small
			if (!protectedRooms.contains(destination))
			{
				protectedRooms.add(destination);
			}
		}
		return destination;
	}
}
