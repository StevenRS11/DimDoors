package StevenDimDoors.experimental;

import StevenDimDoors.mod_pocketDim.Point3D;

public class LinkPlan
{
	private RoomData source;
	private RoomData destination;
	private Point3D sourcePoint;
	private Point3D destinationPoint;
	private final boolean entrance;
	private final boolean internal;
	
	private LinkPlan(RoomData source, boolean entrance, boolean internal)
	{
		if (source == null)
		{
			throw new IllegalArgumentException("source cannot be null.");
		}
		this.source = source;
		this.destination = null;
		this.sourcePoint = null;
		this.destinationPoint = null;
		this.entrance = entrance;
		this.internal = internal;
	}
	
	public static LinkPlan createInternalLink(RoomData source)
	{
		LinkPlan plan = new LinkPlan(source, false, true);
		source.getOutboundLinks().add(plan);
		return plan;
	}
	
	public static LinkPlan createEntranceLink(RoomData source)
	{
		LinkPlan plan = new LinkPlan(source, true, false);
		source.getOutboundLinks().add(plan);
		return plan;
	}
	
	public static LinkPlan createDungeonLink(RoomData source)
	{
		LinkPlan plan = new LinkPlan(source, false, false);
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
		return internal;
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

	public void setDestination(RoomData destination)
	{
		if (!internal)
		{
			throw new IllegalStateException("LinkPlan.setDestination() is only applicable to internal links.");
		}
		if (this.destination != null)
		{
			throw new IllegalStateException("destination can only be set once.");
		}
		if (destination == null)
		{
			throw new IllegalArgumentException("destination cannot be null.");
		}
		this.destination = destination;
		destination.getInboundLinks().add(this);
	}
	
	public Point3D sourcePoint()
	{
		return this.sourcePoint;
	}
	
	public Point3D destinationPoint()
	{
		return this.destinationPoint;
	}
	
	public void setSourcePoint(Point3D value)
	{
		this.sourcePoint = value;
	}
	
	public void setDestinationPoint(Point3D value)
	{
		this.destinationPoint = value;
	}
}
