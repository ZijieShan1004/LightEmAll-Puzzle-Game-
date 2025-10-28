import java.util.ArrayList;
import java.util.HashMap;
import tester.*;
import javalib.impworld.*;
import java.awt.Color;
import javalib.worldimages.*;

// contains the games logic and display
class LightEmAll extends World {

  // a list of all nodes (GamePiece objects)
  ArrayList<GamePiece> nodes;
  // a list of edges of the minimum spanning tree (MST)
  ArrayList<Edge> mst;
  // board width and height
  int width;
  int height;
  // the current location of the power station (initially at (0, 0))
  int powerRow;
  int powerCol;
  // effective radius for extra credit (to be computed later)
  int radius;
  int tileSize;

  // Constructor for LightEmAll; initializes game parameters and board using Kruskal's algorithm.
  public LightEmAll(int width, int height, int tileSize) {
    this.width = width;
    this.height = height;
    this.tileSize = tileSize;
    this.nodes = new ArrayList<>();
    this.mst = new ArrayList<>();
    this.powerRow = 0;
    this.powerCol = 0;
    this.radius = 0;
    initializeBoard();
  }

  // Generates the board using Kruskal's algorithm to form a minimum spanning tree.
  void initializeBoard() {
    // Create a grid of GamePieces with all connections initially set to false.
    GamePiece[][] grid = new GamePiece[height][width];
    for (int row = 0; row < height; row = row + 1) {
      for (int col = 0; col < width; col = col + 1) {
        boolean isPowerStation = (row == 0 && col == 0);
        grid[row][col] = new GamePiece(row, col, false, false, false, false, isPowerStation, false);
        nodes.add(grid[row][col]);
      }
    }
    // Create list of all potential edges between adjacent GamePieces with random weights.
    ArrayList<Edge> allEdges = new ArrayList<>();
    for (int row = 0; row < height; row = row + 1) {
      for (int col = 0; col < width; col = col + 1) {
        if (col < width - 1) { // right neighbor exists
          int weight = (int)(Math.random() * 100);
          allEdges.add(new Edge(grid[row][col], grid[row][col + 1], weight));
        }
        if (row < height - 1) { // bottom neighbor exists
          int weight = (int)(Math.random() * 100);
          allEdges.add(new Edge(grid[row][col], grid[row + 1][col], weight));
        }
      }
    }
    // Sort all potential edges by their weight.
    new SortEdges().sortEdgesList(allEdges);
    // Use union-find and Kruskal's algorithm to select edges for the MST.
    UnionFind uf = new UnionFind(nodes); // UnionFind data structure to manage connected components.
    for (Edge e : allEdges) {
      if (uf.find(e.fromNode) != uf.find(e.toNode)) {
        uf.union(e.fromNode, e.toNode);
        mst.add(e);
        // Set connectivity based on the relative positions of the two connected GamePieces.
        if (e.fromNode.row == e.toNode.row) { // horizontal neighbors
          if (e.fromNode.col < e.toNode.col) {
            e.fromNode.right = true;
            e.toNode.left = true;
          } else {
            e.fromNode.left = true;
            e.toNode.right = true;
          }
        }
        else if (e.fromNode.col == e.toNode.col) { // vertical neighbors
          if (e.fromNode.row < e.toNode.row) {
            e.fromNode.bottom = true;
            e.toNode.top = true;
          } else {
            e.fromNode.top = true;
            e.toNode.bottom = true;
          }
        }
      }
    }
    // Randomly rotate each tile to scramble the board.
    for (GamePiece gp : nodes) {
      tileSpinRandom(gp);
    }
  }

  // Generates the scene of the game.
  public WorldScene makeScene() {
    WorldScene scene = new WorldScene(tileSize * width, tileSize * height);
    updatePoweredStatus();

    for (GamePiece gp : nodes) {
      Color wireColor;
      if (gp.powered) {
        wireColor = Color.YELLOW;
      }
      else {
        wireColor = Color.GRAY;
      }

      WorldImage img = gp.tileImage(tileSize, 5, wireColor, gp.powerStation);
      int x = gp.col * tileSize + tileSize / 2;
      int y = gp.row * tileSize + tileSize / 2;
      scene.placeImageXY(img, x, y);
    }

    if (allPowered()) {
      scene.placeImageXY(new TextImage("You Win!", 40, Color.RED),
          tileSize * width / 2, tileSize * height / 2);
    }
    return scene;
  }

