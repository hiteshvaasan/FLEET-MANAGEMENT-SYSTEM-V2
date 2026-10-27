package com.fleetmanagement.controllers;

import javafx.animation.AnimationTimer;
import javafx.beans.property.DoubleProperty;
import javafx.beans.property.SimpleDoubleProperty;
import javafx.beans.value.ChangeListener;
import javafx.geometry.VPos;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.layout.Pane;
import javafx.scene.paint.Color;
import javafx.scene.paint.CycleMethod;
import javafx.scene.paint.LinearGradient;
import javafx.scene.paint.RadialGradient;
import javafx.scene.paint.Stop;
import javafx.scene.shape.StrokeLineCap;
import javafx.scene.shape.StrokeLineJoin;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.scene.text.Text;
import javafx.scene.text.TextAlignment;
import javafx.stage.Window;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.PriorityQueue;
import java.util.Random;

/**
 * Live "fleet dispatch" background for the login screen (version 2: real street map).
 *
 * What you see:
 *  - a dark night-time city map: a street network with main roads, side streets,
 *    diagonal avenues, city blocks and big empty districts
 *  - hubs placed on main intersections
 *  - trucks that drive hub -> hub along the SHORTEST ROAD ROUTE (Dijkstra on the street
 *    graph), turning at junctions with rounded corners
 *  - the route ahead of every truck glows (like a navigation line) and has light sparks
 *    flowing along it; the destination hub pulses
 *
 * Pure JavaFX (Canvas + AnimationTimer). No video, no extra libraries.
 *
 * FXML usage (first child of the root StackPane):
 *     <AnimatedRouteBackground leftInset="560"/>
 *
 * leftInset = pixels on the left kept free for the login card (hubs are placed to the right).
 */
public class AnimatedRouteBackground extends Pane {

    // ------------------------------------------------------------------ tunables
    private static final double BASE_SPEED    = 85;    // truck speed, px/sec (at scale 1)
    private static final double LATTICE       = 58;    // distance between street intersections
    private static final int    MAJOR_EVERY   = 4;     // every 4th street is a main road
    private static final double CORNER_RADIUS = 18;    // how round the turns are
    private static final int    TRAIL_MAX     = 26;    // length of the glowing tail behind a truck
    private static final int    PARTICLES     = 35;    // floating dust points
    private static final double DWELL_MIN     = 1.0;   // seconds a truck waits at a hub
    private static final double DWELL_RANGE   = 1.2;

    // route preference: lower cost = trucks prefer that road type
    private static final double COST_MINOR  = 1.00;
    private static final double COST_MAJOR  = 0.72;
    private static final double COST_AVENUE = 0.66;

    // diagonal avenues (lattice lines i - j = c, and i + j = d)
    private static final int[] AVENUE_DIAG = {-9, -2, 6, 13};
    private static final int[] AVENUE_ANTI = {-4, 8};

    // ------------------------------------------------------------------ fleet data (edit freely)
    private static final String[] HUB_NAMES = {
            "Hub Alpha (Central)", "Hub Beta (North)", "Hub Gamma (South)",
            "Hub Delta (East)", "Hub Epsilon (North-East)", "Hub Zeta (South-East)"
    };
    // wanted position of every hub inside the free map area (0..1, 0..1);
    // each hub snaps to the nearest main intersection
    private static final double[][] HUB_POS = {
            {0.20, 0.60}, {0.46, 0.18}, {0.50, 0.82},
            {0.90, 0.52}, {0.82, 0.14}, {0.86, 0.86}
    };
    private static final String[] TRUCK_IDS    = {"TRK-101", "TRK-305", "VAN-204", "TRK-412"};
    private static final String[] TRUCK_COLORS = {"#2ee6d6", "#fb923c", "#4ade80", "#a78bfa"};
    // every truck drives a closed loop through these hubs (indexes into HUB_NAMES)
    private static final int[][] TRUCK_LOOPS = {
            {0, 1, 4, 3},
            {4, 1, 2, 5},
            {0, 2, 5, 3},
            {1, 0, 2, 3}
    };

    // ------------------------------------------------------------------ state
    private final Canvas baseCanvas = new Canvas();   // static map (redrawn only on resize)
    private final Canvas fxCanvas   = new Canvas();   // animated layer (redrawn every frame)
    private final DoubleProperty leftInset = new SimpleDoubleProperty(this, "leftInset", 0);

    private final Hub[] hubs = new Hub[HUB_NAMES.length];
    private final Truck[] trucks = new Truck[TRUCK_IDS.length];
    private final Particle[] particles = new Particle[PARTICLES];
    private final Random rnd = new Random();
    private final double[] pos = new double[4];

    // street graph
    private int gridR, gridN, nodeCount;
    private double[] nx, ny;
    private int[] edgeA, edgeB;
    private byte[] edgeT;                 // 0 = side street, 1 = main road, 2 = avenue
    private int[][] adj;
    private double[][] adjW;
    private int[] hubNode;
    private final HashMap<Integer, RoadPath> pathCache = new HashMap<>();

    private double w, h, scale = 1;
    private double time;
    private boolean running;
    private boolean built;

    private Font hubFont, tagFont, hudFont, hudBoldFont;
    private double wNetwork, wOptimal, wLive;

