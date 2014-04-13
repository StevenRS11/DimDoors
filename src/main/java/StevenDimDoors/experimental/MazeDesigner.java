package StevenDimDoors.experimental;

import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.Queue;
import java.util.Random;
import java.util.Stack;

import net.minecraft.util.MathHelper;
import StevenDimDoors.mod_pocketDim.Point3D;

public class MazeDesigner
{
	private static final int MAZE_WIDTH = 34;
	private static final int MAZE_LENGTH = 34;
	private static final int MAZE_HEIGHT = 20;
	private static final int MIN_HEIGHT = 4;
	private static final int MIN_SIDE = 3;
	private static final int SPLIT_COUNT = 9;
	
	private MazeDesigner() { }
	
	public static MazeDesign generate(Random random)
	{
		// Construct a random binary space partitioning of our maze volume
		PartitionNode<RoomData> root = partitionRooms(MAZE_WIDTH, MAZE_HEIGHT, MAZE_LENGTH, SPLIT_COUNT, random);

		// Attach rooms to all the leaf nodes of the partition tree
		ArrayList<RoomData> rooms = new ArrayList<RoomData>(1 << SPLIT_COUNT);
		attachRooms(root, rooms);
		
		// Shuffle the list of rooms so that they're not listed in any ordered way in the room graph
		// This is the only convenient way of randomizing the maze sections generated later
		Collections.shuffle(rooms, random);
		
		// Construct an adjacency graph of the rooms we've carved out. Two rooms are
		// considered adjacent if and only if a doorway could connect them. Their
		// common boundary must be large enough for a doorway.
		DirectedGraph<RoomData, DoorwayData> layout = createRoomGraph(root, rooms, random);
		
		// Cut out random subgraphs from the adjacency graph
		ArrayList<RoomData> cores = createMazeSections(layout, random);
		
		// Remove unnecessary passages through floors/ceilings and some from the walls
		for (RoomData core : cores)
		{
			pruneDoorways(core.getLayoutNode(), layout, random);
		}
		
		// Set up the placement of dimensional doors within the maze
		createMazeLinks(layout, cores, random);
		
		return new MazeDesign(root, layout);
	}

	private static void attachRooms(PartitionNode<RoomData> node, ArrayList<RoomData> partitions)
	{
		if (node.isLeaf())
		{
			partitions.add(new RoomData(node));
		}
		else
		{
			attachRooms(node.leftChild(), partitions);
			attachRooms(node.rightChild(), partitions);
		}
	}
	
	private static PartitionNode partitionRooms(int width, int height, int length, int maxLevels, Random random)
	{
		PartitionNode root = new PartitionNode(width, height, length);
		splitByRandomX(root, maxLevels, random);
		return root;
	}
	
	private static void splitByRandomX(PartitionNode node, int levels, Random random)
	{
		if (node.width() >= 2 * MIN_SIDE)
		{
			node.splitByX(MathHelper.getRandomIntegerInRange(random,
					node.minCorner().getX() + MIN_SIDE, node.maxCorner().getX() - MIN_SIDE + 1));

			if (levels > 1)
			{
				splitByRandomZ(node.leftChild(), levels - 1, random);
				splitByRandomZ(node.rightChild(), levels - 1, random);
			}
		}
		else if (levels > 1)
		{
			splitByRandomZ(node, levels - 1, random);
		}
	}
	
	private static void splitByRandomZ(PartitionNode node, int levels, Random random)
	{
		if (node.length() >= 2 * MIN_SIDE)
		{
			node.splitByZ(MathHelper.getRandomIntegerInRange(random,
					node.minCorner().getZ() + MIN_SIDE, node.maxCorner().getZ() - MIN_SIDE + 1));

			if (levels > 1)
			{
				splitByRandomY(node.leftChild(), levels - 1, random);
				splitByRandomY(node.rightChild(), levels - 1, random);
			}
		}
		else if (levels > 1)
		{
			splitByRandomY(node, levels - 1, random);
		}
	}
	
	private static void splitByRandomY(PartitionNode node, int levels, Random random)
	{
		if (node.height() >= 2 * MIN_HEIGHT)
		{
			node.splitByY(MathHelper.getRandomIntegerInRange(random,
					node.minCorner().getY() + MIN_HEIGHT, node.maxCorner().getY() - MIN_HEIGHT + 1));

			if (levels > 1)
			{
				splitByRandomX(node.leftChild(), levels - 1, random);
				splitByRandomX(node.rightChild(), levels - 1, random);
			}
		}
		else if (levels > 1)
		{
			splitByRandomX(node, levels - 1, random);
		}
	}
	
