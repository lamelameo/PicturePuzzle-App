# Testing the function used to generate randomised grids

For 3x3 and 4x4 grid sizes, the fucntion creates adequately random combinations, tested by generating ridiculously large amounts...

3x3 grid: tested 10 million generated grids. 
Expect an average frequency of 10,000,000/20,160 = 496, if each grid is equally likely.
Created all possible solvable, unique grids with mean frequency of 496, spread in a narrow standard distribution (std. dev. = 22). 

Stats Output:

![alt_text](/GridGeneratorTesting/3x3%2010mil%20unique%20grids.png) ![alt_text]()

Histogram:
![alt_text]()

4x4 grid: tested 1 million generated grids.
Created 999,999 unique grids (0.000015% of total possible) and 1 duplicate.

![alt_text](/GridGeneratorTesting/4x4%201mil%20unique%20grids.png)