    private final AnimationTimer timer = new AnimationTimer() {
        private long last = 0;

        @Override
        public void handle(long now) {
            if (last == 0) last = now;
            double dt = Math.min(0.05, (now - last) / 1_000_000_000.0);
            last = now;
            if (!built) return;
            time += dt;
            update(dt);
            render();
        }

        @Override
        public void start() {
            last = 0;
            super.start();
        }
    };

    private final ChangeListener<Boolean> showingListener = (o, was, is) -> {
        if (is) start(); else stop();
    };
    private final ChangeListener<Window> windowListener = (o, oldW, newW) -> {
        if (oldW != null) oldW.showingProperty().removeListener(showingListener);
        if (newW != null) {
            newW.showingProperty().addListener(showingListener);
            if (newW.isShowing()) start();
        }
    };

    // ------------------------------------------------------------------ construction
    public AnimatedRouteBackground() {
        setMinSize(0, 0);
        setMouseTransparent(true);       // never steal clicks from the login form
        baseCanvas.setManaged(false);
        fxCanvas.setManaged(false);
        getChildren().addAll(baseCanvas, fxCanvas);

        for (int i = 0; i < hubs.length; i++) hubs[i] = new Hub(HUB_NAMES[i], i * 0.37);
        Random r = new Random(11);
        for (int i = 0; i < particles.length; i++) particles[i] = new Particle(r);

        leftInset.addListener((o, a, b) -> {
            if (w > 0) rebuild();
        });

        sceneProperty().addListener((o, oldS, newS) -> {
            if (oldS != null) {
                oldS.windowProperty().removeListener(windowListener);
                if (oldS.getWindow() != null) {
                    oldS.getWindow().showingProperty().removeListener(showingListener);
                }
            }
            if (newS == null) {
                stop();
            } else {
                newS.windowProperty().addListener(windowListener);
                windowListener.changed(null, null, newS.getWindow());
            }
        });
    }

    // FXML property:  <AnimatedRouteBackground leftInset="560"/>
    public double getLeftInset() { return leftInset.get(); }
    public void setLeftInset(double v) { leftInset.set(v); }
    public DoubleProperty leftInsetProperty() { return leftInset; }

    public void start() {
        if (!running) {
            running = true;
            timer.start();
        }
    }

    public void stop() {
        if (running) {
            running = false;
            timer.stop();
        }
    }

    // ------------------------------------------------------------------ layout
    // This node must never force the window to a certain size.
    @Override protected double computePrefWidth(double height)  { return 0; }
    @Override protected double computePrefHeight(double width)  { return 0; }
    @Override protected double computeMinWidth(double height)   { return 0; }
    @Override protected double computeMinHeight(double width)   { return 0; }

    @Override
    protected void layoutChildren() {
        double nw = getWidth(), nh = getHeight();
        if (nw < 2 || nh < 2) return;
        if (nw != w || nh != h) {
            w = nw;
            h = nh;
            baseCanvas.setWidth(w);
            baseCanvas.setHeight(h);
            fxCanvas.setWidth(w);
            fxCanvas.setHeight(h);
            rebuild();
        }
    }

    // ------------------------------------------------------------------ build everything
    private void rebuild() {
        built = false;
        scale = Math.max(0.75, Math.min(1.5, Math.min(w / 1920.0, h / 1080.0) * 1.05));

        hubFont     = Font.font("Segoe UI", FontWeight.NORMAL, 11.5 * scale);
        tagFont     = Font.font("Segoe UI", FontWeight.BOLD, 11 * scale);
        hudFont     = Font.font("Segoe UI", FontWeight.NORMAL, 12 * scale);
        hudBoldFont = Font.font("Segoe UI", FontWeight.BOLD, 12 * scale);
        wNetwork = measure("Network Status: ", hudFont);
        wOptimal = measure("OPTIMAL", hudBoldFont);
        wLive    = measure("LIVE TRACKING", hudBoldFont);

        buildRoads();
        placeHubs();
        pathCache.clear();

        for (int i = 0; i < trucks.length; i++) {
            Truck t = trucks[i];
            boolean first = (t == null);
            if (first) {
                t = new Truck(i, TRUCK_IDS[i], Color.web(TRUCK_COLORS[i]), TRUCK_LOOPS[i]);
                t.leg = i % t.loop.length;
                t.u = 0.10 + rnd.nextDouble() * 0.55;
                trucks[i] = t;
            }
            t.labelW = measure(t.id, tagFont);
            startLeg(t);
            t.dist = t.u * t.path.total;
            t.pathIdx = t.path.locate(t.dist, pos);
            t.x = pos[0];
            t.y = pos[1];
            t.heading = Math.toDegrees(Math.atan2(pos[3], pos[2]));
            t.trail.clear();
        }

        drawBase();
        built = true;
    }

    private static double measure(String s, Font f) {
        Text t = new Text(s);
        t.setFont(f);
        return t.getLayoutBounds().getWidth();
    }

    // ------------------------------------------------------------------ street graph
    private int nodeId(int i, int j) {
        return (i + gridR) * gridN + (j + gridR);
    }