	private static DirectedGraph<RoomData, DoorwayData> createRoomGraph(PartitionNode<RoomData> root, ArrayList<RoomData> rooms, Random random)
	{
		DirectedGraph<RoomData, DoorwayData> layout = new DirectedGraph<RoomData, DoorwayData>();
		
		// Add all rooms to a graph
		for (RoomData room : rooms)
		{
			room.addToLayout(layout);
		}
		// Add edges for each room
		for (IGraphNode<RoomData, DoorwayData> node : layout.nodes())
		{
			findDoorways(node.data(), root, layout);
		}
		return layout;
	}
	
	private static void findDoorways(RoomData room, PartitionNode<RoomData> root,
			DirectedGraph<RoomData, DoorwayData> layout)
	{
		// This function finds rooms adjacent to a specified room that could be connected
		// to it through a doorway. Edges are added to the room graph to denote rooms that
		// could be connected. The areas of their common bounds that could be carved
		// out for a passage are stored in the edges.
		
		// Three directions have to be checked: up, forward, and right. The other three
		// directions (down, back, left) aren't checked because other nodes will cover them.
		// That is, down for this room is up for some other room, if it exists. Also, rooms
		// are guaranteed to have at least one doorway to another room, because the minimum
		// dimensions to which a room can be partitioned still allow passages along all
		// its sides. A room's sibling in the partition tree is guaranteed to share a side
		// through which a doorway could exist. Similar arguments guarantee the existence
		// of passages such that the whole set of rooms is a connected graph - in other words,
		// there will always be a way to walk from any room to any other room.
		
		boolean[][] detected;
		PartitionNode<RoomData> adjacent;
		
		int a, b, c;
		int p, q, r;
		int minXI, minYI, minZI;
		int maxXI, maxYI, maxZI;
		Point3D otherMin;
		Point3D otherMax;
		DoorwayData doorway;
		
		PartitionNode partition = room.getPartitionNode();
		Point3D minCorner = partition.minCorner();
		Point3D maxCorner = partition.maxCorner();
		
		int minX = minCorner.getX();
		int minY = minCorner.getY();
		int minZ = minCorner.getZ();
		
		int maxX = maxCorner.getX();
		int maxY = maxCorner.getY();
		int maxZ = maxCorner.getZ();
		
		int width = partition.width();
		int height = partition.height();
		int length = partition.length();
		
		if (maxZ < root.maxCorner().getZ())
		{
			// Check for adjacent rooms along the XY plane
			detected = new boolean[width][height];
			for (a = 0; a < width; a++)
			{
				for (b = 0; b < height; b++)
				{
					if (!detected[a][b])
					{
						adjacent = root.findPoint(minX + a, minY + b, maxZ + 1);
						if (adjacent != null)
						{
							// Compute the dimensions available for a doorway
							otherMin = adjacent.minCorner();
							otherMax = adjacent.maxCorner();
							minXI = Math.max(minX, otherMin.getX());
							maxXI = Math.min(maxX, otherMax.getX());
							minYI = Math.max(minY, otherMin.getY());
							maxYI = Math.min(maxY, otherMax.getY());
							
							for (p = 0; p <= maxXI - minXI; p++)
							{
								for (q = 0; q <= maxYI - minYI; q++)
								{
									detected[p + a][q + b] = true;
								}	
							}
							// Check if we meet the minimum dimensions needed for a doorway
							if (maxXI - minXI + 1 >= MIN_SIDE && maxYI - minYI + 1 >= MIN_HEIGHT)
							{
								otherMin = new Point3D(minXI, minYI, maxZ);
								otherMax = new Point3D(maxXI, maxYI, maxZ + 1);
								doorway = new DoorwayData(otherMin, otherMax, DoorwayData.Z_AXIS);
								layout.addEdge(room.getLayoutNode(), adjacent.getData().getLayoutNode(), doorway);
							}
						}
						else
						{
							detected[a][b] = true;
						}
					}
				}
			}
		}
		
		
		if (maxX < root.maxCorner().getX())
		{
			// Check for adjacent rooms along the YZ plane
			detected = new boolean[height][length];
			for (b = 0; b < height; b++)
			{
				for (c = 0; c < length; c++)
				{
					if (!detected[b][c])
					{
						adjacent = root.findPoint(maxX + 1, minY + b, minZ + c);
						if (adjacent != null)
						{
							// Compute the dimensions available for a doorway
							otherMin = adjacent.minCorner();
							otherMax = adjacent.maxCorner();
							minYI = Math.max(minY, otherMin.getY());
							maxYI = Math.min(maxY, otherMax.getY());
							minZI = Math.max(minZ, otherMin.getZ());
							maxZI = Math.min(maxZ, otherMax.getZ());
							
							for (q = 0; q <= maxYI - minYI; q++)
							{
								for (r = 0; r <= maxZI - minZI; r++)
								{
									detected[q + b][r + c] = true;
								}	
							}
							// Check if we meet the minimum dimensions needed for a doorway
							if (maxYI - minYI + 1 >= MIN_HEIGHT && maxZI - minZI + 1 >= MIN_SIDE)
							{
								otherMin = new Point3D(maxX, minYI, minZI);
								otherMax = new Point3D(maxX + 1, maxYI, maxZI);
								doorway = new DoorwayData(otherMin, otherMax, DoorwayData.X_AXIS);
								layout.addEdge(room.getLayoutNode(), adjacent.getData().getLayoutNode(), doorway);
							}
						}
						else
						{
							detected[b][c] = true;
						}
					}
				}
			}
		}
		
		
		if (maxY < root.maxCorner().getY())
		{
			// Check for adjacent rooms along the XZ plane
			detected = new boolean[width][length];
			for (a = 0; a < width; a++)
			{
				for (c = 0; c < length; c++)
				{
					if (!detected[a][c])
					{
						adjacent = root.findPoint(minX + a, maxY + 1, minZ + c);
						if (adjacent != null)
						{
							// Compute the dimensions available for a doorway
							otherMin = adjacent.minCorner();
							otherMax = adjacent.maxCorner();
							minXI = Math.max(minX, otherMin.getX());
							maxXI = Math.min(maxX, otherMax.getX());
							minZI = Math.max(minZ, otherMin.getZ());
							maxZI = Math.min(maxZ, otherMax.getZ());
							
							for (p = 0; p <= maxXI - minXI; p++)
							{
								for (r = 0; r <= maxZI - minZI; r++)
								{
									detected[p + a][r + c] = true;
								}	
							}
							// Check if we meet the minimum dimensions needed for a doorway
							if (maxXI - minXI + 1 >= MIN_SIDE && maxZI - minZI + 1 >= MIN_SIDE)
							{
								otherMin = new Point3D(minXI, maxY, minZI);
								otherMax = new Point3D(maxXI, maxY + 1, maxZI);
								doorway = new DoorwayData(otherMin, otherMax, DoorwayData.Y_AXIS);
								layout.addEdge(room.getLayoutNode(), adjacent.getData().getLayoutNode(), doorway);
							}
						}
						else
						{
							detected[a][c] = true;
						}
					}
				}
			}
		}
		
		//Done!
	}
	
