package StevenDimDoors.experimental;

public class LinkPlan
{
	private RoomData source;
	private RoomData destination;
	private boolean entrance;
	
	private LinkPlan(RoomData source, RoomData destination, boolean entrance)
	{
		this.source = source;
		this.destination = destination;
		this.entrance = entrance;
	}
	
	public static LinkPlan createInternalLink(RoomData source, RoomData destination)
	{
		if (source == null)
		{
			throw new IllegalArgumentException("source cannot be null.");
		}
		if (destination == null)
		{
			throw new IllegalArgumentException("destination cannot be null.");
		}
		LinkPlan plan = new LinkPlan(source, destination, false);
		source.getOutboundLinks().add(plan);
		destination.getInboundLinks().add(plan);
		return plan;
	}
	
	public static LinkPlan createEntranceLink(RoomData source)
	{
		if (source == null)
		{
			throw new IllegalArgumentException("source cannot be null.");
		}
		LinkPlan plan = new LinkPlan(source, null, true);
		source.getOutboundLinks().add(plan);
		return plan;
	}
	
	public static LinkPlan createDungeonLink(RoomData source)
	{
		if (source == null)
		{
			throw new IllegalArgumentException("source cannot be null.");
		}
		LinkPlan plan = new LinkPlan(source, null, false);
		source.getOutboundLinks().add(plan);
		return plan;
	}
	
	public RoomData source()
	{
		return this.source;
	}
	
	public RoomData destination()
	{
		return this.destination;
	}
	
	public boolean isEntrance()
	{
		return entrance;
	}
	
	public boolean isInternal()
	{
		return (destination != null);
	}
	
	public void remove()
	{
		if (source != null)
		{
			source.getOutboundLinks().remove(this);
			source = null;
		}
		if (destination != null)
		{
			destination.getInboundLinks().remove(this);
			destination = null;
		}
	}
}