    /** How many of the optional side streets exist inside one big block depends on its "district type". */
    private double minorKeepChance(int i, int j) {
        int ci = Math.floorDiv(i, MAJOR_EVERY), cj = Math.floorDiv(j, MAJOR_EVERY);
        int hsh = (ci * 73856093) ^ (cj * 19349663) ^ 0x5bd1e995;
        hsh ^= (hsh >>> 13);
        double u = ((hsh & 0xffff) / 65535.0);
        if (u < 0.30) return 0.22;      // big empty block (park / industrial area)
        if (u < 0.65) return 0.62;      // medium density
        return 0.93;                    // dense downtown
    }

    private void buildRoads() {
        double reach = Math.hypot(w, h) / 2 + LATTICE * 2;
        gridR = (int) Math.ceil(reach / LATTICE);
        gridN = gridR * 2 + 1;
        nodeCount = gridN * gridN;
        nx = new double[nodeCount];
        ny = new double[nodeCount];

        Random r = new Random(42);
        double ang = Math.toRadians(-17), ca = Math.cos(ang), sa = Math.sin(ang);

        // 1) intersections: a slightly warped, rotated lattice -> gently curving streets
        for (int i = -gridR; i <= gridR; i++) {
            for (int j = -gridR; j <= gridR; j++) {
                boolean onI = Math.floorMod(i, MAJOR_EVERY) == 0;
                boolean onJ = Math.floorMod(j, MAJOR_EVERY) == 0;
                double jit = (onI && onJ) ? 0 : ((onI || onJ) ? 3 : 9);
                double lx = i * LATTICE + (r.nextDouble() - 0.5) * 2 * jit;
                double ly = j * LATTICE + (r.nextDouble() - 0.5) * 2 * jit;
                double wx = lx + 16 * Math.sin(ly * 0.0055 + 1.3) + 9 * Math.sin(ly * 0.013 + 0.4);
                double wy = ly + 16 * Math.sin(lx * 0.0048 + 2.1) + 9 * Math.sin(lx * 0.012 + 5.0);
                int id = nodeId(i, j);
                nx[id] = w / 2 + wx * ca - wy * sa;
                ny[id] = h / 2 + wx * sa + wy * ca;
            }
        }

        // 2) streets between neighbouring intersections
        ArrayList<int[]> edges = new ArrayList<>();
        for (int i = -gridR; i <= gridR; i++) {
            for (int j = -gridR; j <= gridR; j++) {
                if (i < gridR) {
                    boolean major = Math.floorMod(j, MAJOR_EVERY) == 0;
                    if (major || r.nextDouble() < minorKeepChance(i, j)) {
                        edges.add(new int[]{nodeId(i, j), nodeId(i + 1, j), major ? 1 : 0});
                    }
                }
                if (j < gridR) {
                    boolean major = Math.floorMod(i, MAJOR_EVERY) == 0;
                    if (major || r.nextDouble() < minorKeepChance(i, j)) {
                        edges.add(new int[]{nodeId(i, j), nodeId(i, j + 1), major ? 1 : 0});
                    }
                }
            }
        }
        // 3) diagonal avenues
        for (int c : AVENUE_DIAG) {
            for (int i = -gridR; i < gridR; i++) {
                int j = i - c;
                if (j >= -gridR && j < gridR) {
                    edges.add(new int[]{nodeId(i, j), nodeId(i + 1, j + 1), 2});
                }
            }
        }
        for (int d : AVENUE_ANTI) {
            for (int i = -gridR; i < gridR; i++) {
                int j = d - i;
                if (j > -gridR && j <= gridR) {
                    edges.add(new int[]{nodeId(i, j), nodeId(i + 1, j - 1), 2});
                }
            }
        }

        // 4) arrays + adjacency lists (with travel cost) for route finding
        int m = edges.size();
        edgeA = new int[m];
        edgeB = new int[m];
        edgeT = new byte[m];
        int[] deg = new int[nodeCount];
        for (int e = 0; e < m; e++) {
            int[] ed = edges.get(e);
            edgeA[e] = ed[0];
            edgeB[e] = ed[1];
            edgeT[e] = (byte) ed[2];
            deg[ed[0]]++;
            deg[ed[1]]++;
        }
        adj = new int[nodeCount][];
        adjW = new double[nodeCount][];
        for (int n = 0; n < nodeCount; n++) {
            adj[n] = new int[deg[n]];
            adjW[n] = new double[deg[n]];
        }
        int[] fill = new int[nodeCount];
        for (int e = 0; e < m; e++) {
            int a = edgeA[e], b = edgeB[e];
            double len = Math.hypot(nx[a] - nx[b], ny[a] - ny[b]);
            double cost = len * (edgeT[e] == 1 ? COST_MAJOR : edgeT[e] == 2 ? COST_AVENUE : COST_MINOR);
            adj[a][fill[a]] = b;
            adjW[a][fill[a]++] = cost;
            adj[b][fill[b]] = a;
            adjW[b][fill[b]++] = cost;
        }
    }