	private static ArrayList<RoomData> createMazeSections(DirectedGraph<RoomData, DoorwayData> layout, Random random)
	{
		// The randomness of the sections generated here hinges on
		// the nodes in the graph being in a random order. We assume
		// that was handled in a previous step!
		
		// We split the maze into sections by choosing core rooms and removing
		// rooms that are a certain number of doorways away. However, for a section
		// to be valid, it must also have enough space for at least two doors in
		// rooms without floor holes. If a section can't fit two doors, more
		// neighboring rooms are added until the necessary space is found or the
		// search space is exhausted.
		
		final int MAX_DISTANCE = 2 + random.nextInt(2);
		final int MIN_SECTION_ROOMS = 5;
		final int MIN_SECTION_CAPACITY = 2;
		
		int distance;
		int capacity;
		RoomData room;
		RoomData neighbor;
		boolean hasHoles;
		IGraphNode<RoomData, DoorwayData> roomNode;
		
		ArrayList<RoomData> cores = new ArrayList<RoomData>();
		ArrayList<RoomData> section = new ArrayList<RoomData>();
		ArrayList<IGraphNode<RoomData, DoorwayData>> nodes = new ArrayList<IGraphNode<RoomData, DoorwayData>>(layout.nodeCount());
		
		Queue<RoomData> ordering = new LinkedList<RoomData>();
		
		// List all graph nodes so that we can iterate over this list instead
		// of using the graph's iterator. That avoids the risk of breaking
		// the graph's iterator during removals.
		for (IGraphNode<RoomData, DoorwayData> node : layout.nodes())
		{
			nodes.add(node);
		}
		
		// Repeatedly generate sections until all nodes have been visited
		for (IGraphNode<RoomData, DoorwayData> node : nodes)
		{
			// If this room hasn't been visited (distance = -1), then use it as the core of a new section
			// Otherwise, ignore it, since it was already processed. Also make sure to check that room
			// isn't null, which happens if the room was removed previously.
			room = node.data();
			if (room != null && room.getDistance() < 0)
			{
				// Perform a breadth-first search to tag surrounding nodes with distances
				ordering.add(room);
				room.setDistance(0);
				section.clear();
				capacity = 0;
				
				while (room != null && (room.getDistance() <= MAX_DISTANCE || capacity < 2))
				{
					ordering.remove();
					section.add(room);
					roomNode = room.getLayoutNode();
					distance = room.getDistance() + 1;
					hasHoles = false;
					
					// Visit neighboring rooms and assign them distances,
					// if they don't have a proper distance assigned already.
					// Also check for floor holes.
					for (IEdge<RoomData, DoorwayData> edge : roomNode.inbound())
					{
						neighbor = edge.head().data();
						if (neighbor.getDistance() < 0)
						{
							neighbor.setDistance(distance);
							ordering.add(neighbor);
						}
						if (edge.data().axis() == DoorwayData.Y_AXIS)
						{
							hasHoles = true;
						}
					}
					for (IEdge<RoomData, DoorwayData> edge : roomNode.outbound())
					{
						neighbor = edge.tail().data();
						if (neighbor.getDistance() < 0)
						{
							neighbor.setDistance(distance);
							ordering.add(neighbor);
						}
					}
					
					// Count this room's door capacity if it has no floor holes
					if (!hasHoles)
					{
						capacity += room.estimateDoorCapacity();
					}
					
					room = ordering.peek();
				}
				
				// The remaining rooms in the ordering are those that are at the
				// frontier of structure. They must be removed to create a gap
				// between this section and other sections.
				while (!ordering.isEmpty())
				{
					ordering.remove().remove();
				}
				
				// Check if this section contains enough rooms and capacity for doors
				if (section.size() >= MIN_SECTION_ROOMS && capacity >= MIN_SECTION_CAPACITY)
				{
					cores.add(node.data());
				}
				else
				{
					// Discard the whole section
					for (RoomData target : section)
					{
						target.remove();
					}
				}
			}
		}
		return cores;
	}
	
