import javafx.application.Application;
import javafx.beans.value.ChangeListener;
import javafx.collections.*;
import javafx.geometry.*;
import javafx.scene.*;
import javafx.scene.canvas.*;
import javafx.scene.control.*;
import javafx.scene.input.*;
import javafx.scene.layout.*;
import javafx.scene.paint.*;
import javafx.scene.text.*;
import javafx.stage.*;
import java.io.*;
import java.util.*;
import java.util.stream.Collectors;

public class FlightTrackerApp extends Application {
    private FlightNetwork net = new FlightNetwork();
    private final ObservableList<String> airportList = FXCollections.observableArrayList();
    private final Canvas mapCanvas = new Canvas(800, 500);
    private boolean darkMode = false;

    @Override
    public void start(Stage stage) {
        stage.setTitle("Flight Route Tracker");

        BorderPane root = new BorderPane();
        root.setPadding(new Insets(10));

        // Airport list and controls
        ListView<String> airportLV = new ListView<>(airportList);
        airportLV.setFocusTraversable(true);

        Button addAirportBtn = new Button("Add Airport");
        Button removeAirportBtn = new Button("Remove Selected");
        HBox airportBtns = new HBox(5, addAirportBtn, removeAirportBtn);

        VBox airportBox = new VBox(5, new Label("Airports:"), airportLV, airportBtns);
        airportBox.setPrefWidth(220);

        // Route controls
        ComboBox<String> fromCB = new ComboBox<>(airportList), toCB = new ComboBox<>(airportList);
        TextField costField = new TextField();
        costField.setPromptText("Cost $");
        Button addRouteBtn = new Button("Add Route");
        Button removeRouteBtn = new Button("Remove Route");
        HBox routeBox = new HBox(5, new Label("From:"), fromCB, new Label("To:"), toCB, costField, addRouteBtn, removeRouteBtn);

        // Search
        ComboBox<String> searchFromCB = new ComboBox<>(airportList), searchToCB = new ComboBox<>(airportList);
        CheckBox cheapestCB = new CheckBox("Cheapest");
        CheckBox directCB = new CheckBox("Direct only");
        Button searchBtn = new Button("Find Route");
        HBox searchBox = new HBox(5, new Label("From:"), searchFromCB, new Label("To:"), searchToCB, cheapestCB, directCB, searchBtn);

        // File save/load
        Button saveBtn = new Button("Save Network"), loadBtn = new Button("Load Network");
        Button darkModeBtn = new Button("Dark Mode");
        HBox fileBox = new HBox(5, saveBtn, loadBtn, darkModeBtn);

        // Error message label
        Label msgLabel = new Label("Ready.");
        msgLabel.setTextFill(Color.GREEN);
        msgLabel.setWrapText(true);

        VBox left = new VBox(15, airportBox, routeBox, searchBox, fileBox, msgLabel);
        left.setPrefWidth(300);

        // Map visualization
        StackPane mapPane = new StackPane(mapCanvas);
        mapPane.setStyle("-fx-background-color: #f4f4f4;"); // Light bg

        root.setLeft(left);
        root.setCenter(mapPane);

        // Accessibility
        airportLV.setAccessibleText("List of airports");
        addAirportBtn.setAccessibleText("Add new airport");
        removeAirportBtn.setAccessibleText("Remove selected airport");
        fromCB.setAccessibleText("Select source airport");
        toCB.setAccessibleText("Select destination airport");
        costField.setAccessibleText("Enter route cost");
        addRouteBtn.setAccessibleText("Add route");
        removeRouteBtn.setAccessibleText("Remove route");
        searchFromCB.setAccessibleText("Source for route search");
        searchToCB.setAccessibleText("Destination for route search");
        cheapestCB.setAccessibleText("Search for cheapest");
        directCB.setAccessibleText("Direct flights only");
        searchBtn.setAccessibleText("Search route");
        saveBtn.setAccessibleText("Save network");
        loadBtn.setAccessibleText("Load network");

        // Populate airport list UI when model changes
        Runnable updateAirportList = () -> {
            airportList.setAll(net.getAirports().stream().map(a -> a.toString()).collect(Collectors.toList()));
            fromCB.setItems(airportList);
            toCB.setItems(airportList);
            searchFromCB.setItems(airportList);
            searchToCB.setItems(airportList);
            drawMap(null);
        };
        updateAirportList.run();

        // Add airport dialog
        addAirportBtn.setOnAction(e -> {
            Dialog<FlightNetwork.Airport> dialog = new Dialog<>();
            dialog.setTitle("Add Airport");
            GridPane pane = new GridPane();
            pane.setHgap(5); pane.setVgap(5);
            TextField code = new TextField(), name = new TextField(), country = new TextField(), lat = new TextField(), lon = new TextField();
            pane.add(new Label("IATA Code:"), 0, 0); pane.add(code, 1, 0);
            pane.add(new Label("Name:"), 0, 1); pane.add(name, 1, 1);
            pane.add(new Label("Country:"), 0, 2); pane.add(country, 1, 2);
            pane.add(new Label("Latitude:"), 0, 3); pane.add(lat, 1, 3);
            pane.add(new Label("Longitude:"), 0, 4); pane.add(lon, 1, 4);
            dialog.getDialogPane().setContent(pane);
            dialog.getDialogPane().getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);
            dialog.setResultConverter(btn -> {
                if (btn == ButtonType.OK) {
                    try {
                        return new FlightNetwork.Airport(code.getText().trim().toUpperCase(), name.getText(), country.getText(),
                                Double.parseDouble(lat.getText()), Double.parseDouble(lon.getText()));
                    } catch (Exception ex) { return null; }
                }
                return null;
            });
            Optional<FlightNetwork.Airport> result = dialog.showAndWait();
            if (result.isPresent() && net.addAirport(result.get())) {
                msgLabel.setText("Added airport " + result.get().toString());
                msgLabel.setTextFill(Color.GREEN);
                updateAirportList.run();
            } else if (result.isPresent()) {
                msgLabel.setText("Failed to add airport (maybe duplicate code?)");
                msgLabel.setTextFill(Color.RED);
            }
        });