  // Spreads power through connected GamePieces using breadth-first search.
  void updatePoweredStatus() {
    for (GamePiece gp : nodes) {
      gp.powered = false;
    }
    Queue<GamePiece> queue = new Queue<>();
    GamePiece start = findPiece(powerRow, powerCol);

    if (start != null) {
      start.powered = true;
      queue.addAtTail(start);
      while (!queue.isEmpty()) {
        GamePiece current = queue.removeFromHead();
        checkNeighbor(current, current.row - 1, current.col, current.top, queue);
        checkNeighbor(current, current.row + 1, current.col, current.bottom, queue);
        checkNeighbor(current, current.row, current.col - 1, current.left, queue);
        checkNeighbor(current, current.row, current.col + 1, current.right, queue);
      }
    }
  }

  // Checks and powers neighbor if connected to current piece.
  void checkNeighbor(
      GamePiece current, int row, int col, boolean connected, Queue<GamePiece> queue) {
    if (connected) {
      GamePiece neighbor = findPiece(row, col);
      if (neighbor != null && !neighbor.powered && isConnected(current, neighbor)) {
        neighbor.powered = true;
        queue.addAtTail(neighbor);
      }
    }
  }

  // Returns true if two adjacent pieces are connected.
  boolean isConnected(GamePiece from, GamePiece to) {
    if (from.row == to.row) {
      if (from.col < to.col) {
        return from.right && to.left;
      }
      else {
        return from.left && to.right;
      }
    }
    else if (from.col == to.col) {
      if (from.row < to.row) {
        return from.bottom && to.top;
      }
      else {
        return from.top && to.bottom;
      }
    }
    else {
      return false;
    }
  }

  // Retrieves GamePiece at specified grid position.
  GamePiece findPiece(int row, int col) {
    for (GamePiece gp : nodes) {
      if (gp.row == row && gp.col == col) {
        return gp;
      }
    }
    return null;
  }

  // Checks if all pieces are powered.
  boolean allPowered() {
    for (GamePiece gp : nodes) {
      if (!gp.powered) {
        return false;
      }
    }
    return true;
  }

  // Rotates clicked piece and updates power flow.
  public void onMouseClicked(Posn pos, String buttonName) {
    int clickedCol = pos.x / tileSize;
    int clickedRow = pos.y / tileSize;

    for (GamePiece gp : nodes) {
      if (gp.row == clickedRow && gp.col == clickedCol) {
        rotatePiece(gp);
        updatePoweredStatus();
      }
    }
  }

  // Moves the power station if the target cell is connected.
  public void onKeyEvent(String key) {
    GamePiece current = findPiece(powerRow, powerCol);
    if (current == null) {
      return;
    }

    int newRow = powerRow;
    int newCol = powerCol;

    if (key.equals("up")) {
      newRow = newRow - 1;
    }
    else if (key.equals("down")) {
      newRow = newRow + 1;
    }
    else if (key.equals("left")) {
      newCol = newCol - 1;
    }
    else if (key.equals("right")) {
      newCol = newCol + 1;
    }

    GamePiece target = findPiece(newRow, newCol);
    if (target != null && isConnected(current, target)) {
      current.powerStation = false;
      target.powerStation = true;
      powerRow = newRow;
      powerCol = newCol;
      updatePoweredStatus();
    }
  }

  // Rotates a game piece 90 degrees clockwise.
  void rotatePiece(GamePiece gp) {
    boolean temp = gp.top;
    gp.top = gp.left;
    gp.left = gp.bottom;
    gp.bottom = gp.right;
    gp.right = temp;
  }

  // Randomly rotates a game piece a number of times.
  void tileSpinRandom(GamePiece gp) {
    double randomSpin = Math.random() * 4.0;
    for (double i = 0.0; i < randomSpin; i = i + 1.0) {
      rotatePiece(gp);
    }
  }
}

// New class: UnionFind - a union-find data structure for managing disjoint sets of GamePieces.
class UnionFind {
  // Maps each GamePiece to its parent in the union-find structure.
  HashMap<GamePiece, GamePiece> parent;

  // Constructor: initializes each GamePiece to be its own parent.
  UnionFind(ArrayList<GamePiece> items) {
    parent = new HashMap<>();
    for (GamePiece item : items) {
      parent.put(item, item);
    }
  }

  // Finds the representative (root) of the set that contains the given item.
  GamePiece find(GamePiece item) {
    if (parent.get(item) != item) {
      parent.put(item, find(parent.get(item))); // path compression
    }
    return parent.get(item);
  }