    /** Put every hub on the main intersection that is closest to its wanted spot. */
    private void placeHubs() {
        hubNode = new int[hubs.length];
        boolean[] used = new boolean[nodeCount];

        double left = Math.min(leftInset.get(), w * 0.45) + 50;
        double right = w - 60;
        double top = 90, bottom = h - 90;

        for (int k = 0; k < hubs.length; k++) {
            double px = left + HUB_POS[k][0] * (right - left);
            double py = top + HUB_POS[k][1] * (bottom - top);
            int best = -1;
            double bd = Double.MAX_VALUE;
            for (int pass = 0; pass < 2 && best < 0; pass++) {
                for (int i = -gridR; i <= gridR; i++) {
                    if (Math.floorMod(i, MAJOR_EVERY) != 0) continue;
                    for (int j = -gridR; j <= gridR; j++) {
                        if (Math.floorMod(j, MAJOR_EVERY) != 0) continue;
                        int id = nodeId(i, j);
                        if (used[id]) continue;
                        double x = nx[id], y = ny[id];
                        if (pass == 0 && (x < left - 20 || x > w - 40 || y < 70 || y > h - 70)) continue;
                        double d = (x - px) * (x - px) + (y - py) * (y - py);
                        if (d < bd) {
                            bd = d;
                            best = id;
                        }
                    }
                }
            }
            used[best] = true;
            hubNode[k] = best;
            hubs[k].x = nx[best];
            hubs[k].y = ny[best];
        }
    }

    // ------------------------------------------------------------------ route finding
    private int[] dijkstra(int src, int dst) {
        double[] dist = new double[nodeCount];
        int[] prev = new int[nodeCount];
        Arrays.fill(dist, Double.MAX_VALUE);
        Arrays.fill(prev, -1);
        PriorityQueue<double[]> pq = new PriorityQueue<>((p, q) -> Double.compare(p[0], q[0]));
        dist[src] = 0;
        pq.add(new double[]{0, src});
        while (!pq.isEmpty()) {
            double[] cur = pq.poll();
            int u = (int) cur[1];
            if (cur[0] > dist[u]) continue;
            if (u == dst) break;
            for (int k = 0; k < adj[u].length; k++) {
                int v = adj[u][k];
                double nd = dist[u] + adjW[u][k];
                if (nd < dist[v]) {
                    dist[v] = nd;
                    prev[v] = u;
                    pq.add(new double[]{nd, v});
                }
            }
        }
        if (dist[dst] == Double.MAX_VALUE) return null;
        ArrayList<Integer> rev = new ArrayList<>();
        for (int n = dst; n != -1; n = prev[n]) rev.add(n);
        int[] ids = new int[rev.size()];
        for (int i = 0; i < ids.length; i++) ids[i] = rev.get(ids.length - 1 - i);
        return ids;
    }

    private RoadPath pathBetween(int hubA, int hubB) {
        int key = hubA * 16 + hubB;
        RoadPath p = pathCache.get(key);
        if (p != null) return p;

        int[] ids = dijkstra(hubNode[hubA], hubNode[hubB]);
        ArrayList<double[]> pts = new ArrayList<>();
        if (ids == null || ids.length < 2) {                 // should not happen; safe fallback
            pts.add(new double[]{hubs[hubA].x, hubs[hubA].y});
            pts.add(new double[]{hubs[hubB].x, hubs[hubB].y});
        } else {
            pts.add(new double[]{nx[ids[0]], ny[ids[0]]});
            // round every corner with a small curve so the trucks turn smoothly
            for (int k = 1; k < ids.length - 1; k++) {
                double ax = nx[ids[k - 1]], ay = ny[ids[k - 1]];
                double bx = nx[ids[k]],     by = ny[ids[k]];
                double cx = nx[ids[k + 1]], cy = ny[ids[k + 1]];
                double abx = ax - bx, aby = ay - by, bcx = cx - bx, bcy = cy - by;
                double la = Math.max(1e-6, Math.hypot(abx, aby));
                double lc = Math.max(1e-6, Math.hypot(bcx, bcy));
                double rad = Math.min(CORNER_RADIUS * scale, Math.min(la, lc) * 0.5);
                double p1x = bx + abx / la * rad, p1y = by + aby / la * rad;
                double p2x = bx + bcx / lc * rad, p2y = by + bcy / lc * rad;
                pts.add(new double[]{p1x, p1y});
                for (int s = 1; s < 6; s++) {
                    double t = s / 6.0, q = 1 - t;
                    pts.add(new double[]{
                            q * q * p1x + 2 * q * t * bx + t * t * p2x,
                            q * q * p1y + 2 * q * t * by + t * t * p2y});
                }
                pts.add(new double[]{p2x, p2y});
            }
            pts.add(new double[]{nx[ids[ids.length - 1]], ny[ids[ids.length - 1]]});
        }
        p = new RoadPath(pts);
        pathCache.put(key, p);
        return p;
    }

    private void startLeg(Truck t) {
        int n = t.loop.length;
        t.path = pathBetween(t.loop[t.leg], t.loop[(t.leg + 1) % n]);
        t.dist = 0;
        t.pathIdx = 0;
    }

