import java.io.*;
import java.util.*;
import java.util.stream.Collectors;

public class FlightNetwork implements Serializable {
    public static class Airport implements Serializable {
        public final String code, name, country;
        public final double latitude, longitude;

        public Airport(String code, String name, String country, double latitude, double longitude) {
            this.code = code;
            this.name = name;
            this.country = country;
            this.latitude = latitude;
            this.longitude = longitude;
        }

        @Override
        public String toString() {
            return code + " (" + name + ", " + country + ")";
        }
    }

    public static class Route implements Serializable {
        public final String from, to;
        public double cost, distance, flightTime;

        public Route(String from, String to, double cost, double distance, double flightTime) {
            this.from = from;
            this.to = to;
            this.cost = cost;
            this.distance = distance;
            this.flightTime = flightTime;
        }

        @Override
        public String toString() {
            return from + " -> " + to + " | $" + cost + " | " + String.format("%.1f km", distance) + " | " + String.format("%.1f h", flightTime);
        }
    }

    private final Map<String, Airport> airports = new HashMap<>();
    private final Map<String, List<Route>> routes = new HashMap<>();

    public boolean addAirport(Airport a) {
        if (a == null || airports.containsKey(a.code)) return false;
        airports.put(a.code, a);
        return true;
    }

    public boolean removeAirport(String code) {
        if (!airports.containsKey(code)) return false;
        airports.remove(code);
        routes.remove(code);
        for (List<Route> rts : routes.values())
            rts.removeIf(r -> r.to.equals(code));
        return true;
    }

    public boolean addRoute(String from, String to, double cost) {
        if (!airports.containsKey(from) || !airports.containsKey(to)) return false;
        double dist = haversine(airports.get(from).latitude, airports.get(from).longitude, airports.get(to).latitude, airports.get(to).longitude);
        double flightTime = dist / 800.0; // Assume avg 800 km/h
        Route r = new Route(from, to, cost, dist, flightTime);
        routes.computeIfAbsent(from, k -> new ArrayList<>()).add(r);
        return true;
    }

    public boolean removeRoute(String from, String to) {
        if (!routes.containsKey(from)) return false;
        return routes.get(from).removeIf(r -> r.to.equals(to));
    }

    public List<Airport> getAirports() {
        return new ArrayList<>(airports.values());
    }

    public List<Route> getRoutesFrom(String airportCode) {
        return routes.getOrDefault(airportCode, new ArrayList<>());
    }

    // Dijkstra for shortest path by cost or distance, with direct only option
    public List<Route> findRoute(String start, String end, boolean byCost, boolean directOnly) {
        if (!airports.containsKey(start) || !airports.containsKey(end)) return null;
        Map<String, Double> dist = new HashMap<>();
        Map<String, String> prev = new HashMap<>();
        Map<String, Route> prevRoute = new HashMap<>();
        Set<String> visited = new HashSet<>();
        PriorityQueue<String> pq = new PriorityQueue<>(Comparator.comparingDouble(dist::get));
        for (String code : airports.keySet()) dist.put(code, Double.POSITIVE_INFINITY);
        dist.put(start, 0.0);
        pq.add(start);

        while (!pq.isEmpty()) {
            String u = pq.poll();
            if (!visited.add(u)) continue;
            if (u.equals(end)) break;
            for (Route r : getRoutesFrom(u)) {
                if (directOnly && !r.to.equals(end)) continue;
                double alt = dist.get(u) + (byCost ? r.cost : r.distance);
                if (alt < dist.get(r.to)) {
                    dist.put(r.to, alt);
                    prev.put(r.to, u);
                    prevRoute.put(r.to, r);
                    pq.add(r.to);
                }
            }
        }
        if (!prevRoute.containsKey(end)) return null;
        LinkedList<Route> path = new LinkedList<>();
        String cur = end;
        while (!cur.equals(start)) {
            Route r = prevRoute.get(cur);
            path.addFirst(r);
            cur = prev.get(cur);
        }
        return path;
    }

    public void saveToFile(String filename) throws IOException {
        try (ObjectOutputStream o = new ObjectOutputStream(new FileOutputStream(filename))) {
            o.writeObject(this);
        }
    }

    public static FlightNetwork loadFromFile(String filename) throws IOException, ClassNotFoundException {
        try (ObjectInputStream o = new ObjectInputStream(new FileInputStream(filename))) {
            return (FlightNetwork) o.readObject();
        }
    }

    // Haversine distance in km
    private static double haversine(double lat1, double lon1, double lat2, double lon2) {
        double dLat = Math.toRadians(lat2 - lat1), dLon = Math.toRadians(lon2 - lon1);
        double a = Math.sin(dLat / 2) * Math.sin(dLat / 2)
                + Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2))
                * Math.sin(dLon / 2) * Math.sin(dLon / 2);
        return 6371.0 * 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
    }
}