  // Unions the sets containing the two given GamePieces.
  void union(GamePiece a, GamePiece b) {
    GamePiece repA = find(a);
    GamePiece repB = find(b);
    parent.put(repA, repB);
  }
}

// holds the game pieces information for the game
class GamePiece {
  // in logical coordinates, with the origin at the top-left corner of the screen
  int row;
  int col;
  // connectivity to adjacent pieces
  boolean left;
  boolean right;
  boolean top;
  boolean bottom;
  // whether the power station is on this piece
  boolean powerStation;
  // whether this piece is powered
  boolean powered;

  GamePiece(int row, int col, boolean left, boolean right, 
      boolean top, boolean bottom, boolean powerStation, boolean powered) {
    this.row = row;
    this.col = col;
    this.left = left;
    this.right = right;
    this.top = top;
    this.bottom = bottom;
    this.powerStation = powerStation;
    this.powered = powered;
  }

  // Generate an image of this GamePiece.
  // - size: the size of the tile, in pixels
  // - wireWidth: the width of wires, in pixels
  // - wireColor: the Color to use for rendering wires on this tile
  // - hasPowerStation: if true, draws a fancy star to represent the power station
  WorldImage tileImage(int size, int wireWidth, Color wireColor, boolean hasPowerStation) {
    WorldImage image = new OverlayImage(
        new RectangleImage(wireWidth, wireWidth, OutlineMode.SOLID, wireColor),
        new RectangleImage(size, size, OutlineMode.SOLID, Color.DARK_GRAY));
    WorldImage vWire = new RectangleImage(wireWidth, (size + 1) / 2, OutlineMode.SOLID, wireColor);
    WorldImage hWire = new RectangleImage((size + 1) / 2, wireWidth, OutlineMode.SOLID, wireColor);

    if (this.top) {
      image = new OverlayOffsetAlign(AlignModeX.CENTER, AlignModeY.TOP, vWire, 0, 0, image);
    }
    
    if (this.right) {
      image = new OverlayOffsetAlign(AlignModeX.RIGHT, AlignModeY.MIDDLE, hWire, 0, 0, image);
    }
    
    if (this.bottom) {
      image = new OverlayOffsetAlign(AlignModeX.CENTER, AlignModeY.BOTTOM, vWire, 0, 0, image);
    }
    
    if (this.left) {
      image = new OverlayOffsetAlign(AlignModeX.LEFT, AlignModeY.MIDDLE, hWire, 0, 0, image);
    }
    
    if (hasPowerStation) {
      image = new OverlayImage(
        new OverlayImage(
          new StarImage(size / 3, 7, OutlineMode.OUTLINE, new Color(255, 128, 0)),
          new StarImage(size / 3, 7, OutlineMode.SOLID, new Color(0, 255, 255))
        ),
        image
      );
    }
    return image;
  }
}

// represents an edge between two GamePieces with an associated weight
class Edge {
  GamePiece fromNode;
  GamePiece toNode;
  int weight;

  Edge(GamePiece fromNode, GamePiece toNode, int weight) {
    this.fromNode = fromNode;
    this.toNode = toNode;
    this.weight = weight;
  }
}

// sorts a list of Edge objects in ascending order based on weight
class SortEdges {
  // Sorts the given list of edges using a simple bubble sort.
  void sortEdgesList(ArrayList<Edge> listOfEdges) {
    for (int i = 0; i < listOfEdges.size() - 1; i = i + 1) {
      for (int j = i + 1; j < listOfEdges.size(); j = j + 1) {
        if (listOfEdges.get(i).weight > listOfEdges.get(j).weight) {
          Edge changingIndex = listOfEdges.get(i);
          listOfEdges.set(i, listOfEdges.get(j));
          listOfEdges.set(j, changingIndex); 
        }
      }
    }
  }
}

// simple node class for internal collection usage in queue/deque
class Node<T> {
  T data;
  Node<T> next;
  Node<T> prev;

  Node(T data) {
    this.data = data;
    this.next = null;
    this.prev = null;
  }
}

// double-ended queue implementation
class Deque<T> implements ICollection<T> {
  Node<T> head;
  Node<T> tail;
  int size;

  Deque() {
    this.head = null;
    this.tail = null;
    this.size = 0;
  }

  public boolean isEmpty() {
    return this.head == null;
  }