	private static void pruneDoorways(IGraphNode<RoomData, DoorwayData> core,
			DirectedGraph<RoomData, DoorwayData> layout, Random random)
	{
		// We receive a node for one of the rooms in a section of the maze
		// and we need to remove as many floor doorways as possible while
		// still allowing any room to be reachable from any other room.
		// In technical terms, we receive a node from a connected subgraph
		// and we need to remove as many Y_AXIS-type edges as possible while
		// preserving connectedness. We also want to randomly remove some of
		// the other doorways without breaking connectedness.
		
		// An efficient solution is to assign nodes to disjoint sets based
		// on their components, ignoring all Y_AXIS edges, then iterate over
		// a list of those edges and remove them if they connect two nodes
		// in the same set. Otherwise, merge their sets and keep the edge.
		// This is similar to algorithms for spanning trees. The same
		// idea applies for the other doorways, plus some randomness.
		
		// First, list all nodes in the subgraph
		IGraphNode<RoomData, DoorwayData> current;
		IGraphNode<RoomData, DoorwayData> neighbor;
		
		Stack<IGraphNode<RoomData, DoorwayData>> ordering = new Stack<IGraphNode<RoomData, DoorwayData>>();
		ArrayList<IGraphNode<RoomData, DoorwayData>> subgraph = new ArrayList<IGraphNode<RoomData, DoorwayData>>(64);
		DisjointSet<IGraphNode<RoomData, DoorwayData>> components = new DisjointSet<IGraphNode<RoomData, DoorwayData>>(128);
		
		ordering.add(core);
		components.makeSet(core);
		while (!ordering.isEmpty())
		{
			current = ordering.pop();
			subgraph.add(current);
			
			for (IEdge<RoomData, DoorwayData> edge : current.inbound())
			{
				neighbor = edge.head();
				if (components.makeSet(neighbor))
				{
					ordering.add(neighbor);
				}
			}
			for (IEdge<RoomData, DoorwayData> edge : current.outbound())
			{
				neighbor = edge.tail();
				if (components.makeSet(neighbor))
				{
					ordering.add(neighbor);
				}
			}
		}
		
		// Now iterate over the list of nodes and merge their sets based on
		// being connected by X_AXIS or Z_AXIS doorways. We only have to look
		// at outbound edges since inbound edges mirror them. List any Y_AXIS
		// doorways we come across to consider removing them later, depending
		// on their impact on connectedness.
		ArrayList<IEdge<RoomData, DoorwayData>> targets =
				new ArrayList<IEdge<RoomData, DoorwayData>>();
		
		for (IGraphNode<RoomData, DoorwayData> room : subgraph)
		{
			for (IEdge<RoomData, DoorwayData> passage : room.outbound())
			{
				if (passage.data().axis() != DoorwayData.Y_AXIS)
				{
					components.mergeSets(passage.head(), passage.tail());
				}
				else
				{
					targets.add(passage);
				}
			}
		}
		
		// Shuffle the list of doorways to randomize which ones are removed
		Collections.shuffle(targets, random);
		
		// Merge sets together and remove unnecessary doorways
		for (IEdge<RoomData, DoorwayData> passage : targets)
		{
			if (!components.mergeSets(passage.head(), passage.tail()))
			{
				layout.removeEdge(passage);
				
			}
		}
		
		// Repeat the pruning process with X_AXIS and Z_AXIS doorways
		// In this case, unnecessary edges might be kept at random
		components.clear();
		targets.clear();
		
		for (IGraphNode<RoomData, DoorwayData> room : subgraph)
		{
			components.makeSet(room);
		}
		for (IGraphNode<RoomData, DoorwayData> room : subgraph)
		{
			for (IEdge<RoomData, DoorwayData> passage : room.outbound())
			{
				if (passage.data().axis() == DoorwayData.Y_AXIS)
				{
					components.mergeSets(passage.head(), passage.tail());
				}
				else
				{
					targets.add(passage);
				}
			}
		}
		Collections.shuffle(targets, random);
		for (IEdge<RoomData, DoorwayData> passage : targets)
		{
			if (!components.mergeSets(passage.head(), passage.tail()) && random.nextBoolean())
			{
				layout.removeEdge(passage);
			}
		}
	}
	
