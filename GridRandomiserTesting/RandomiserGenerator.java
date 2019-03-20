package com.example.lamelameo.picturepuzzle;

import java.io.*;
import java.util.ArrayList;
import java.util.Random;

public class RandomiserGenerator {

    public static void main(String[] args) throws IOException {
        for (String s: args) {  // can use for command line arguments
            System.out.println(s);
        }
        // set parameters for creating dataset
        int numGenerated = 100000;
        int gridSize = 9;  // NOTE: use gridsize not cols
        // create File object and check if the file exists already, to add modifier if necessary so no conflict occurs
        File generatedFile = new File("gridsize("+gridSize+")_numgrids("+numGenerated+").txt");
        if (generatedFile.exists()) {
            System.out.println("file already exists");
            // TODO: change filename to avoid overwriting old one
//            int stuff = 0;
//            generatedFile = new File("gridsize("+gridSize+") numgrids("+numGenerated+")"+stuff+".txt");
        } else {
            System.out.println("made file");
        }
        // create the File in current folder using given name, and call randomiser function to generate grids which are
        // written to the file, each as a String of values separated by commas and a newline ("\n")
        boolean bool = generatedFile.createNewFile();
        FileWriter writer = new FileWriter(generatedFile);
        Random random = new Random();
        StringBuilder stringBuilder = new StringBuilder();
        // randomly generate the set amount of (solvable) grids using the randomise function
        // convert to string, append to stringbuilder, and write the stringBuilder contents to the file with a FileWriter
        // TODO: analyse the data all within this file, rather than using python?
        for (int i = 0; i < numGenerated; i++) {
            for (Integer num: randomiseGrid(gridSize, random)) {
                stringBuilder.append(num.toString()).append(",");
            }
            stringBuilder.append("\n");
        }
        writer.append(stringBuilder.toString());
        writer.flush();
        writer.close();

    }

    // function which generates a random solvable n x n grid of integers 1 - (n x n - 1) in list form (l->r, t->b)
    private static ArrayList<Integer> randomiseGrid(int gridSize, Random random) {
        // initialise objects and set variables
        ArrayList<Integer> randomisedGrid = new ArrayList<>();
        ArrayList<Integer> posPool = new ArrayList<>();
        // list of ascending values from 0 - size of grid used for tracking values tested for inversions
//        ArrayList<Integer> unTestedValues = new ArrayList<>();

        while (true) {  // create randomised grid, check if solvable, then break if it is
            // initialise variables for start of each loop
            int bound = gridSize-1;  // bounds for random generator...between 0 (inclusive) and number (exclusive)
            randomisedGrid.clear();
            for (int x=0; x<gridSize-1; x++) {  // pool for random indexes to be drawn from - exclude last cell index
                posPool.add(x);
            }

            // randomise grid and create list with outcome
            for (int x=0; x<gridSize; x++) {
//                unTestedValues.add(x);
                if (x == gridSize-1) {  // add last index to last in list to ensure it is empty
                    randomisedGrid.add(gridSize-1);
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
            // inversion: position pairs (a,b) where (list) index a < index b and (value) a > b have to check all
            int inversions = 0;
            for (int index=0; index<gridSize-1; index++) {  // test all grid cells for pairs with higher index cells
                int currentNum = randomisedGrid.get(index);
                for (int x=index+1; x<gridSize; x++) {  // find all pairs with higher index than current selected cell
                    int pairNum = randomisedGrid.get(x);  // get the next highest index cell
                    if (currentNum > pairNum) {  // add inversion if paired cell value is less than current cell value
                        inversions += 1;
                    }
                }
            }

            // if randomised grid is sovlable then break the while loop and return that grid - else next loop creates new grid
            if (inversions%2 == 0) {  // empty cell always on bottom right so both odd and even size grids need even inversions
                break;
            }
        }
        return randomisedGrid;
    }
}
