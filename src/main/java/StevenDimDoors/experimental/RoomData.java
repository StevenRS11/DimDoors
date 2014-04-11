package StevenDimDoors.experimental;

import java.util.ArrayList;

public class RoomData
{
	/* Implementation Note:
	 * Plans for links between rooms are stored in lists rather than a graph,
	 * even though they duplicate some graph functionality, because there are
	 * relatively few of them compared to the number of rooms. Moreover, some
	 * links don't even have destinations because they're intended to lead to
	 * other dungeons.
	 */
	
	private int capacity;
	private int distance;
	private boolean decayed;
	private PartitionNode partitionNode;
	private ArrayList<LinkPlan> inboundLinks;
	private ArrayList<LinkPlan> outboundLinks;
	private DirectedGraph<RoomData, DoorwayData> layout;
	private IGraphNode<RoomData, DoorwayData> layoutNode;
	
	public RoomData(PartitionNode partitionNode)
	{
		this.partitionNode = partitionNode;
		this.inboundLinks = new ArrayList<LinkPlan>();
		this.outboundLinks = new ArrayList<LinkPlan>();
		this.layoutNode = null;
		this.layout = null;
		this.distance = -1;
		this.capacity = -1;
		this.decayed = false;
		partitionNode.setData(this);
	}
	
	public PartitionNode getPartitionNode()
	{
		return this.partitionNode;
	}
	
	public IGraphNode<RoomData, DoorwayData> getLayoutNode()
	{
		return this.layoutNode;
	}
	
	public void addToLayout(DirectedGraph<RoomData, DoorwayData> layout)
	{
		this.layout = layout;
		this.layoutNode = layout.addNode(this);
	}
	
	public boolean isDecayed()
	{
		return decayed;
	}
	
	public void setDecayed(boolean value)
	{
		this.decayed = value;
	}
	
	public ArrayList<LinkPlan> getInboundLinks()
	{
		return this.inboundLinks;
	}
	
	public ArrayList<LinkPlan> getOutboundLinks()
	{
		return this.outboundLinks;
	}
	
	public int getDistance()
	{
		return distance;
	}
	
	public void setDistance(int value)
	{
		distance = value;
	}
	
	public void remove()
	{
		// Remove the room from the partition tree and from the layout graph.
		// Also remove any ancestors that become leaf nodes.
		PartitionNode parent;
		PartitionNode current = partitionNode;
		while (current != null && current.isLeaf())
		{
			parent = current.parent();
			current.remove();
			current = parent;
		}
		
		// Remove the room from the layout graph
		layout.removeNode(layoutNode);
		
		// Remove any links
		while (!inboundLinks.isEmpty())
			inboundLinks.get(inboundLinks.size() - 1).remove();

		while (!outboundLinks.isEmpty())
			outboundLinks.get(outboundLinks.size() - 1).remove();
		
		// Wipe the room's data, as a precaution
		layout = null;
		partitionNode = null;
		inboundLinks = null;
		outboundLinks = null;
	}

	public int estimateDoorCapacity()
	{
		if (capacity >= 0)
			return capacity;
		
		int cellsX = (partitionNode.width() - 3) / 2;
		int cellsZ = (partitionNode.length() - 3) / 2;
		capacity = Math.min(cellsX * cellsZ, 3);
		return capacity;
	}
	
	public int getRemainingDoorCapacity()
	{
		return (estimateDoorCapacity() - outboundLinks.size());
	}
	
	public boolean isProtected()
	{
		return !inboundLinks.isEmpty() || !outboundLinks.isEmpty();
	}
}