	private static void createMazeLinks(DirectedGraph<RoomData, DoorwayData> layout,
			ArrayList<RoomData> cores, Random random)
	{
		// We have 4 objectives here...
		// 1. Place the entrance to the maze
		// 2. Place links to other dungeons
		// 3. Place internal links connecting the different sections of the maze
		// 4. Place more internal links to confuse people
		
		// We need to start by counting the door capacity of each section and
		// listing which rooms can have doors or destinations for each section.
		int index;
		int[] capacity = new int[cores.size()];
		ArrayList<RoomData>[] sourceRooms = (ArrayList<RoomData>[]) Array.newInstance(cores.getClass(), cores.size());
		ArrayList<RoomData>[] destinationRooms = (ArrayList<RoomData>[]) Array.newInstance(cores.getClass(), cores.size());

		for (index = 0; index < sourceRooms.length; index++)
		{
			sourceRooms[index] = new ArrayList<RoomData>();
			destinationRooms[index] = new ArrayList<RoomData>();
			capacity[index] = listLinkRooms(cores.get(index).getLayoutNode(), sourceRooms[index], destinationRooms[index]);
		}
		
		// Now we select the room in which to place the entrance.
		// We can safely assume all source room lists are non-empty because
		// createMazeSections() guarantees that each section has at least
		// the capacity for 2 doors.
		index = random.nextInt(sourceRooms.length);
		createEntranceLink(sourceRooms[index], random.nextInt(sourceRooms[index].size()));
		
		// The next task is to place internal links. These links must connect
		// the different maze sections to create a strongly connected graph.
		
		
	}
	
	private static int listLinkRooms(IGraphNode<RoomData, DoorwayData> core,
			ArrayList<RoomData> sourceRooms, ArrayList<RoomData> destinationRooms)
	{
		int capacity = 0;
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
		return capacity;
	}
	
	private static void createEntranceLink(ArrayList<RoomData> sources, int index)
	{
		RoomData entranceRoom = sources.get(index);
		LinkPlan.createEntranceLink(entranceRoom);
		if (entranceRoom.getRemainingDoorCapacity() == 0)
		{
			sources.remove(index);
		}
	}
}
