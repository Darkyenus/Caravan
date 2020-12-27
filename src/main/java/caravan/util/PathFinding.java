package caravan.util;

import com.badlogic.gdx.utils.BinaryHeap;
import com.badlogic.gdx.utils.LongArray;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import static caravan.util.Vec2.x;
import static caravan.util.Vec2.y;

/**
 * Fast A* path finding algorithm implementation.
 */
public final class PathFinding {

	private final int height;
	private final @NotNull PathWorld world;

	private final @Nullable NodeRecord[] nodeRecords;
	private final BinaryHeap<NodeRecord> openList = new BinaryHeap<>();
	/** The unique ID for each search run. Used to mark nodes.  */
	private int searchId = 0;

	public PathFinding(int width, int height, @NotNull PathWorld world) {
		this.height = height;
		this.world = world;
		nodeRecords = new NodeRecord[width * height];
	}

	public @Nullable Path findPath(long from, long to, @NotNull LongArray endPositions) {
		initSearch(from, to);
		final BinaryHeap<NodeRecord> openList = this.openList;
		do {
			// Retrieve the node with smallest estimated total cost from the open list
			final NodeRecord current = openList.pop();
			current.category = CLOSED;
			// Terminate if we reached the goal node
			if (endPositions.contains(current.node)) {
				return generateNodePath(current.node);
			}
			visitChildren(current, to);
		} while (openList.size > 0);
		// We've run out of nodes without finding the goal, so there's no solution
		return null;
	}

	public @Nullable Path findPathInTimeLimit(long from, long to, @NotNull LongArray endPositions, long maxTimeNanos) {
		final long endTime = System.nanoTime() + maxTimeNanos;

		initSearch(from, to);
		final BinaryHeap<NodeRecord> openList = this.openList;
		int iteration = 0;
		do {
			// Retrieve the node with smallest estimated total cost from the open list
			final NodeRecord current = openList.pop();
			current.category = CLOSED;
			// Terminate if we reached the goal node
			if (endPositions.contains(current.node)) {
				return generateNodePath(current.node);
			}

			if ((++iteration & 0b1111) == 0 && System.nanoTime() >= endTime) {
				// Timed out
				return null;
			}

			visitChildren(current, to);
		} while (openList.size > 0);
		// We've run out of nodes without finding the goal, so there's no solution
		return null;
	}

	public @Nullable Path findPathWithMaxComplexity(long from, long to, @NotNull LongArray endPositions, float maxComplexityCostFactor) {
		final float maxCost = estimateDistance(from, to) * maxComplexityCostFactor;

		initSearch(from, to);
		final BinaryHeap<NodeRecord> openList = this.openList;
		do {
			// Retrieve the node with smallest estimated total cost from the open list
			final NodeRecord current = openList.pop();
			current.category = CLOSED;
			// Terminate if we reached the goal node
			if (endPositions.contains(current.node)) {
				return generateNodePath(current.node);
			}

			if (current.costSoFar > maxCost) {
				// Too costly to find
				return null;
			}

			visitChildren(current, to);
		} while (openList.size > 0);
		// We've run out of nodes without finding the goal, so there's no solution
		return null;
	}

	@SuppressWarnings("ConstantConditions")
	private @NotNull Path generateNodePath(long endNode) {
		final PathImpl outPath = new PathImpl();

		// Work back along the path, accumulating nodes
		@NotNull NodeRecord current = nodeRecords[graphIndex(endNode)];
		while (current.from != Vec2.NULL) {
			outPath.add(current.node);
			current = nodeRecords[graphIndex(current.from)];
		}
		// Reverse the path
		outPath.reverse();
		return outPath;
	}

	private void initSearch(long startNode, long endNode) {
		// Increment the search id
		searchId++;
		// Initialize the open list
		openList.clear();
		// Initialize the record for the start node and add it to the open list
		final NodeRecord startRecord = getNodeRecord(startNode);
		startRecord.from = Vec2.NULL;
		startRecord.costSoFar = 0f;
		addToOpenList(startRecord, estimateDistance(startNode, endNode));
	}