  public void addAtTail(T item) {
    Node<T> addedNode = new Node<T>(item);
    if (isEmpty()) {
      this.head = addedNode;
      this.tail = addedNode;
    } else {
      this.tail.next = addedNode;
      addedNode.prev = this.tail;
      this.tail = addedNode;
    }
    this.size = this.size + 1;
  }

  public T removeFromHead() {
    if (isEmpty()) {
      throw new RuntimeException("Deque has no data");
    }
    T result = this.head.data;
    this.head = this.head.next;
    this.size = this.size - 1;
    return result;
  }

  public int size() {
    return this.size;
  }
}

// simple queue implementation using Deque
class Queue<T> implements ICollection<T> {
  Deque<T> contents;

  Queue() {
    this.contents = new Deque<T>();
  }

  public boolean isEmpty() {
    return this.contents.isEmpty();
  }

  public void addAtTail(T item) {
    this.contents.addAtTail(item);
  }

  public T removeFromHead() {
    return this.contents.removeFromHead();
  }
  
  public int size() {
    return this.contents.size();
  }
}

// generic collection interface
interface ICollection<T> {
  boolean isEmpty();
  
  void addAtTail(T item);
  
  T removeFromHead();
}

// Test class
class LightExamples {
  // Test for game
  void testGame(Tester t) {
    LightEmAll game = new LightEmAll(10, 10, 65);
    game.bigBang(650, 650, 1.0);
  }

  // Test for findPiece method
  void testFindPiece(Tester t) {
    LightEmAll game = new LightEmAll(3, 3, 50);
    t.checkExpect(game.findPiece(1, 1) != null, true);
    t.checkExpect(game.findPiece(-1, 2), null);
    t.checkExpect(game.findPiece(5, 5), null);
  }

  // Test for rotatePiece method
  void testRotatePiece(Tester t) {
    GamePiece gp1 = new GamePiece(0, 0, true, false, false, false, false, false);
    LightEmAll game = new LightEmAll(1, 1, 50);
    game.rotatePiece(gp1);
    t.checkExpect(gp1.top, true);
    t.checkExpect(gp1.right, false);
    
    GamePiece gp2 = new GamePiece(0, 0, false, false, false, false, false, false);
    game.rotatePiece(gp2);
    t.checkExpect(gp2.top, false);
    
    GamePiece gp3 = new GamePiece(0, 0, true, true, true, true, false, false);
    game.rotatePiece(gp3);
    t.checkExpect(gp3.left, true);
  }

  // Test for Deque operations
  void testDequeOperations(Tester t) {
    Deque<Integer> d1 = new Deque<>();
    t.checkException(new RuntimeException("Deque has no data"), d1, "removeFromHead");
    
    Deque<Integer> d2 = new Deque<>();
    d2.addAtTail(1);
    t.checkExpect(d2.removeFromHead(), 1);
    
    Deque<Integer> d3 = new Deque<>();
    for (int i = 0; i < 1000; i++) {
      d3.addAtTail(i);
    }
    t.checkExpect(d3.size(), 1000);
  }

  // Test for Queue operations
  void testQueueOperations(Tester t) {
    Queue<Integer> q1 = new Queue<>();
    t.checkExpect(q1.isEmpty(), true);
    
    Queue<Integer> q2 = new Queue<>();
    q2.addAtTail(3);
    t.checkExpect(q2.removeFromHead(), 3);
    
    Queue<Integer> q3 = new Queue<>();
    for (int i = 0; i < 500; i++) {
      q3.addAtTail(i);
    }
    t.checkExpect(q3.size(), 500);
  }

  // Test for UnionFind operations
  void testUnionFind(Tester t) {    
    GamePiece gp1 = new GamePiece(0, 0, false, false, false, false, false, false);
    ArrayList<GamePiece> singleItem = new ArrayList<>();
    singleItem.add(gp1);
    UnionFind uf2 = new UnionFind(singleItem);
    t.checkExpect(uf2.find(gp1), gp1);
    
    GamePiece gp2 = new GamePiece(0, 1, false, false, false, false, false, false);
    GamePiece gp3 = new GamePiece(1, 0, false, false, false, false, false, false);
    ArrayList<GamePiece> multiItems = new ArrayList<>();
    multiItems.add(gp1);
    multiItems.add(gp2);
    multiItems.add(gp3);
    UnionFind uf3 = new UnionFind(multiItems);
    uf3.union(gp1, gp2);
    uf3.union(gp2, gp3);
    t.checkExpect(uf3.find(gp1), gp3);
  }

