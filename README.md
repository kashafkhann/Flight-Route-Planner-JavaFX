# Flight-Route-Planner-JavaFX

A modern JavaFX desktop application for planning, visualizing, and managing flight routes and airport networks. Effortlessly add airports and routes, search for the most efficient or direct paths, and interact with an intuitive graphical map in a responsive, accessible interface.

---

## ✈️ Features

- **Airport & Route Management:** Easily add or remove airports and define routes with costs and real-world distances.
- **Cheapest & Direct Routes:** Find the most cost-efficient or direct paths between airports.
- **Interactive Map:** Visualize your airport network and flight routes on a dynamic, user-friendly map.
- **Save & Load Data:** Save your network to a file and reload it at any time.
- **Accessibility & Dark Mode:** Enjoy a clean, accessible UI with optional dark mode for comfortable viewing.

---

## 🚀 Getting Started

### Requirements

- **Java 11** or newer
- **JavaFX SDK** (Download from [https://openjfx.io/](https://openjfx.io/))

### Setup Instructions

1. **Clone the repository**
   ```sh
   git clone https://github.com/kashafkhann/Flight-Route-Planner-JavaFX.git
   cd Flight-Route-Planner-JavaFX
   ```

2. **Compile the project**
   ```sh
   javac FlightNetwork.java FlightTrackerApp.java
   ```

3. **Run the application**
   ```sh
   java --module-path /path/to/javafx/lib --add-modules javafx.controls,javafx.fxml FlightTrackerApp
   ```
   _Replace `/path/to/javafx/lib` with the path to your JavaFX SDK's `lib` directory._


---

## 💡 Usage

- **Add Airports:** Enter airport names to expand your network.
- **Add Routes:** Specify departure, destination, cost, and distance.
- **Find Routes:** Use the search to find cheapest or direct connections.
- **View Map:** The main window displays all airports and routes visually.
- **Save/Load:** Manage your networks by saving to and loading from files.
- **Switch Modes:** Toggle dark mode or accessibility settings from the menu.

---

## 📁 File Structure

```
Flight-Route-Planner-JavaFX/
├── FlightNetwork.java         # Backend logic for airports and routes
├── FlightTrackerApp.java      # JavaFX application (frontend)
├── README.md                  # Project documentation
├── LICENSE                    # License file (MIT by default)
└── .gitignore                 # Git ignore patterns
```

---

## 📝 License

This project is licensed under the [MIT License](LICENSE).

---

## 🤝 Contributing

Contributions are welcome! Please open an issue or submit a pull request for suggestions and improvements.

---

## 👤 Author

- [@kashafkhann](https://github.com/kashafkhann)

---

## ⭐️ Show your support

If you like this project, please give it a ⭐️ on GitHub!