        removeAirportBtn.setOnAction(e -> {
            String sel = airportLV.getSelectionModel().getSelectedItem();
            if (sel == null) {
                msgLabel.setText("Select an airport to remove.");
                msgLabel.setTextFill(Color.RED);
                return;
            }
            String code = sel.split(" ")[0];
            if (net.removeAirport(code)) {
                msgLabel.setText("Removed airport " + code);
                msgLabel.setTextFill(Color.GREEN);
                updateAirportList.run();
            }
        });

        addRouteBtn.setOnAction(e -> {
            String from = fromCB.getValue(), to = toCB.getValue();
            if (from == null || to == null) {
                msgLabel.setText("Select both airports.");
                msgLabel.setTextFill(Color.RED); return;
            }
            try {
                double cost = Double.parseDouble(costField.getText());
                String fromCode = from.split(" ")[0], toCode = to.split(" ")[0];
                if (net.addRoute(fromCode, toCode, cost)) {
                    msgLabel.setText("Added route " + fromCode + "->" + toCode);
                    msgLabel.setTextFill(Color.GREEN);
                    drawMap(null);
                } else {
                    msgLabel.setText("Failed to add route.");
                    msgLabel.setTextFill(Color.RED);
                }
            } catch (NumberFormatException ex) {
                msgLabel.setText("Enter a valid cost.");
                msgLabel.setTextFill(Color.RED);
            }
        });

        removeRouteBtn.setOnAction(e -> {
            String from = fromCB.getValue(), to = toCB.getValue();
            if (from == null || to == null) {
                msgLabel.setText("Select both airports.");
                msgLabel.setTextFill(Color.RED); return;
            }
            String fromCode = from.split(" ")[0], toCode = to.split(" ")[0];
            if (net.removeRoute(fromCode, toCode)) {
                msgLabel.setText("Removed route " + fromCode + "->" + toCode);
                msgLabel.setTextFill(Color.GREEN);
                drawMap(null);
            } else {
                msgLabel.setText("Failed to remove route.");
                msgLabel.setTextFill(Color.RED);
            }
        });

        searchBtn.setOnAction(e -> {
            String from = searchFromCB.getValue(), to = searchToCB.getValue();
            if (from == null || to == null) {
                msgLabel.setText("Select both airports.");
                msgLabel.setTextFill(Color.RED); return;
            }
            String fromCode = from.split(" ")[0], toCode = to.split(" ")[0];
            boolean byCost = cheapestCB.isSelected(), directOnly = directCB.isSelected();
            List<FlightNetwork.Route> path = net.findRoute(fromCode, toCode, byCost, directOnly);
            if (path == null) {
                msgLabel.setText("No route found.");
                msgLabel.setTextFill(Color.RED);
                drawMap(null);
            } else {
                double totalCost = path.stream().mapToDouble(r -> r.cost).sum();
                double totalDist = path.stream().mapToDouble(r -> r.distance).sum();
                double totalTime = path.stream().mapToDouble(r -> r.flightTime).sum();
                msgLabel.setText("Route: " + path + "\nTotal: $" + String.format("%.2f", totalCost)
                        + ", " + String.format("%.1f km", totalDist)
                        + ", " + String.format("%.1f h", totalTime));
                msgLabel.setTextFill(Color.DARKBLUE);
                drawMap(path);
            }
        });

        saveBtn.setOnAction(e -> {
            FileChooser fc = new FileChooser();
            fc.setTitle("Save Network");
            File f = fc.showSaveDialog(stage);
            if (f != null) {
                try {
                    net.saveToFile(f.getAbsolutePath());
                    msgLabel.setText("Saved!");
                    msgLabel.setTextFill(Color.GREEN);
                } catch (Exception ex) {
                    msgLabel.setText("Failed to save.");
                    msgLabel.setTextFill(Color.RED);
                }
            }
        });

