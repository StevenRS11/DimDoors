package StevenDimDoors.experimental;

import java.util.ArrayList;

public class RoomData
{
	private int distance;
	private boolean decayed;
	private PartitionNode partitionNode;
	private ArrayList<MazeLinkData> inboundLinks;
	private ArrayList<MazeLinkData> outboundLinks;
	private IGraphNode<RoomData, DoorwayData> layoutNode;
	
	public RoomData(PartitionNode partitionNode)
	{
		this.partitionNode = partitionNode;
		this.inboundLinks = new ArrayList<MazeLinkData>();
		this.outboundLinks = new ArrayList<MazeLinkData>();
		this.layoutNode = null;
		this.distance = -1;
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
	
	public ArrayList<MazeLinkData> getInboundLinks()
	{
		return this.inboundLinks;
	}
	
	public ArrayList<MazeLinkData> getOutboundLinks()
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
	
	public void clear()
	{
		partitionNode = null;
		layoutNode = null;
	}

	public int estimateDoorCapacity()
	{
		int cellsX = (partitionNode.width() - 3) / 2;
		int cellsZ = (partitionNode.length() - 3) / 2;
		return Math.min(cellsX * cellsZ, 3);
	}
}