  // Test for Edge class
  void testEdge(Tester t) {
    GamePiece gp1 = new GamePiece(0, 0, false, false, false, false, false, false);
    Edge e1 = new Edge(null, null, 0);
    t.checkExpect(e1.weight, 0);
    
    Edge e2 = new Edge(gp1, gp1, 50);
    t.checkExpect(e2.fromNode, gp1);
    
    Edge e3 = new Edge(gp1, gp1, Integer.MAX_VALUE);
    t.checkExpect(e3.weight, Integer.MAX_VALUE);
  }

  // Test for sortEdgesList method
  void testSortEdgesList(Tester t) {
    ArrayList<Edge> empty = new ArrayList<>();
    new SortEdges().sortEdgesList(empty);
    t.checkExpect(empty.size(), 0);
    
    GamePiece gp = new GamePiece(0, 0, false, false, false, false, false, false);
    ArrayList<Edge> single = new ArrayList<>();
    single.add(new Edge(gp, gp, 10));
    new SortEdges().sortEdgesList(single);
    t.checkExpect(single.get(0).weight, 10);
    
    ArrayList<Edge> multi = new ArrayList<>();
    multi.add(new Edge(gp, gp, 30));
    multi.add(new Edge(gp, gp, 10));
    multi.add(new Edge(gp, gp, 20));
    new SortEdges().sortEdgesList(multi);
    t.checkExpect(multi.get(0).weight, 10);
    t.checkExpect(multi.get(1).weight, 20);
    t.checkExpect(multi.get(2).weight, 30);
  }

  // Test for isConnected method
  void testIsConnected(Tester t) {
    GamePiece a = new GamePiece(0, 0, false, false, false, false, false, false);
    GamePiece b = new GamePiece(0, 1, false, false, false, false, false, false);
    t.checkExpect(new LightEmAll(1,1,50).isConnected(a, b), false);
    
    a.right = true;
    b.left = true;
    t.checkExpect(new LightEmAll(1,2,50).isConnected(a, b), true);
    
    a.bottom = true;
    GamePiece c = new GamePiece(1, 0, true, false, false, false, false, false);
    t.checkExpect(new LightEmAll(2,1,50).isConnected(a, c), false);
  }

  // Test for allPowered method
  void testAllPowered(Tester t) {
    LightEmAll emptyGame = new LightEmAll(0, 0, 50);
    t.checkExpect(emptyGame.allPowered(), true);
    
    LightEmAll game2x2 = new LightEmAll(2, 2, 50);
    for (GamePiece gp : game2x2.nodes) {
      gp.powered = true;
    }
    t.checkExpect(game2x2.allPowered(), true);
    
    game2x2.nodes.get(0).powered = false;
    t.checkExpect(game2x2.allPowered(), false);
  }

  // Test for onKeyEvent method
  void testOnKeyEvent(Tester t) {
    LightEmAll game = new LightEmAll(2, 2, 50);
    game.onKeyEvent("left");
    t.checkExpect(game.findPiece(0,0).powerStation, true);

    GamePiece start = new GamePiece(0,0,false,true,false,true,true,false);
    GamePiece below = new GamePiece(1,0,false,false,true,false,false,false);
    ArrayList<GamePiece> nodes = new ArrayList<>();
    nodes.add(start);
    nodes.add(below);
    game.nodes = nodes;
    game.onKeyEvent("down");
    t.checkExpect(below.powerStation, true);

    GamePiece rightNeighbor = new GamePiece(0,1,true,false,false,false,false,false);
    nodes.add(rightNeighbor);
    start.right = true;
    rightNeighbor.left = true;
    game.onKeyEvent("right");
    t.checkExpect(rightNeighbor.powerStation, false);
  }

  // Test for tileImage method
  void testTileImage(Tester t) {
    GamePiece gp1 = new GamePiece(0, 0, false, false, false, false, false, false);
    t.checkExpect(gp1.tileImage(0, 0, Color.BLACK, false) != null, true);
    
    GamePiece gp2 = new GamePiece(0, 0, true, true, true, true, true, true);
    t.checkExpect(gp2.tileImage(50, 5, Color.YELLOW, true) != null, true);
    
    GamePiece gp3 = new GamePiece(0, 0, false, true, false, true, false, false);
    t.checkExpect(gp3.tileImage(100, 10, Color.RED, false) != null, true);
  }