	private void visitChildren(NodeRecord current, long endNode) {
		final long from = current.node;
		final float cost = 1f / world.movementSpeedMultiplier(x(from), y(from));

		for (long direction : Vec2.DIRECTIONS) {
			final long to = Vec2.plus(from, direction);
			if (!world.isAccessible(x(to), y(to))) {
				continue;
			}

			final float nodeCost = current.costSoFar + cost;
			final NodeRecord nodeRecord = getNodeRecord(to);
			final float nodeHeuristic;
			if (nodeRecord.category == CLOSED) {
				// The node is closed If we didn't find a shorter route, skip
				if (nodeRecord.costSoFar <= nodeCost) continue;
				// We can use the node's old cost values to calculate its heuristic
				// without calling the possibly expensive heuristic function
				nodeHeuristic = nodeRecord.getValue() - nodeRecord.costSoFar;
			} else if (nodeRecord.category == OPEN) {
				// If our route is no better, then skip
				if (nodeRecord.costSoFar <= nodeCost) continue;
				// Remove it from the open list (it will be re-added with the new cost)
				openList.remove(nodeRecord);
				// We can use the node's old cost values to calculate its heuristic
				// without calling the possibly expensive heuristic function
				nodeHeuristic = nodeRecord.getValue() - nodeRecord.costSoFar;
			} else { // the node is unvisited
				// We'll need to calculate the heuristic value using the function,
				// since we don't have a node record with a previously calculated value
				nodeHeuristic = estimateDistance(to, endNode);
			}
			// Update node record's cost and connection
			nodeRecord.costSoFar = nodeCost;
			nodeRecord.from = from;
			// Add it to the open list with the estimated total cost
			addToOpenList(nodeRecord, nodeCost + nodeHeuristic);
		}
	}

	private void addToOpenList(@NotNull NodeRecord nodeRecord, float estimatedTotalCost) {
		openList.add(nodeRecord, estimatedTotalCost);
		nodeRecord.category = OPEN;
	}

	private NodeRecord getNodeRecord(long node) {
		final int index = graphIndex(node);
		NodeRecord nr = nodeRecords[index];
		if (nr != null) {
			if (nr.searchId != searchId) {
				nr.category = UNVISITED;
				nr.searchId = searchId;
			}
			return nr;
		}
		nr = new NodeRecord(node);
		nr.searchId = searchId;
		nodeRecords[index] = nr;
		return nr;
	}

	private static final class NodeRecord extends BinaryHeap.Node {

		final long node;

		/** The incoming connection to the node  */
		long from = Vec2.NULL;
		/** The actual cost from the start node.  */
		float costSoFar = 0f;
		/** The node category: [UNVISITED], [OPEN] or [CLOSED].  */
		int category = 0;
		/** ID of the current search.  */
		int searchId = 0;

		NodeRecord(long node) {
			super(0f);
			this.node = node;
		}
	}

	public interface Path {
		int length();
		long node(int i);
		int nodeX(int i);
		int nodeY(int i);
	}

	private static final class PathImpl extends LongArray implements Path {
		@Override
		public int length() {
			return size;
		}

		@Override
		public long node(int i) {
			return items[i];
		}

		@Override
		public int nodeX(int i) {
			return x(items[i]);
		}

		@Override
		public int nodeY(int i) {
			return y(items[i]);
		}
	}

	private static final byte UNVISITED = 0;
	private static final byte OPEN = 1;
	private static final byte CLOSED = 2;

	private int graphIndex(long pos) {
		return x(pos) + y(pos) * height;
	}

	private static float estimateDistance(long from, long to) {
		// Anything that does not overestimate is ok
		return Vec2.manhattanLen(Vec2.minus(from, to));
	}

	public interface PathWorld {
		boolean isAccessible(int x, int y);
		float movementSpeedMultiplier(int x, int y);
	}
}
