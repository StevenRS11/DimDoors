package StevenDimDoors.experimental;

import java.util.ArrayList;

public class MazeDesign
{
	private PartitionNode<RoomData> root;
	private DirectedGraph<RoomData, DoorwayData> layout;
	
	public MazeDesign(PartitionNode<RoomData> root, DirectedGraph<RoomData, DoorwayData> layout)
	{
		this.root = root;
		this.layout = layout;
	}

	public PartitionNode<RoomData> getRootPartition()
	{
		return root;
	}

	public DirectedGraph<RoomData, DoorwayData> getLayout()
	{
		return layout;
	}
	
	public int width()
	{
		return root.width();
	}
	
	public int height()
	{
		return root.height();
	}
	
	public int length()
	{
		return root.length();
	}
}