  // Test for checkNeighbor method
  void testCheckNeighbor(Tester t) {
    LightEmAll game = new LightEmAll(2, 2, 50);
    Queue<GamePiece> q1 = new Queue<>();
    game.checkNeighbor(new GamePiece(0,0,false,false,false,false,false,false), 
                      1, 1, true, q1);
    t.checkExpect(q1.size(), 0);

    GamePiece a = new GamePiece(0,0,false,true,false,false,true,true);
    GamePiece b = new GamePiece(0,1,true,false,false,false,false,true);
    ArrayList<GamePiece> nodes = new ArrayList<>();
    nodes.add(a);
    nodes.add(b);
    game.nodes = nodes;
    Queue<GamePiece> q2 = new Queue<>();
    game.checkNeighbor(a, 0, 1, true, q2);
    t.checkExpect(q2.size(), 0);

    GamePiece c = new GamePiece(1,0,false,false,true,false,false,false);
    nodes.add(c);
    a.bottom = true;
    c.top = true;
    Queue<GamePiece> q3 = new Queue<>();
    game.checkNeighbor(a, 1, 0, true, q3);
    t.checkExpect(q3.size(), 1);
  }

  // Test for initializeBoard method
  void testInitializeBoard(Tester t) {
    LightEmAll game0 = new LightEmAll(0, 0, 50);
    t.checkExpect(game0.mst.size(), 0);
    
    LightEmAll game1x1 = new LightEmAll(1, 1, 50);
    t.checkExpect(game1x1.mst.size(), 0);
    
    LightEmAll game3x3 = new LightEmAll(3, 3, 50);
    t.checkExpect(game3x3.mst.size(), 8);
  }

  // Test for tileSpinRandom method
  void testTileSpinRandom(Tester t) {
    GamePiece gp1 = new GamePiece(0,0,false,false,false,false,false,false);
    new LightEmAll(1,1,50).tileSpinRandom(gp1);
    t.checkExpect(!gp1.top && !gp1.right && !gp1.bottom && !gp1.left, true
    );
    
    GamePiece gp2 = new GamePiece(0,0,true,false,false,false,false,false);
    new LightEmAll(1,1,50).tileSpinRandom(gp2);
    t.checkExpect(gp2.top || gp2.right || gp2.bottom || gp2.left, true
    );
    
    GamePiece gp3 = new GamePiece(0,0,true,true,true,true,false,false);
    new LightEmAll(1,1,50).tileSpinRandom(gp3);
    t.checkExpect(gp3.top && gp3.right && gp3.bottom && gp3.left, true
    );
  }
  
  // Test update powered status
  void testUpdatePoweredStatus(Tester t) {
    LightEmAll game = new LightEmAll(3, 1, 50);
    GamePiece a = new GamePiece(0, 0, false, true, false, false, true, false);
    GamePiece b = new GamePiece(0, 1, true, true, false, false, false, false);
    GamePiece c = new GamePiece(0, 2, true, false, false, false, false, false);
    a.right = true;
    b.left = true;
    b.right = true;
    c.left = true;
    ArrayList<GamePiece> nodes = new ArrayList<>();
    nodes.add(a);
    nodes.add(b);
    nodes.add(c);
    game.nodes = nodes;
    game.updatePoweredStatus();
    t.checkExpect(c.powered, true);

    GamePiece d = new GamePiece(0, 0, true, false, true, false, true, false);
    GamePiece e = new GamePiece(1, 0, false, false, false, true, false, false);
    d.bottom = true;
    e.top = true;
    ArrayList<GamePiece> nodes2 = new ArrayList<>();
    nodes2.add(d);
    nodes2.add(e);
    LightEmAll game2 = new LightEmAll(1, 2, 50);
    game2.nodes = nodes2;
    game2.updatePoweredStatus();
    t.checkExpect(e.powered, true);

    LightEmAll game3 = new LightEmAll(2, 2, 50);
    GamePiece f = new GamePiece(0, 0, true, false, true, false, true, false);
    GamePiece g = new GamePiece(0, 1, false, false, false, false, false, false);
    GamePiece h = new GamePiece(1, 0, false, false, false, true, false, false);
    f.right = true;
    f.bottom = true;
    g.left = true;
    h.top = true;
    ArrayList<GamePiece> nodes3 = new ArrayList<>();
    nodes3.add(f);
    nodes3.add(g);
    nodes3.add(h);
    game3.nodes = nodes3;
    game3.updatePoweredStatus();
    t.checkExpect(g.powered, true);
    t.checkExpect(h.powered, true);
  }
}