    // ------------------------------------------------------------------ static map layer
    private void drawBase() {
        GraphicsContext g = baseCanvas.getGraphicsContext2D();
        g.clearRect(0, 0, w, h);

        // ground
        g.setFill(new LinearGradient(0, 0, 0, h, false, CycleMethod.NO_CYCLE,
                new Stop(0.0, Color.web("#020821")),
                new Stop(0.55, Color.web("#04122f")),
                new Stop(1.0, Color.web("#020a24"))));
        g.fillRect(0, 0, w, h);
        g.setFill(glow(w * 0.72, h * 0.45, Math.max(w, h) * 0.55, Color.web("#1d4ed8"), 0.16));
        g.fillRect(0, 0, w, h);
        g.setFill(glow(w * 0.25, h * 0.95, Math.max(w, h) * 0.45, Color.web("#0891b2"), 0.10));
        g.fillRect(0, 0, w, h);

        // city blocks (random cells get a faint tint -> looks like built-up areas)
        Random br = new Random(9);
        double[] bx = new double[4], by = new double[4];
        for (int i = -gridR; i < gridR; i++) {
            for (int j = -gridR; j < gridR; j++) {
                double u = br.nextDouble();
                if (u > 0.28) continue;
                int a = nodeId(i, j), b = nodeId(i + 1, j), c = nodeId(i + 1, j + 1), d = nodeId(i, j + 1);
                bx[0] = nx[a]; by[0] = ny[a];
                bx[1] = nx[b]; by[1] = ny[b];
                bx[2] = nx[c]; by[2] = ny[c];
                bx[3] = nx[d]; by[3] = ny[d];
                g.setFill(rgba(40, 130, 170, 0.035 + u * 0.12));
                g.fillPolygon(bx, by, 4);
            }
        }

        // a few parks / water bodies
        Random r = new Random(7);
        for (int i = 0; i < 6; i++) {
            double cx = r.nextDouble() * w, cy = r.nextDouble() * h;
            double rw = 110 + r.nextDouble() * 220, rh = 70 + r.nextDouble() * 140;
            g.setFill(i % 3 == 0 ? rgba(56, 189, 248, 0.04) : rgba(20, 184, 120, 0.03));
            g.fillOval(cx - rw / 2, cy - rh / 2, rw, rh);
        }

        // streets
        g.setLineCap(StrokeLineCap.ROUND);
        g.setLineJoin(StrokeLineJoin.ROUND);

        g.setStroke(rgba(22, 112, 138, 0.38));
        g.setLineWidth(1.0);
        strokeEdges(g, 0);

        g.setStroke(rgba(32, 150, 178, 0.46));
        g.setLineWidth(2.4);
        strokeEdges(g, 1);

        g.setStroke(rgba(40, 190, 215, 0.10));
        g.setLineWidth(7);
        strokeEdges(g, 2);
        g.setStroke(rgba(48, 175, 200, 0.55));
        g.setLineWidth(2.8);
        strokeEdges(g, 2);

        // main intersections
        g.setFill(rgba(90, 210, 230, 0.35));
        for (int i = -gridR; i <= gridR; i++) {
            if (Math.floorMod(i, MAJOR_EVERY) != 0) continue;
            for (int j = -gridR; j <= gridR; j++) {
                if (Math.floorMod(j, MAJOR_EVERY) != 0) continue;
                int id = nodeId(i, j);
                if (nx[id] < -10 || nx[id] > w + 10 || ny[id] < -10 || ny[id] > h + 10) continue;
                g.fillOval(nx[id] - 2, ny[id] - 2, 4, 4);
            }
        }

        // darker strip on the left so the login card always stays readable
        double fade = Math.min(leftInset.get(), w * 0.45) + 220;
        g.setFill(new LinearGradient(0, 0, fade, 0, false, CycleMethod.NO_CYCLE,
                new Stop(0, rgba(2, 6, 16, 0.62)), new Stop(1, rgba(2, 6, 16, 0))));
        g.fillRect(0, 0, fade, h);

        // vignette
        g.setFill(new RadialGradient(0, 0, w / 2, h / 2, Math.max(w, h) * 0.75, false, CycleMethod.NO_CYCLE,
                new Stop(0.55, rgba(0, 0, 0, 0)), new Stop(1.0, rgba(0, 0, 0, 0.60))));
        g.fillRect(0, 0, w, h);
    }

    private void strokeEdges(GraphicsContext g, int type) {
        g.beginPath();
        for (int e = 0; e < edgeA.length; e++) {
            if (edgeT[e] != type) continue;
            double x1 = nx[edgeA[e]], y1 = ny[edgeA[e]], x2 = nx[edgeB[e]], y2 = ny[edgeB[e]];
            if ((x1 < -80 && x2 < -80) || (x1 > w + 80 && x2 > w + 80)
                    || (y1 < -80 && y2 < -80) || (y1 > h + 80 && y2 > h + 80)) continue;
            g.moveTo(x1, y1);
            g.lineTo(x2, y2);
        }
        g.stroke();
    }