        loadBtn.setOnAction(e -> {
            FileChooser fc = new FileChooser();
            fc.setTitle("Load Network");
            File f = fc.showOpenDialog(stage);
            if (f != null) {
                try {
                    net = FlightNetwork.loadFromFile(f.getAbsolutePath());
                    msgLabel.setText("Loaded!");
                    msgLabel.setTextFill(Color.GREEN);
                    updateAirportList.run();
                } catch (Exception ex) {
                    msgLabel.setText("Failed to load.");
                    msgLabel.setTextFill(Color.RED);
                }
            }
        });

        darkModeBtn.setOnAction(e -> {
            darkMode = !darkMode;
            mapPane.setStyle(darkMode ? "-fx-background-color: #222;" : "-fx-background-color: #f4f4f4;");
            darkModeBtn.setText(darkMode ? "Light Mode" : "Dark Mode");
            drawMap(null);
        });

        // Responsive map
        ChangeListener<Number> resizeListener = (obs, oldV, newV) -> {
            mapCanvas.setWidth(mapPane.getWidth());
            mapCanvas.setHeight(mapPane.getHeight());
            drawMap(null);
        };
        mapPane.widthProperty().addListener(resizeListener);
        mapPane.heightProperty().addListener(resizeListener);

        Scene scene = new Scene(root, 1050, 650);
        scene.getStylesheets().add(getClass().getResource("flighttracker.css") == null ? "" : getClass().getResource("flighttracker.css").toExternalForm());
        if (darkMode) scene.getRoot().setStyle("-fx-base: #222; -fx-text-fill: #fff;");
        // Keyboard navigation for accessibility
        scene.setOnKeyPressed(e -> {
            if (e.getCode() == KeyCode.TAB) airportLV.requestFocus();
        });
        stage.setScene(scene);
        stage.show();
    }

    // Draw airports as nodes, routes as lines, highlight path if present
    private void drawMap(List<FlightNetwork.Route> highlightPath) {
        GraphicsContext gc = mapCanvas.getGraphicsContext2D();
        gc.clearRect(0, 0, mapCanvas.getWidth(), mapCanvas.getHeight());
        double w = mapCanvas.getWidth(), h = mapCanvas.getHeight();
        // Get all airports and map lat/lon to screen
        List<FlightNetwork.Airport> airports = net.getAirports();
        if (airports.isEmpty()) return;
        double minLat = airports.stream().mapToDouble(a -> a.latitude).min().getAsDouble();
        double maxLat = airports.stream().mapToDouble(a -> a.latitude).max().getAsDouble();
        double minLon = airports.stream().mapToDouble(a -> a.longitude).min().getAsDouble();
        double maxLon = airports.stream().mapToDouble(a -> a.longitude).max().getAsDouble();
        Map<String, Point2D> posMap = new HashMap<>();
        for (FlightNetwork.Airport a : airports) {
            double x = (a.longitude - minLon) / (maxLon - minLon + 0.01) * (w - 100) + 50;
            double y = (maxLat - a.latitude) / (maxLat - minLat + 0.01) * (h - 100) + 50;
            posMap.put(a.code, new Point2D(x, y));
        }
        // Draw routes
        for (FlightNetwork.Airport a : airports) {
            for (FlightNetwork.Route r : net.getRoutesFrom(a.code)) {
                Point2D from = posMap.get(r.from), to = posMap.get(r.to);
                if (from == null || to == null) continue;
                gc.setStroke(Color.GRAY);
                gc.setLineWidth(2);
                gc.strokeLine(from.getX(), from.getY(), to.getX(), to.getY());
            }
        }
        // Highlighted path
        if (highlightPath != null) {
            gc.setStroke(Color.ORANGE);
            gc.setLineWidth(4.5);
            for (FlightNetwork.Route r : highlightPath) {
                Point2D from = posMap.get(r.from), to = posMap.get(r.to);
                gc.strokeLine(from.getX(), from.getY(), to.getX(), to.getY());
            }
        }
        // Draw airports
        for (FlightNetwork.Airport a : airports) {
            Point2D p = posMap.get(a.code);
            gc.setFill(Color.AQUA);
            gc.fillOval(p.getX() - 8, p.getY() - 8, 16, 16);
            gc.setStroke(Color.DARKBLUE);
            gc.strokeOval(p.getX() - 8, p.getY() - 8, 16, 16);
            gc.setFill(darkMode ? Color.WHITE : Color.BLACK);
            gc.setFont(Font.font("Arial", FontWeight.BOLD, 13));
            gc.fillText(a.code, p.getX() + 9, p.getY() + 5);
        }
        // Airport metadata on hover
        mapCanvas.setOnMouseMoved(e -> {
            for (FlightNetwork.Airport a : airports) {
                Point2D p = posMap.get(a.code);
                if (p.distance(e.getX(), e.getY()) < 14) {
                    Tooltip t = new Tooltip(a.toString() + "\nLat: " + a.latitude + ", Lon: " + a.longitude);
                    Tooltip.install(mapCanvas, t);
                    return;
                }
            }
            Tooltip.uninstall(mapCanvas, null);
        });
    }

    public static void main(String[] args) {
        launch(args);
    }
}