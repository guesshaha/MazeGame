import javafx.scene.layout.StackPane;
import javafx.scene.text.Text;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.scene.paint.Color;
import javafx.scene.shape.Rectangle;
import javafx.scene.Group;

public class Grid extends Group
{

    private Tile[][] tiles;

    public Grid(int numRows, int numCols, double screenHeight, double gridOffset, double tileOffset)
    {
        super();
        double tileHeight = (screenHeight - 2 * gridOffset) / numCols - tileOffset;
        double tileWidth = tileHeight;
        double startX = -numCols * 0.5 * (tileWidth + tileOffset);
        double startY = -numRows * 0.5 * (tileHeight + tileOffset);

        tiles = new Tile[numRows][numCols];
        for (int row = 0; row < numRows; row++)
        {
            for (int col = 0; col < numCols; col++)
            {
                Tile tile = new Tile(tileWidth, tileHeight);
                tile.setTranslateX(startX + col * (tileWidth + tileOffset));
                tile.setTranslateY(startY + row * (tileHeight + tileOffset));
                getChildren().add(tile);
                tiles[row][col] = tile;
            }
        }
    }

    public void update(GameState gameState)
    {
        for (int row = 0; row < gameState.gridSize(); row++)
        {
            for (int col = 0; col < gameState.gridSize(); col++)
            {
                tiles[row][col].updateText(gameState.getOccupantAsString(row, col));
            }
        }
    }

    private class Tile extends StackPane
    {

        private Text text;

        public Tile(double tileWidth, double tileHeight)
        {
            super();
            Rectangle rect = new Rectangle(0, 0, tileWidth, tileHeight);
            rect.setFill(Color.rgb(10, 191, 188));
            getChildren().add(rect);

            text = new Text();
            text.setFont(Font.font("Verdana", FontWeight.BOLD, 12));
            getChildren().add(text);
            updateText("");
        }

        public void updateText(String s)
        {
            text.setText(s);
            if (s == "*")
            {
                text.setFill(Color.rgb(252, 53, 76));
            } else if (s == "")
            {
            } else
            {
                assert s.length() == 2 : "Trying to set text " + s + " which does not have exactly 2 characters.";
                text.setFill(Color.rgb(252, 247, 197));
            }
        }
    }
}