    // ------------------------------------------------------------------ simulation
    private void update(double dt) {
        for (Particle p : particles) {
            p.x += p.vx * dt;
            p.y += p.vy * dt;
            if (p.x < 0) p.x += 1;
            if (p.x > 1) p.x -= 1;
            if (p.y < 0) p.y += 1;
            if (p.y > 1) p.y -= 1;
        }

        for (Truck t : trucks) {
            if (t.dwell > 0) {
                t.dwell -= dt;
                if (!t.trail.isEmpty()) t.trail.pollFirst();      // tail slowly shrinks while parked
            } else {
                double total = t.path.total;
                double edge = Math.min(t.dist, total - t.dist);    // distance to nearest end of the route
                double speed = BASE_SPEED * scale * (0.30 + 0.70 * Math.min(1, edge / (140 * scale)));
                t.dist += speed * dt;
                if (t.dist >= total) {
                    int n = t.loop.length;
                    int arrived = t.loop[(t.leg + 1) % n];
                    hubs[arrived].lastArrival = time;
                    t.leg = (t.leg + 1) % n;
                    startLeg(t);
                    t.dwell = DWELL_MIN + rnd.nextDouble() * DWELL_RANGE;
                }
            }

            t.pathIdx = t.path.locate(t.dist, pos);
            t.u = Math.max(0, Math.min(1, t.dist / t.path.total));
            t.x = pos[0];
            t.y = pos[1];

            if (t.dwell <= 0) {
                t.trail.addLast(new double[]{t.x, t.y});
                while (t.trail.size() > TRAIL_MAX) t.trail.pollFirst();
            }

            // turn smoothly towards the direction of the road
            double target = Math.toDegrees(Math.atan2(pos[3], pos[2]));
            double diff = (target - t.heading) % 360;
            if (diff > 180) diff -= 360;
            if (diff < -180) diff += 360;
            t.heading += diff * Math.min(1, dt * 9);
        }
    }

    // ------------------------------------------------------------------ rendering
    private void render() {
        GraphicsContext g = fxCanvas.getGraphicsContext2D();
        g.clearRect(0, 0, w, h);

        drawParticles(g);
        for (Truck t : trucks) drawRoute(g, t);
        drawHubs(g);
        for (Truck t : trucks) drawTrail(g, t);
        for (Truck t : trucks) drawTruck(g, t);
        for (Truck t : trucks) drawTag(g, t);
        drawHud(g);
    }

    private void drawParticles(GraphicsContext g) {
        for (Particle p : particles) {
            double a = 0.08 + 0.26 * (0.5 + 0.5 * Math.sin(time * p.speed + p.phase));
            g.setFill(rgba(150, 230, 255, a));
            double r = p.size * scale;
            g.fillOval(p.x * w - r, p.y * h - r, r * 2, r * 2);
        }
    }

    /** The glowing navigation line from the truck to its destination. */
    private void drawRoute(GraphicsContext g, Truck t) {
        RoadPath p = t.path;
        g.setLineCap(StrokeLineCap.ROUND);
        g.setLineJoin(StrokeLineJoin.ROUND);

        g.beginPath();
        g.moveTo(t.x, t.y);
        for (int k = t.pathIdx + 1; k < p.x.length; k++) g.lineTo(p.x[k], p.y[k]);

        g.setStroke(alpha(t.color, 0.10));
        g.setLineWidth(13 * scale);
        g.stroke();
        g.setStroke(alpha(t.color, 0.26));
        g.setLineWidth(6.5 * scale);
        g.stroke();
        g.setStroke(alpha(t.color.interpolate(Color.WHITE, 0.55), 0.96));
        g.setLineWidth(2.3 * scale);
        g.stroke();

        // light sparks flowing along the route, fixed to the road (they move forward, the truck follows)
        double period = 52 * scale;
        double offset = (((t.dist - time * 75 * scale) % period) + period) % period;
        g.save();
        g.setStroke(rgba(255, 255, 255, 0.95));
        g.setLineWidth(3.4 * scale);
        g.setLineDashes(2 * scale, 50 * scale);
        g.setLineDashOffset(offset);
        g.stroke();
        g.restore();

        // destination marker
        Hub d = hubs[t.loop[(t.leg + 1) % t.loop.length]];
        double pulse = 0.5 + 0.5 * Math.sin(time * 3 + t.index);
        double rr = (14 + 5 * pulse) * scale;
        g.setStroke(alpha(t.color, 0.35 + 0.45 * pulse));
        g.setLineWidth(2 * scale);
        g.strokeOval(d.x - rr, d.y - rr, rr * 2, rr * 2);
    }

    private void drawHubs(GraphicsContext g) {
        g.setFont(hubFont);
        g.setTextAlign(TextAlignment.CENTER);
        g.setTextBaseline(VPos.TOP);
        Color c = Color.web("#5eead4");
        for (Hub hb : hubs) {
            // calm radar rings
            g.setLineWidth(1.4 * scale);
            for (int k = 0; k < 2; k++) {
                double ph = ((time / 3.4) + hb.phase + k * 0.5) % 1.0;
                double r = (9 + 34 * ph) * scale;
                g.setStroke(alpha(c, 0.22 * Math.pow(1 - ph, 1.6)));
                g.strokeOval(hb.x - r, hb.y - r, r * 2, r * 2);
            }

            // bigger burst when a truck arrives
            double age = time - hb.lastArrival;
            if (age >= 0 && age < 1.8) {
                double p = age / 1.8;
                double ease = 1 - Math.pow(1 - p, 3);
                g.setLineWidth(2 * scale);
                for (int k = 0; k < 2; k++) {
                    double r = (10 + (60 + k * 26) * ease) * scale;
                    g.setStroke(alpha(Color.web("#7dd3fc"), 0.55 * (1 - p)));
                    g.strokeOval(hb.x - r, hb.y - r, r * 2, r * 2);
                }
            }

            // node
            g.setFill(glow(hb.x, hb.y, 20 * scale, c, 0.35));
            g.fillOval(hb.x - 20 * scale, hb.y - 20 * scale, 40 * scale, 40 * scale);
            g.setFill(rgba(3, 12, 30, 0.96));
            g.fillOval(hb.x - 8 * scale, hb.y - 8 * scale, 16 * scale, 16 * scale);
            g.setStroke(alpha(c, 0.95));
            g.setLineWidth(2 * scale);
            g.strokeOval(hb.x - 8 * scale, hb.y - 8 * scale, 16 * scale, 16 * scale);
            g.setFill(alpha(c, 1));
            g.fillOval(hb.x - 3 * scale, hb.y - 3 * scale, 6 * scale, 6 * scale);

            // label
            g.setFill(rgba(0, 0, 0, 0.7));
            g.fillText(hb.name, hb.x + 1, hb.y + 14 * scale + 1);
            g.setFill(rgba(226, 232, 240, 0.92));
            g.fillText(hb.name, hb.x, hb.y + 14 * scale);
        }
    }

