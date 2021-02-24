import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Random;

/**
 * Call a copy of the function {@link #randomiseGrid} from PuzzleActivity a given amount of times and record
 * the return values (a String representing a unique solvable grid for the app) in a plain text file saved in the
 * folder @GridRandomiserTesting/test data with a given file name
 * ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
 * UPDATED
 * ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
 * Modified to create a HashMap to count frequency of each unique grid then write the grid names and frequency to the file.
 * This makes the files much smaller size, and means less work to do in the python script to analyse the data
 */
public class RandomiserGenerator {

    public static void main(String[] args) throws IOException {
        for (String s : args) {  // can use for command line arguments
            System.out.println(s);
        }

        // set parameters for creating dataset
        int numGenerated = 1;
        int gridSize = 9;  // NOTE: use gridsize not cols
        // create File object and check if the file exists already, to add modifier if necessary so no conflict occurs
        String fileName = "gridsize(" + gridSize + ")_numgrids(" + numGenerated + ")_tableform";
        String folderPath = "..\\test data\\";
        File generatedFile = new File(folderPath + fileName + ".txt");
        int copyDistinguisher = 0;
        while (generatedFile.exists()) {  // add suffix term to avoid conflict
            System.out.println("file already exists");
            copyDistinguisher += 1;
            generatedFile = new File(folderPath + fileName + "_copy(" + copyDistinguisher + ").txt");
        }
        long startTime = System.currentTimeMillis();
        System.out.println("writing to file...");
        // create the File in current folder using given name, and call randomiser function to generate grids which are
        // written to the file, each as a String of values separated by commas and a newline ("\n")
        boolean bool = generatedFile.createNewFile();
        FileWriter writer = new FileWriter(generatedFile);
        Random random = new Random();
        StringBuilder stringBuilder = new StringBuilder();

        String gridString;
        Integer gridFrequency;
        // randomly generate the set amount of (solvable) grids using the randomise function
        // convert to string, append to stringbuilder, and write the stringBuilder contents to the file with a FileWriter
        // Use HashMap to determine unique grids and frequencies and only write this data to the file
        HashMap<String, Integer> frequencyTable = new HashMap<>();  // can set initial capacity to num combinations?
        for (int i = 0; i < numGenerated; i++) {
            StringBuilder gridBuilder = new StringBuilder();
            for (Integer num : randomiseGrid(gridSize, random)) {
                gridBuilder.append(num.toString()).append(",");
            }
            gridString = gridBuilder.toString();
            gridFrequency = frequencyTable.get(gridString);
            if (gridFrequency == null) {  // if no key value pair for that unique grid, set it with value 1
                frequencyTable.put(gridString, 1);
            } else {  // there is a key for that grid already, so get its value (frequency) and increment it by 1
                gridFrequency += 1;
                frequencyTable.put(gridString, gridFrequency);
            }
        }
        // create file contents with stringBuilder in the form: gridString, frequency\n
        for (String uniqueGrid : frequencyTable.keySet()) {
            stringBuilder.append(uniqueGrid).append(" ").append(frequencyTable.get(uniqueGrid)).append("\n");
        }
        writer.append(stringBuilder.toString());
        writer.flush();
        writer.close();

        long elapsedTimeSecs = (System.currentTimeMillis() - startTime) / 1000;
        System.out.println("File Written. Time taken(secs): " + elapsedTimeSecs);
    }

    // function which generates a random solvable n x n grid of integers 1 - (n x n - 1) in list form (l->r, t->b)
    private static ArrayList<Integer> randomiseGrid(int gridSize, Random random) {
        // initialise objects and set variables
        ArrayList<Integer> randomisedGrid = new ArrayList<>();
        ArrayList<Integer> posPool = new ArrayList<>();

        while (true) {  // create randomised grid, check if solvable, then break if it is
            // initialise variables for start of each loop
            int bound = gridSize - 1;  // bounds for random generator...between 0 (inclusive) and number (exclusive)
            randomisedGrid.clear();
            for (int x = 0; x < gridSize - 1; x++) {  // pool for random indexes to be drawn from - exclude last cell index
                posPool.add(x);
            }

            // randomise grid and create list with outcome
            for (int x = 0; x < gridSize; x++) {
                if (x == gridSize - 1) {  // add last index to last in list to ensure it is empty
                    randomisedGrid.add(gridSize - 1);
                } else {
                    int rngIndex = random.nextInt(bound);  // gets a randomised number within the pools bounds
                    int rngBmpIndex = posPool.get(rngIndex); // get the bitmap index from the pool using the randomised number
                    posPool.remove((Integer) rngBmpIndex);  // remove used number from the pool - use Integer else it takes as Arrayindex
                    bound -= 1;  // lower the bounds by 1 to match the new pool size so the next cycle can function properly
                    randomisedGrid.add(rngBmpIndex);  // add the randomised bmp index to the gridList
                }
            }

            // n=odd -> inversions: even = solvable
            // n=even -> empty cell on even row (from bottom: 1,2,3++ = 1 for bottom right) + inversions: odd = solvable
            //        -> empty cell on odd row + inversions: even = solvable
            //  empty cell always on bottom right so both odd and even size grids need even inversions
            // inversion: position pairs (a,b) where (list) index a < index b and (value) a > b have to check all
            int inversions = getInversions(gridSize, randomisedGrid);
            // if randomised grid is solvable then break the while loop and return that grid - else next loop creates new grid
            if (inversions % 2 == 0) {
//                System.out.println("Inversions even: " + inversions);
                break;
            }
            else {  // TODO: can just swap two neighbours to get correct grid
//                System.out.println("Inversions odd: " + inversions);
                int swap = randomisedGrid.get(0);
                randomisedGrid.set(0, randomisedGrid.get(1));
                randomisedGrid.set(1, swap);
//                System.out.println("Inversions modified: " + getInversions(gridSize, randomisedGrid));
//                break;
            }
        }
        return randomisedGrid;
    }

    private static int getInversions(int gridSize, ArrayList<Integer> randomisedGrid) {
        int inversions = 0;
        for (int index = 0; index < gridSize - 1; index++) {  // test all grid cells for pairs with higher index cells
            int currentNum = randomisedGrid.get(index);
            for (int x = index + 1; x < gridSize; x++) {  // find all pairs with higher index than current selected cell
                int pairNum = randomisedGrid.get(x);  // get the next highest index cell
                if (currentNum > pairNum) {  // add inversion if paired cell value is less than current cell value
                    inversions += 1;
                }
            }
        }
        return inversions;
    }
}
