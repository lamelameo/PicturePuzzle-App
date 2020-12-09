# Testing the function, PuzzleGridTest.randomiseGrid, used to generate randomised grids.
### *TL;DR: The function generates sufficiently random grid states for my liking.*

For 3x3 and 4x4 grid sizes, the function creates adequately random combinations. Tested by generating large amounts of 
grids (Using RandomiserGenerator.java) and simply analysing the data. For a 4x4 grid size, 1 test was done by generating 
one million grids. For the 3x3 grid size, 6 tests were done, generating one hundred up to ten million grids (multiplying 
the amount by ten for each test). Each grid was saved as an individual string in a text file then opened from a Python 
script (randomiser stats.py) which stored each unique grid in a hash table (dict), with the value being the frequency 
of that grid in the dataset. Pandas/matplotlib were used to analyse the data and display as either a scatter plot or histogram. 

Below are the statistics for the 4x4 grid size, of the 15! (last cell is fixed) combinations possible , only half are 
solvable. All but one of the million grids generated was unique, therefore any user is very unlikely to ever get a duplicate. 

![](https://github.com/lamelameo/PicturePuzzle-App/blob/master/RandomiserGenerator/images/4x4%201mil%20unique%20grids.png)

Below are the statistics and plots/histograms from the data obtained using one hundred -> ten million generated grids. 
Duplicate grids will be generated if called enough times, with 0 after 100 function calls but 26 after 1,000 calls, 
so a typical user will not get many duplicates. From 100,000 to 10,000,000 calls basically all possible grids will 
have been generated, and the frequency of each unique grid is approximately equal to: (number of calls)/(number of 
possible combinations). The frequency values are spread in a (Poisson) normal distribution for large call values with 
the mean value being that stated above (and std. dev. = sqrt(mean)), meaning each grid is equally likely to be generated.

![](https://github.com/lamelameo/PicturePuzzle-App/blob/master/RandomiserGenerator/images/3x3%20stats%20combined.png)

![](https://github.com/lamelameo/PicturePuzzle-App/blob/master/RandomiserGenerator/images/3x3%20scatter%20combined.png)

![](https://github.com/lamelameo/PicturePuzzle-App/blob/master/RandomiserGenerator/images/3x3%20hist%20combined.png)

There are extra sorted scatter plots in this folder, as well as images of stats related to extra testing where specific 
grids frequencies were tracked through different datasets.