    private void drawTrail(GraphicsContext g, Truck t) {
        int n = t.trail.size();
        if (n < 2) return;
        g.setLineCap(StrokeLineCap.ROUND);
        double[] prev = null;
        int i = 0;
        for (double[] p : t.trail) {
            if (prev != null) {
                double f = i / (double) n;
                g.setStroke(alpha(t.color, 0.70 * f));
                g.setLineWidth((1.0 + 3.0 * f) * scale);
                g.strokeLine(prev[0], prev[1], p[0], p[1]);
            }
            prev = p;
            i++;
        }
    }

    /** Top-down truck (cab + cargo box), rotated to the direction of the road. */
    private void drawTruck(GraphicsContext g, Truck t) {
        double ts = 1.12 * scale;
        Color body = t.color;
        Color cab = t.color.interpolate(Color.WHITE, 0.45);
        Color dark = Color.web("#0b1220");

        g.save();
        g.translate(t.x, t.y);
        g.rotate(t.heading);
        g.scale(ts, ts);

        // aura
        g.setFill(glow(0, 0, 34, body, 0.30));
        g.fillOval(-34, -34, 68, 68);

        // headlight beam
        g.setFill(new LinearGradient(19, 0, 62, 0, false, CycleMethod.NO_CYCLE,
                new Stop(0, rgba(255, 255, 255, 0.26)), new Stop(1, rgba(255, 255, 255, 0))));
        g.fillPolygon(new double[]{18, 62, 62}, new double[]{0, -16, 16}, 3);

        // soft shadow
        g.setFill(rgba(0, 0, 0, 0.35));
        g.fillRoundRect(-20, -4, 41, 13, 4, 4);

        // wheels (poke out on both sides)
        g.setFill(dark);
        double[] wheelX = {-15, -9, 13};
        for (double wx : wheelX) {
            g.fillRoundRect(wx - 3.2, -8.2, 6.4, 2.8, 1, 1);
            g.fillRoundRect(wx - 3.2, 5.4, 6.4, 2.8, 1, 1);
        }

        // cargo box
        g.setFill(body);
        g.fillRoundRect(-21, -6, 27, 12, 3, 3);
        g.setStroke(rgba(0, 0, 0, 0.28));
        g.setLineWidth(0.9);
        for (int i = 0; i < 4; i++) {
            double lx = -16 + i * 5.5;
            g.strokeLine(lx, -5.4, lx, 5.4);
        }
        g.setFill(rgba(255, 255, 255, 0.18));
        g.fillRoundRect(-20, -5.2, 25, 3, 2, 2);

        // coupling
        g.setFill(dark);
        g.fillRect(6, -1.6, 3, 3.2);

        // cab
        g.setFill(cab);
        g.fillRoundRect(8.5, -5.2, 11, 10.4, 3.5, 3.5);
        g.setFill(rgba(8, 20, 40, 0.92));
        g.fillRoundRect(14.4, -4.0, 3.6, 8, 1.5, 1.5);

        // headlights
        g.setFill(Color.web("#fef9c3"));
        g.fillOval(18.6, -4.8, 2, 2.2);
        g.fillOval(18.6, 2.6, 2, 2.2);

        g.restore();
    }

    private void drawTag(GraphicsContext g, Truck t) {
        double padX = 7 * scale, tagH = 18 * scale;
        double tagW = t.labelW + padX * 2;
        double x = t.x - tagW / 2;
        double y = t.y - 30 * scale - tagH;

        g.setFill(alpha(t.color, 0.90));
        g.fillRoundRect(x, y, tagW, tagH, 6 * scale, 6 * scale);
        g.setStroke(alpha(Color.WHITE, 0.35));
        g.setLineWidth(1);
        g.strokeRoundRect(x, y, tagW, tagH, 6 * scale, 6 * scale);

        g.setFont(tagFont);
        g.setTextAlign(TextAlignment.CENTER);
        g.setTextBaseline(VPos.CENTER);
        g.setFill(Color.web("#04121f"));
        g.fillText(t.id, t.x, y + tagH / 2);
    }

    private void drawHud(GraphicsContext g) {
        g.setTextBaseline(VPos.CENTER);
        g.setTextAlign(TextAlignment.RIGHT);

        // bottom-right telemetry strip
        double y = h - 34;
        double x = w - 44;
        g.setFont(hudBoldFont);
        g.setFill(Color.web("#4ade80"));
        g.fillText("OPTIMAL", x, y);
        g.setFont(hudFont);
        g.setFill(rgba(148, 163, 184, 0.95));
        g.fillText("Network Status: ", x - wOptimal, y);
        g.fillText("Active Units: " + trucks.length + "        ", x - wOptimal - wNetwork, y);

        // top-right LIVE indicator
        double ly = 40;
        double lx = w - 44;
        g.setFont(hudBoldFont);
        g.setFill(rgba(226, 232, 240, 0.9));
        g.fillText("LIVE TRACKING", lx, ly);
        double blink = 0.45 + 0.55 * (0.5 + 0.5 * Math.sin(time * 4));
        double dotX = lx - wLive - 14;
        g.setFill(rgba(239, 68, 68, 0.25 * blink));
        g.fillOval(dotX - 8, ly - 8, 16, 16);
        g.setFill(rgba(239, 68, 68, blink));
        g.fillOval(dotX - 4, ly - 4, 8, 8);
    }

    // ------------------------------------------------------------------ helpers
    private static Color rgba(int r, int g, int b, double a) {
        return Color.rgb(r, g, b, Math.max(0, Math.min(1, a)));
    }

    private static Color alpha(Color c, double a) {
        return new Color(c.getRed(), c.getGreen(), c.getBlue(), Math.max(0, Math.min(1, a)));
    }

    /** Soft radial glow in absolute pixel coordinates. */
    private static RadialGradient glow(double cx, double cy, double radius, Color c, double a) {
        return new RadialGradient(0, 0, cx, cy, radius, false, CycleMethod.NO_CYCLE,
                new Stop(0, alpha(c, a)), new Stop(1, alpha(c, 0)));
    }

    // ------------------------------------------------------------------ small data classes
    private static final class Hub {
        final String name;
        final double phase;
        double x, y;
        double lastArrival = -100;

        Hub(String name, double phase) {
            this.name = name;
            this.phase = phase;
        }
    }

    private static final class Particle {
        double x, y, vx, vy, size, phase, speed;

        Particle(Random r) {
            x = r.nextDouble();
            y = r.nextDouble();
            vx = (r.nextDouble() - 0.5) * 0.010;
            vy = (r.nextDouble() - 0.5) * 0.010;
            size = 0.8 + r.nextDouble() * 1.3;
            phase = r.nextDouble() * 6.28;
            speed = 0.8 + r.nextDouble() * 1.6;
        }
    }

    /** A drivable route: a polyline along real streets, with cumulative length for constant-speed travel. */
    private static final class RoadPath {
        final double[] x, y, cum;
        final double total;

        RoadPath(ArrayList<double[]> pts) {
            ArrayList<double[]> clean = new ArrayList<>();
            for (double[] p : pts) {
                if (clean.isEmpty()) {
                    clean.add(p);
                } else {
                    double[] q = clean.get(clean.size() - 1);
                    if (Math.hypot(p[0] - q[0], p[1] - q[1]) > 0.05) clean.add(p);
                }
            }
            if (clean.size() < 2) {                      // degenerate: make it a tiny segment
                double[] p = clean.get(0);
                clean.add(new double[]{p[0] + 1, p[1]});
            }
            int n = clean.size();
            x = new double[n];
            y = new double[n];
            cum = new double[n];
            for (int i = 0; i < n; i++) {
                x[i] = clean.get(i)[0];
                y[i] = clean.get(i)[1];
                cum[i] = (i == 0) ? 0 : cum[i - 1] + Math.hypot(x[i] - x[i - 1], y[i] - y[i - 1]);
            }
            total = Math.max(1, cum[n - 1]);
        }

        /** out = {x, y, tangentX, tangentY}. Returns the index of the point just behind the position. */
        int locate(double d, double[] out) {
            d = Math.max(0, Math.min(total, d));
            int lo = 0, hi = cum.length - 1;
            while (hi - lo > 1) {
                int mid = (lo + hi) >>> 1;
                if (cum[mid] <= d) lo = mid; else hi = mid;
            }
            double span = Math.max(1e-6, cum[hi] - cum[lo]);
            double f = (d - cum[lo]) / span;
            out[0] = x[lo] + (x[hi] - x[lo]) * f;
            out[1] = y[lo] + (y[hi] - y[lo]) * f;
            double tx = x[hi] - x[lo], ty = y[hi] - y[lo];
            double tl = Math.max(1e-6, Math.hypot(tx, ty));
            out[2] = tx / tl;
            out[3] = ty / tl;
            return lo;
        }
    }

    private static final class Truck {
        final int index;
        final String id;
        final Color color;
        final int[] loop;
        final ArrayDeque<double[]> trail = new ArrayDeque<>();

        RoadPath path;
        int leg;                 // which hub of the loop the truck is leaving from
        int pathIdx;             // index of the path point just behind the truck
        double dist;             // distance travelled on the current route (px)
        double u;                // same as a 0..1 fraction (kept across window resizes)
        double dwell;            // seconds left to wait at a hub
        double x, y, heading;    // heading in degrees
        double labelW;

        Truck(int index, String id, Color color, int[] loop) {
            this.index = index;
            this.id = id;
            this.color = color;
            this.loop = loop;
        }
    }
}